package interdroid.vdb.persistence.api;

import java.io.IOException;
import java.util.Set;

import org.eclipse.jgit.lib.ProgressMonitor;
import org.eclipse.jgit.revwalk.RevWalk;

public interface VdbRepository {
	/**
	 * Retrieves the name of the repository as configured in the corresponding
	 * {@link VdbRepositoryRegistry}.
	 *
	 * @return the name of the repository
	 */
	public String getName();

	/**
	 * Creates a new branch, with the reference pointing to the baseRef commit.
	 * If baseRef is a branch name it will be automatically resolved to the
	 * current commit leaf of that branch.
	 *
	 * @param branchName Name of the new branch
	 * @param baseRef The commit to base the branch on (sha1 or branch name)
	 */
	public void createBranch(String branchName, String baseRef)
		throws IOException;


	public void deleteBranch(String branchName)
		throws IOException;
	/**
	 * Retrieves the VdbCheckout object for an existing branch.
	 *
	 * @param branchName
	 * @return the VdbCheckout object
	 * @throws IOException
	 */
	public VdbCheckout getBranch(String branchName) throws IOException;

	/**
	 * Retrieves a VdbCheckout object for a commit.
	 * Internally it will checkout the commit to a temporary directory.
	 * The returned object will be destined only for read access, methods
	 * like commit and getReadWriteDatabase() will fail.
	 *
	 * @param sha1
	 * @return
	 * @throws IOException
	 */
	public VdbCheckout getCommit(String sha1) throws IOException;

	/**
	 * Retrieves a {@link VdbCheckout} object for the current version of
	 * a remote branch.
	 * The returned object will be destined only for read access, methods
	 * like {@link VdbCheckout#commit(String, String, String)} and
	 * {@link VdbCheckout#getReadWriteDatabase()} will fail.
	 *
	 * Also after a synchronization operation the returned {@link VdbCheckout} object
	 * does not change to the new version, instead another call to
	 * {@link #getRemoteBranch(String)} needs to be made to retrieve an updated
	 * checkout.
	 *
	 * @param remoteBranchName The "remoteName/branchName" formatted string,
	 * 		representing the branch, as returned by {@link #listRemoteBranches()}
	 * @return
	 * @throws IOException
	 */
	public VdbCheckout getRemoteBranch(String remoteBranchName) throws IOException;

	/**
	 * @return Set of active branches.
	 */
	public Set<String> listBranches()
		throws IOException;

	/**
	 * @return Set of configured remote synchronization devices.
	 */
	public Set<String> listRemotes()
		throws IOException;

	/**
	 * Retrieves the list of currently available remote branches, individual
	 * elements are formatted as "remoteName/branchName".
	 * @throws IOException
	 */
	public Set<String> listRemoteBranches() throws IOException;

	/**
	 * Retrieves the {@link RemoteInfo} configuration for the given remote.
	 *
	 * @param remoteName
	 * @return the {@link RemoteInfo} or null when the {@code remoteName}
	 * is not configured.
	 * @throws IOException
	 */
	public RemoteInfo getRemoteInfo(String remoteName) throws IOException;

	/**
	 * Updates or adds configuration for a remote device.
	 *
	 * @param info
	 * @throws IOException
	 */
	public void saveRemote(RemoteInfo info) throws IOException;

	/**
	 * Deletes all configuration for the given remote, which includes
	 * the {@link RemoteInfo} as well as its branches.
	 *
	 * @param remoteName
	 * @throws IOException
	 */
	public void deleteRemote(String remoteName) throws IOException;

	/**
	 * Pushes all the available branches to the given remote. The kind of
	 * synchronization is configured the type and parameters in the
	 * {@link RemoteInfo} for this remote. For {@link RemoteType#HUB}s
	 * it may fetch multiple sets of branches, for {@link RemoteType#MERGE_POINT}s
	 * it fetches a single set of branches.
	 *
	 * @param remoteName the name of the remote device.
	 * @param monitor callback object for monitoring.
	 * @throws IOException
	 */
	public void pullFromRemote(String remoteName, ProgressMonitor monitor)
		throws IOException;

	/**
	 * Pushes all the local branches to the given remote. The destination
	 * is configured by the type and parameters in the {@link RemoteInfo}
	 * for this remote.
	 *
	 * @param remoteName the name of the remote device.
	 * @param monitor callback object for monitoring.
	 * @throws IOException
	 */
	public void pushToRemote(String remoteName, ProgressMonitor monitor)
		throws IOException;

	/**
	 * Pushes a single branch to the given remote, as configured
	 * by its {@link RemoteInfo}. The destination branch name may be
	 * different from the local name, but the path is configured only
	 * by the {@link RemoteInfo} parameters.
	 *
	 * If the remote branch does not exist it may be created when the
	 * remote device allows it.
	 *
	 * @param remoteName the name of the remote device.
	 * @param monitor callback object for monitoring.
	 * @param localBranch the name of a local branch
	 * @param remoteBranch a name of a branch on the remote.
	 * @throws IOException
	 */
	public void pushToRemoteExplicit(String remoteName,
			String localBranch, String remoteBranch) throws IOException;


	/**
	 * Returns a {@link RevWalk} object used to walk the revision graph upwards to
	 * the root beginning at the given leaves.
	 *
	 * @param leaves The sha1 or branch names of the leaves to start the walk from,
	 * 				 in case of an empty list all the branches are walked.
	 * @return the {@link RevWalk} object
	 */
	public RevWalk enumerateCommits(String ...leaves)
		throws IOException;

	/**
	 * Returns true if this repository is marked as a public repository.
	 * @return true if this is a public repository.
	 */
	public boolean isPublic();

	/**
	 * Sets this repository to be public or private.
	 * @param isChecked true if this repository should be public
	 * @throws IOException If there is a problem saving the preference.
	 */
	public void setIsPublic(boolean isChecked) throws IOException;


}
