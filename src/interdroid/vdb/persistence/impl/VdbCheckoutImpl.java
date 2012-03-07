package interdroid.vdb.persistence.impl;

import interdroid.vdb.persistence.api.DirtyCheckoutException;
import interdroid.vdb.persistence.api.MergeInProgressException;
import interdroid.vdb.persistence.api.MergeInfo;
import interdroid.vdb.persistence.api.VdbCheckout;
import interdroid.vdb.persistence.api.VdbInitializer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.CharBuffer;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.CommitCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.ConcurrentRefUpdateException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.api.errors.NoFilepatternException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.api.errors.NoMessageException;
import org.eclipse.jgit.api.errors.WrongRepositoryStateException;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.dircache.DirCacheBuilder;
import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.CommitBuilder;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectInserter;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.database.sqlite.SQLiteDatabase;


@SuppressWarnings("deprecation")
public class VdbCheckoutImpl implements VdbCheckout {
    private static final Logger logger = LoggerFactory.getLogger(VdbCheckoutImpl.class);

    private static final String SCHEMA_FILE = "schema";

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

    private static final String BRANCH_REF_PREFIX = Constants.R_HEADS;
    private static final String SQLITEDB = "sqlite.db";
    private static final String MERGEINFO = "MERGE_INFO";

    public VdbCheckoutImpl(VdbRepositoryImpl parentRepo, String checkoutName)
    {
        parentRepo_ = parentRepo;
        checkoutName_ = checkoutName;
        checkoutDir_ = new File(parentRepo.getRepositoryDir(), checkoutName);
        gitRepo_ = parentRepo.getGitRepository(checkoutName);

        if (!checkoutDir_.isDirectory()) { // assume it's already checked out
            throw new RuntimeException("Not checked out yet.");
        }
        loadMergeInfo();
    }

    @Override
    public synchronized void commit(String authorName, String authorEmail, String msg)
            throws IOException, MergeInProgressException
    {
        checkDeletedState();
        if (logger.isDebugEnabled())
            logger.debug("commit on " + checkoutName_);

        if (mergeInfo_ != null && !mergeInfo_.isResolved()) {
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

        if (mergeInfo_ != null && !mergeInfo_.isResolved()) {
            throw new MergeInProgressException();
        }

        Git git = new Git(gitRepo_);
        CommitCommand commit = git.commit();
        AddCommand add = git.add();
        add.addFilepattern(SQLITEDB);
        add.addFilepattern(SCHEMA_FILE);
        try {
            add.call();
        } catch (NoFilepatternException e) {
            throw new IOException();
        }
        PersonIdent author = new PersonIdent(authorName, authorEmail);
        commit.setAuthor(author);
        commit.setCommitter(author);
        RevCommit revision;
        try {
            revision = commit.call();
        } catch (Exception e) {
            throw new IOException();
        }

        if (mergeInfo_ != null) {
            // successfully committed the merge, get back to normal mode
            mergeInfo_ = null;
            saveMergeInfo();
            detachMergeDatabases();
        }

        if (logger.isDebugEnabled())
            logger.debug("Succesfully committed revision "
                + revision.getName().toString() + " on branch "
                + checkoutName_);
    }

    public static VdbCheckoutImpl createMaster(VdbRepositoryImpl parentRepo,
            VdbInitializer initializer)
    throws IOException
    {
        VdbCheckoutImpl branch = null;

        if (logger.isDebugEnabled())
            logger.debug("Creating master for: " + parentRepo.getName());
        File masterDir = new File(parentRepo.getRepositoryDir(), Constants.MASTER);
        if (!masterDir.mkdirs()) {
            throw new IOException("Unable to create directory: " + masterDir.getCanonicalPath());
        }

        if (initializer != null) {
            SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(new File(masterDir, SQLITEDB), null);
            db.setVersion(1);
            initializer.onCreate(db);

            File schema = new File(masterDir, SCHEMA_FILE);
            if (!schema.createNewFile()) {
                throw new RuntimeException("Unable to create schema file");
            }
            FileOutputStream fos = new FileOutputStream(schema);
            fos.write(initializer.getSchema().getBytes("utf8"));
            fos.close();

            branch = new VdbCheckoutImpl(parentRepo, Constants.MASTER);
            branch.setDb(db);

            try {
                branch.commit("Versioning Daemon", "vd@localhost", "Initial schema-only version.");
            } catch (MergeInProgressException e) {
                // should never happen because we're surely not in merge mode
                throw new RuntimeException(e);
            }
        }

        return branch;
    }

    private synchronized void setDb(SQLiteDatabase db) {
        db_ = db;
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

            File baseCheckout = parentRepo_.checkoutCommit(mergeInfo.getBase());
            File oursCheckout = parentRepo_.checkoutCommit(mergeInfo.getOurs());
            File theirsCheckout = parentRepo_.checkoutCommit(mergeInfo.getTheirs());

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
            logger.warn("Ignoring interupted exception: ", e);
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
            if(!infoFile.delete()) {
                logger.warn("Error deleting: {}", infoFile);
            }
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
        mergeInfo_.setResolved();
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

        MergeInfo info;
        try {
            AnyObjectId theirCommit = gitRepo_.resolve(theirSha1);
            AnyObjectId ourCommit = gitRepo_.getRef(BRANCH_REF_PREFIX + checkoutName_).getObjectId();
            String baseCommit = parentRepo_.getMergeBase(theirCommit, ourCommit).getId().getName();

            info = new MergeInfo(baseCommit,
                    theirCommit.getName(), ourCommit.getName());
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
        if (logger.isDebugEnabled())
            logger.debug("delete called for " + checkoutName_);

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

    @Override
    public String getSchema() throws IOException {
        File schema = new File(checkoutDir_, SCHEMA_FILE);
        if (!schema.canRead()) {
            throw new RuntimeException("Unable to read schema file");
        }
        BufferedReader reader = new BufferedReader(new FileReader(schema));
        CharBuffer target = null;
        try {
            target = CharBuffer.allocate((int) schema.length());
            reader.read(target);
        } finally {
            reader.close();
        }
        return target == null ? "" : target.toString();
    }
}
