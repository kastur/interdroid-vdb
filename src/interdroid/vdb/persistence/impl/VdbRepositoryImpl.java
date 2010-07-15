package interdroid.vdb.persistence.impl;

import interdroid.vdb.persistence.api.RemoteInfo;
import interdroid.vdb.persistence.api.VdbCheckout;
import interdroid.vdb.persistence.api.VdbInitializer;
import interdroid.vdb.persistence.api.VdbRepository;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.GitIndex;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ProgressMonitor;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.RefDatabase;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryConfig;
import org.eclipse.jgit.lib.Tree;
import org.eclipse.jgit.lib.GitIndex.Entry;
import org.eclipse.jgit.lib.RefUpdate.Result;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.filter.RevFilter;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.Transport;
import org.eclipse.jgit.treewalk.filter.TreeFilter;

import android.util.Log;

public class VdbRepositoryImpl implements VdbRepository {
	private File repoDir_;
	private final String name_;
	private final VdbInitializer initializer_;
	private Repository gitRepo_;
	private final Map<String, VdbCheckoutImpl> checkouts_ = new HashMap<String, VdbCheckoutImpl>();

    private static final String BRANCH_REF_PREFIX = Constants.R_HEADS;
    private static final String REMOTES_REF_PREFIX = Constants.R_REMOTES;
	private static final String TAG = "VdbRepoImpl";

	public VdbRepositoryImpl(String name, File repoDir, VdbInitializer initializer)
	throws IOException
	{
		repoDir_ = repoDir;
		name_ = name;
		initializer_ = initializer;

		initializeRepository();
	}

	public File getRepositoryDir()
	{
		return repoDir_;
	}

	public Repository getGitRepository()
	{
		return gitRepo_;
	}

