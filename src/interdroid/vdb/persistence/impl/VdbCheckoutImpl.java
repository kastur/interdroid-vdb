package interdroid.vdb.persistence.impl;

import interdroid.vdb.persistence.api.DirtyCheckoutException;
import interdroid.vdb.persistence.api.MergeInProgressException;
import interdroid.vdb.persistence.api.MergeInfo;
import interdroid.vdb.persistence.api.VdbCheckout;
import interdroid.vdb.persistence.api.VdbInitializer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.Commit;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.GitIndex;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectWriter;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.GitIndex.Entry;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.filter.RevFilter;
import org.eclipse.jgit.treewalk.filter.TreeFilter;

import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class VdbCheckoutImpl implements VdbCheckout {
	private final VdbRepositoryImpl parentRepo_;
	private final Repository gitRepo_;
	private final String checkoutName_;
	private final File checkoutDir_;
	private MergeInfo mergeInfo_;	
	private SQLiteDatabase db_;
	private boolean wasDeleted_;
	
	/**
	 * We protect access to the sqlite database by using this lock. 
	 * The read/write lock DOES NOT correspond to reading or writing
	 * the database.
	 * 
	 * Instead - the read lock is used for accessing the database both
	 * for ro or rw modes, while the write lock is used for exclusively
	 * locking the checkout directory for commits.
	 */
	private final ReentrantReadWriteLock accessLock_ = new ReentrantReadWriteLock();

	private static final String TAG = "VdbBranch";
	private static final String BRANCH_REF_PREFIX = Constants.R_HEADS;
    private static final String SQLITEDB = "sqlite.db";
    private static final String MERGEINFO = "MERGE_INFO";
	
	public VdbCheckoutImpl(VdbRepositoryImpl parentRepo, String checkoutName)
	{
		parentRepo_ = parentRepo;
		checkoutName_ = checkoutName;
		checkoutDir_ = new File(parentRepo.getRepositoryDir(), checkoutName);
		gitRepo_ = parentRepo.getGitRepository();
		
		if (!checkoutDir_.isDirectory()) { // assume it's already checked out
    		throw new RuntimeException("Not checked out yet.");
    	}
		loadMergeInfo();
	}
	
    private void clearIndex(GitIndex idx)
    {
    	try {
	    	for (Entry e : idx.getMembers()) {
	    		if (!idx.remove(null, new File(e.getName()))) {
	    			throw new IOException("Could not remove entry " + e.getName());
	    		}
	    	}
    	} catch(IOException e) {
    		throw new RuntimeException("Could clear index.", e);
    	}
    }
	
	@Override
	public synchronized void commit(String authorName, String authorEmail, String msg)
			throws IOException, MergeInProgressException
	{
		checkDeletedState();
		Log.v(TAG, "commit on " + checkoutName_);
		
		if (mergeInfo_ != null && !mergeInfo_.resolved_) {
			throw new MergeInProgressException();
		}
		
		try {
			if (!accessLock_.writeLock().tryLock(5, TimeUnit.SECONDS)) {
				throw new RuntimeException("Timeout waiting for the locked database for commit.");
			}
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}

		try {
			commitImpl(authorName, authorEmail, msg);
		} finally {
			accessLock_.writeLock().unlock();
		}
	}
	
	private synchronized void commitImpl(String authorName, String authorEmail, String msg)
			throws IOException, MergeInProgressException
	{
		GitIndex index = new GitIndex(gitRepo_);
		clearIndex(index);
		index.add(checkoutDir_, new File(checkoutDir_, SQLITEDB));
		ObjectId newTreeId = index.writeTree();

		Commit commit = new Commit(gitRepo_);
		commit.setCommitter(new PersonIdent(authorName, authorEmail));
		commit.setAuthor(new PersonIdent(authorName, authorEmail));
		commit.setTreeId(newTreeId);
		commit.setMessage(msg);

		Ref ref = gitRepo_.getRef(BRANCH_REF_PREFIX + checkoutName_);
		if (ref != null && ref.getObjectId() != null) {
			if (mergeInfo_ != null) {
				// this is a merge commit with  two parents
				commit.setParentIds(new ObjectId[] {
						ref.getObjectId(),
						ObjectId.fromString(mergeInfo_.theirCommit_)});
			} else {
				commit.setParentIds(new ObjectId[] { ref.getObjectId() });
			}
		} else {
			commit.setParentIds(new ObjectId[0]);
		}

		ObjectWriter writer = new ObjectWriter(gitRepo_);
		commit.setCommitId(writer.writeCommit(commit));

		final RefUpdate ru = gitRepo_
				.updateRef(BRANCH_REF_PREFIX + checkoutName_);
		ru.setNewObjectId(commit.getCommitId());
		ru.disableRefLog();
		if (ru.forceUpdate() == RefUpdate.Result.LOCK_FAILURE) {
			throw new IOException("Error updating reference for branch "
					+ checkoutName_);
		}
		
		if (mergeInfo_ != null) {
			// successfully committed the merge, get back to normal mode
			mergeInfo_ = null;
			saveMergeInfo();
			detachMergeDatabases();
		}
		
		Log.v(TAG, "Succesfully committed revision "
				+ commit.getCommitId().toString() + " on branch "
				+ checkoutName_);
	}
	
	public static VdbCheckoutImpl createMaster(VdbRepositoryImpl parentRepo,
			VdbInitializer initializer)
	throws IOException
    {
		File masterDir = new File(parentRepo.getRepositoryDir(), Constants.MASTER);
		masterDir.mkdirs();
		
        SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(new File(masterDir, SQLITEDB), null);
        initializer.onCreate(db);
		db.setVersion(1);
		
		VdbCheckoutImpl branch = new VdbCheckoutImpl(parentRepo, Constants.MASTER);
		branch.db_ = db;
		
		try {
			branch.commit("Versioning Daemon", "vd@localhost", "Initial schema-only version.");
		} catch (MergeInProgressException e) {
			// should never happen because we're surely not in merge mode
			throw new RuntimeException(e);
		}

		return branch;
    }

	private synchronized void openDatabase()
	{
		if (db_ == null) {
			db_ = SQLiteDatabase.openDatabase(new File(checkoutDir_, SQLITEDB).getAbsolutePath(), 
					null /* cursor factory */, SQLiteDatabase.OPEN_READWRITE);
			try {
				attachMergeDatabases();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}
	
	private synchronized void detachMergeDatabases()
	{
		db_.execSQL("DETACH DATABASE base");
		db_.execSQL("DETACH DATABASE ours");
		db_.execSQL("DETACH DATABASE theirs");
	}
	
	private synchronized void attachMergeDatabases() throws IOException
	{
		openDatabase();
		
		MergeInfo mergeInfo = getMergeInfo();
		if (mergeInfo != null) {
			// mergeInfo.baseCommit = mergeInfo.ourCommit = mergeInfo.theirCommit
			//	= gitRepo_.resolve(BRANCH_REF_PREFIX + checkoutName_).getName();
			
			File baseCheckout = parentRepo_.checkoutCommit(mergeInfo.baseCommit_);
			File oursCheckout = parentRepo_.checkoutCommit(mergeInfo.ourCommit_);
			File theirsCheckout = parentRepo_.checkoutCommit(mergeInfo.theirCommit_);
			
			db_.execSQL("ATTACH DATABASE '" + new File(baseCheckout, SQLITEDB).getAbsolutePath()
					+ "' AS base");
			db_.execSQL("ATTACH DATABASE '" + new File(oursCheckout, SQLITEDB).getAbsolutePath()
					+ "' AS ours");
			db_.execSQL("ATTACH DATABASE '" + new File(theirsCheckout, SQLITEDB).getAbsolutePath()
					+ "' AS theirs");
		}
	}
	
	private synchronized SQLiteDatabase getDatabase() {
		openDatabase();
		try {
			if (accessLock_.readLock().tryLock(5, TimeUnit.SECONDS)) {
				return db_;
			}
		} catch (InterruptedException e) {
			// ignore
		}
		throw new RuntimeException("Timeout waiting for the locked database.");
	}
	
	@Override
	public synchronized SQLiteDatabase getReadOnlyDatabase() throws IOException {
		checkDeletedState();
		return getDatabase();
	}

	@Override
	public synchronized SQLiteDatabase getReadWriteDatabase() throws IOException {
		checkDeletedState();
		return getDatabase();
	}

	@Override
	public synchronized void releaseDatabase() {
		checkDeletedState();
		accessLock_.readLock().unlock();
	}

	private synchronized void loadMergeInfo()
	{
		File infoFile = new File(checkoutDir_, MERGEINFO);
		try {
			FileInputStream fis = new FileInputStream(infoFile);
			ObjectInputStream ois = new ObjectInputStream(fis);
			mergeInfo_ = (MergeInfo) ois.readObject();
			ois.close();
		} catch (FileNotFoundException e) {
			mergeInfo_ = null;
		} catch (IOException e) {
			throw new RuntimeException("Error while reading MergeInformation from " 
					+ infoFile, e);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException("Error while reading MergeInformation from " 
					+ infoFile, e);
		}
	}
	
	private synchronized void saveMergeInfo()
	{
		File infoFile = new File(checkoutDir_, MERGEINFO);
		if (mergeInfo_ == null) {
			infoFile.delete();
		} else {
			try {
				FileOutputStream fos = new FileOutputStream(infoFile);
				ObjectOutputStream oos = new ObjectOutputStream(fos);
				oos.writeObject(mergeInfo_);
				oos.close();
			} catch (FileNotFoundException e) {
				throw new RuntimeException("Could not open " + infoFile.getAbsolutePath()
						+ " for writing");
			} catch (IOException e) {
				throw new RuntimeException("Error while reading MergeInformation from " 
						+ infoFile, e);
			}
		}
		
	}
	
	@Override
	public synchronized MergeInfo getMergeInfo()
	{
		checkDeletedState();
		return (mergeInfo_ != null) ? mergeInfo_.clone() : null;
	}

	@Override
	public synchronized void doneMerge() throws IllegalStateException
	{
		checkDeletedState();
		if (mergeInfo_ == null) {
			throw new IllegalStateException("Branch was not in merge mode.");
		}
		mergeInfo_.resolved_ = true;
		saveMergeInfo();
	}
	
	@Override
	public synchronized void revert() throws IOException
	{
		checkDeletedState();
		try {
			Runtime.getRuntime().exec(new String[] {"rm", "-r",
					checkoutDir_.getAbsolutePath()}).waitFor();
		} catch (InterruptedException e) {
			throw new RuntimeException("Interrupt not allowed.");
		}
		parentRepo_.checkoutBranch(checkoutName_);
		mergeInfo_ = null;
	}
	
	@Override
	public synchronized void startMerge(String theirSha1)
	throws MergeInProgressException, DirtyCheckoutException, IOException
	{
		checkDeletedState();
		if (mergeInfo_ != null) {
			throw new MergeInProgressException();
		}
		
		// TODO(emilian): throw DirtyCheckoutException
		
		MergeInfo info = new MergeInfo();
		try {
			AnyObjectId theirCommit = gitRepo_.mapCommit(theirSha1).getCommitId();
			AnyObjectId ourCommit = gitRepo_.getRef(BRANCH_REF_PREFIX + checkoutName_).getObjectId();
			
			info.theirCommit_ = theirCommit.getName();
			info.ourCommit_ = ourCommit.getName();
			info.baseCommit_ =
					parentRepo_.getMergeBase(theirCommit, ourCommit).getId().getName();
			info.resolved_ = false;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
		// Only now save the merge state to the member variable to prevent invalid merge
		// state in case part of the above operations fail. 
		mergeInfo_ = info;
		saveMergeInfo();
		attachMergeDatabases();
	}

	private void checkDeletedState()
	{
		if (wasDeleted_) {
			throw new IllegalStateException("This checkout was deleted.");
		}
	}
	
	private void doDelete(File path) throws IOException
	{
		if (path.isDirectory()) {
			for (File child : path.listFiles()) {
				doDelete(child);
			}
		}
		if (!path.delete()) {
			throw new IOException("Could not delete " + path);
		}
	}
	
	@Override
	public void delete()
	{
		checkDeletedState();
		Log.v(TAG, "delete called for " + checkoutName_);
		
		try {
			if (!accessLock_.writeLock().tryLock(5, TimeUnit.SECONDS)) {
				throw new RuntimeException("Timeout waiting for exclusive lock on database.");
			}
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}

		try {
			wasDeleted_ = true;		
			doDelete(checkoutDir_);			
		} catch (IOException e) {
			throw new RuntimeException("Could not delete checkout.", e);
		} finally {
			accessLock_.writeLock().unlock();
			parentRepo_.releaseCheckout(checkoutName_);
		}
	}
}