    private void initializeRepository()
    {
    	try {
			gitRepo_ = new Repository(null, repoDir_);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

    	File gitDir = new File(repoDir_, ".git");
    	if (!gitDir.exists()) {
    		try {
				gitRepo_.create();
				Log.d(TAG, "Creating master.");
				VdbCheckoutImpl master = VdbCheckoutImpl.createMaster(this, initializer_);
				checkouts_.put(Constants.MASTER, master);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
    	}
    }

    public File checkoutReference(String subdir, String reference) throws IOException
    {
    	File checkoutDir = new File(repoDir_, subdir);
    	if (checkoutDir.isDirectory()) { // assume it's already checked out
    		return checkoutDir;
    	}
    	checkoutDir.mkdir();
    	if (!checkoutDir.isDirectory()) {
    		throw new IOException("Could not create checkout directory " + subdir);
    	}
    	Tree tree = gitRepo_.mapTree(reference);
    	GitIndex idx = gitRepo_.getIndex();
    	idx.readTree(tree);
    	idx.checkout(checkoutDir);

    	return checkoutDir;
    }

    public File checkoutBranch(String branchName) throws IOException
    {
    	return checkoutReference(branchName, BRANCH_REF_PREFIX + branchName);
    }

    public File checkoutCommit(String sha1) throws IOException
    {
    	return checkoutReference(sha1, sha1);
    }

	@Override
	public void createBranch(String branchName, String baseRef) throws IOException
	{
		ObjectId oId = gitRepo_.resolve(baseRef);
    	createBranchFromId(branchName, oId);
	}

    private void createBranchFromId(String branchName, ObjectId oId) throws IOException
    {
    	Ref ref = gitRepo_.getRef(BRANCH_REF_PREFIX + branchName);
    	if (ref != null && ref.getObjectId() != null) {
    		throw new IOException("Branch already exists, not overwriting.");
    	}
    	final RefUpdate ru = gitRepo_.updateRef(BRANCH_REF_PREFIX + branchName);
		ru.setNewObjectId(oId);
		ru.disableRefLog();
		if (ru.forceUpdate() == RefUpdate.Result.LOCK_FAILURE) {
			throw new IOException("Error updating reference " + branchName);
		}
		try {
			checkoutBranch(branchName);
		} catch(IOException e) {
			RefUpdate undoRu = gitRepo_.updateRef(BRANCH_REF_PREFIX + branchName);
			undoRu.delete();

			throw e;
		}
    }

	@Override
	public Set<String> listBranches() throws IOException
	{
		Map<String, Ref> branches = gitRepo_.getRefDatabase().getRefs(BRANCH_REF_PREFIX);
		return branches.keySet();
	}

	public Set<String> listRemotes() throws IOException
	{
		Config rc = gitRepo_.getConfig();
		return rc.getSubsections(RemoteInfo.SECTION);
	}

	@Override
	public Set<String> listRemoteBranches() throws IOException
	{
		Map<String, Ref> branches = gitRepo_.getRefDatabase().getRefs(REMOTES_REF_PREFIX);
		return branches.keySet();
	}

	@Override
	public RevWalk enumerateCommits(String... leaves) throws IOException
	{
		RevWalk rw = new RevWalk(gitRepo_);
		for (String leaf : leaves) {
			Ref r = gitRepo_.getRef(leaf);
			if (r == null  || r.getObjectId() == null) {
				 // TODO(emilian): proper exception
				throw new IOException("Invalid leaf reference.");
			}
			RevCommit commit = rw.parseCommit(r.getObjectId());
			rw.markStart(commit);
		}
		return rw;
	}

	public RevCommit getMergeBase(AnyObjectId... commitIds) throws IOException
	{
		if (commitIds.length < 2) {
			throw new IllegalArgumentException("Need to specify at least 2 commits.");
		}

		RevWalk walk = new RevWalk(gitRepo_);
		walk.setRevFilter(RevFilter.MERGE_BASE);
		walk.setTreeFilter(TreeFilter.ALL);

		for (AnyObjectId commitId : commitIds) {
			walk.markStart(walk.parseCommit(commitId));
		}

		RevCommit baseCommit = walk.next();
		if (baseCommit == null) {
			throw new IllegalStateException("Could not find merge base.");
		}
		return baseCommit;
	}

	@Override
	public synchronized VdbCheckout getBranch(String branchName) throws IOException
	{
		if (!checkouts_.containsKey(branchName)) {
			// statements below throw if not successful
			checkoutBranch(branchName);

			VdbCheckoutImpl branch = new VdbCheckoutImpl(this, branchName);
			checkouts_.put(branchName, branch);
		}
		return checkouts_.get(branchName);
	}

	@Override
	public VdbCheckout getCommit(String sha1) throws IOException
	{
		if (!checkouts_.containsKey(sha1)) {
			checkoutCommit(sha1);

			VdbCheckoutImpl checkout = new VdbCommitImpl(this, sha1);
			checkouts_.put(sha1, checkout);
		}
		return checkouts_.get(sha1);
	}

	@Override
	public String getName()
	{
		return name_;
	}

	/* package */ void releaseCheckout(String checkoutName)
	{
		checkouts_.remove(checkoutName);
	}

	@Override
	public synchronized void deleteBranch(String branchName) throws IOException
	{
		VdbCheckout checkout = getBranch(branchName);
		checkout.delete();
		checkouts_.remove(branchName);

		RefUpdate update = gitRepo_.getRefDatabase()
			.newUpdate(BRANCH_REF_PREFIX + branchName, true /* detach */);
		update.setForceUpdate(true);
		Result res = update.delete();
		if (res != Result.FORCED) {
			throw new RuntimeException("Could not delete, instead was " + res);
		}
	}

	@Override
	public synchronized void deleteRemote(String remoteName) throws IOException
	{
		RepositoryConfig rc = gitRepo_.getConfig();
		rc.unsetSection(RemoteInfo.SECTION, remoteName);
		rc.save();

		// now remove all references
		RefDatabase refDb = gitRepo_.getRefDatabase();
		String refsPrefix = REMOTES_REF_PREFIX + remoteName + "/";
		Map<String, Ref> branches = refDb.getRefs(refsPrefix);
		for (String name : branches.keySet()) {
			String fullName = refsPrefix + name;
			RefUpdate update = refDb.newUpdate(fullName, true /* detach */);
			update.delete();
		}
	}

	@Override
	public VdbCheckout getRemoteBranch(String remoteBranchName) throws IOException
	{
		gitRepo_.scanForRepoChanges();
		Ref ref = gitRepo_.getRefDatabase().getRefs(Constants.R_REMOTES)
				.get(remoteBranchName);
		ObjectId commitId = ref.getObjectId();
		if (commitId == null) {
			throw new IllegalStateException("Invalid remote branch.");
		}
		return getCommit(commitId.name());
	}

	@Override
	public synchronized RemoteInfo getRemoteInfo(String remoteName) throws IOException
	{
		if (!listRemotes().contains(remoteName)) {
			return null;
		}
		RemoteInfo info = new RemoteInfo(remoteName);
		try {
			info.load(gitRepo_.getConfig());
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
		return info;
	}

	@Override
	public void saveRemote(RemoteInfo info) throws IOException
	{
		RepositoryConfig rc = gitRepo_.getConfig();
		try {
			info.save(rc);
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
		rc.save();
	}

	public Transport buildConnection(String remoteName)
	{
		// this will throw if remoteName is invalid
		try {
			if (getRemoteInfo(remoteName) == null) {
				throw new RuntimeException("The remote is not configured.");
			}
			RemoteConfig cfg = new RemoteConfig(gitRepo_.getConfig(), remoteName);
			return Transport.open(gitRepo_, cfg);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void pullFromRemote(String remoteName, ProgressMonitor monitor)
			throws IOException
	{
		Transport connection = null;
		try {
			connection = buildConnection(remoteName);
			// TODO(emilian): need to watch for semantics depending on type
			connection.setRemoveDeletedRefs(true);
			connection.fetch(monitor, null);
		} finally {
			if (connection != null) {
				connection.close();
			}
		}
	}

	@Override
	public void pushToRemote(String remoteName, ProgressMonitor monitor)
			throws IOException
	{
		Transport connection = null;
		try {
			connection = buildConnection(remoteName);
			// TODO(emilian): need to watch semantics for MERGE_POINTs and HUBs
			connection.setRemoveDeletedRefs(true);
			connection.push(monitor, null);
		} finally {
			if (connection != null) {
				connection.close();
			}
		}
	}

	@Override
	public void pushToRemoteExplicit(String remoteName, String localBranch, String remoteBranch)
			throws IOException
	{
		// TODO(emilian): implement me
		throw new RuntimeException("Not implemented");
	}
}