/*
 * Copyright (c) 2008-2012 Vrije Universiteit, The Netherlands All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * Neither the name of the Vrije Universiteit nor the names of its contributors
 * may be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS ``AS IS''
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package interdroid.vdb.persistence.api;

import java.io.IOException;
import java.util.Set;

import org.apache.avro.Schema;
import org.eclipse.jgit.lib.ProgressMonitor;
import org.eclipse.jgit.revwalk.RevWalk;

/**
 * The interface for a repository within VDB.
 *
 * @author nick &lt;palmer@cs.vu.nl&gt;
 *
 */
public interface VdbRepository {
	/**
	 * Retrieves the name of the repository as configured in the corresponding
	 * {@link VdbRepositoryRegistry}.
	 *
	 * @return the name of the repository
	 */
	String getName();

	/**
	 * Creates a new branch, with the reference pointing to the baseRef commit.
	 * If baseRef is a branch name it will be automatically resolved to the
	 * current commit leaf of that branch.
	 *
	 * @param branchName Name of the new branch
	 * @param baseRef The commit to base the branch on (sha1 or branch name)
	 * @throws IOException if there is a problem reading or writing
	 */
	void createBranch(String branchName, String baseRef)
		throws IOException;

	/**
	 * Delete a branch with the given name.
	 * @param branchName the name of the branch to delete
	 * @throws IOException if there is a problem reading or writing
	 */
	void deleteBranch(String branchName)
		throws IOException;
	/**
	 * Retrieves the VdbCheckout object for an existing branch.
	 *
	 * @param branchName the name of the branch to cehckout
	 * @return the VdbCheckout object
	 * @throws IOException if there is a problem reading or writing
	 */
	VdbCheckout getBranch(String branchName) throws IOException;

	/**
	 * Retrieves a VdbCheckout object for a commit.
	 * Internally it will checkout the commit to a temporary directory.
	 * The returned object will be destined only for read access, methods
	 * like commit and getReadWriteDatabase() will fail.
	 *
	 * @param sha1 the SHA1 of the commit to fetch
	 * @return a checkout for the given commit
	 * @throws IOException if there is a problem reading or writing
	 */
	VdbCheckout getCommit(String sha1) throws IOException;

	/**
	 * Retrieves a {@link VdbCheckout} object for the current version of
	 * a remote branch.
	 * The returned object will be destined only for read access, methods
	 * like {@link VdbCheckout#commit(String, String, String)} and
	 * {@link VdbCheckout#getReadWriteDatabase()} will fail.
	 *
	 * Also after a synchronization operation the returned
	 * {@link VdbCheckout} object
	 * does not change to the new version, instead another call to
	 * {@link #getRemoteBranch(String)} needs to be made to retrieve an updated
	 * checkout.
	 *
	 * @param remoteBranchName The "remoteName/branchName" formatted string,
	 *     representing the branch, as returned by {@link #listRemoteBranches()}
	 * @return a checkout for the given branch
	 * @throws IOException if there is a problem reading or writing
	 */
	VdbCheckout getRemoteBranch(String remoteBranchName) throws IOException;

	/**
	 * @return Set of active branches.
	 * @throws IOException if there is a problem reading or writing
	 */
	Set<String> listBranches()
		throws IOException;

	/**
	 * @return Set of configured remote synchronization devices.
	 * @throws IOException if there is a problem reading or writing
	 */
	Set<String> listRemotes()
		throws IOException;

	/**
	 * Retrieves the list of currently available remote branches, individual
	 * elements are formatted as "remoteName/branchName".
	 * @return the list of available remote branches.
	 * @throws IOException if there is a problem reading or writing
	 */
	Set<String> listRemoteBranches() throws IOException;

	/**
	 * Retrieves the {@link RemoteInfo} configuration for the given remote.
	 *
	 * @param remoteName the name of the desired remote
	 * @return the {@link RemoteInfo} or null when the {@code remoteName}
	 * is not configured.
	 * @throws IOException if there is a problem reading or writing
	 */
	RemoteInfo getRemoteInfo(String remoteName) throws IOException;

	/**
	 * Updates or adds configuration for a remote device.
	 *
	 * @param info the info to be saved
	 * @throws IOException if there is a problem reading or writing
	 */
	void saveRemote(RemoteInfo info) throws IOException;

	/**
	 * Deletes all configuration for the given remote, which includes
	 * the {@link RemoteInfo} as well as its branches.
	 *
	 * @param remoteName the name of the remote
	 * @throws IOException if there is a problem reading or writing
	 */
	void deleteRemote(String remoteName) throws IOException;

	/**
	 * Pushes all the available branches to the given remote. The kind of
	 * synchronization is configured the type and parameters in the
	 * {@link RemoteInfo} for this remote. For {@link RemoteType#HUB}s
	 * it may fetch multiple sets of branches, for
	 * {@link RemoteType#MERGE_POINT}s
	 * it fetches a single set of branches.
	 *
	 * @param remoteName the name of the remote device.
	 * @param monitor callback object for monitoring.
	 * @throws IOException if there is a problem reading or writing
	 */
	void pullFromRemote(String remoteName, ProgressMonitor monitor)
		throws IOException;

	/**
	 * Pushes all the local branches to the given remote. The destination
	 * is configured by the type and parameters in the {@link RemoteInfo}
	 * for this remote.
	 *
	 * @param remoteName the name of the remote device.
	 * @param monitor callback object for monitoring.
	 * @throws IOException if there is a problem reading or writing
	 */
	void pushToRemote(String remoteName, ProgressMonitor monitor)
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
	 * @param localBranch the name of a local branch
	 * @param remoteBranch a name of a branch on the remote.
	 * @throws IOException if there is a problem reading or writing
	 */
	void pushToRemoteExplicit(String remoteName,
			String localBranch, String remoteBranch) throws IOException;


	/**
	 * Returns a {@link RevWalk} object used to walk the revision graph
	 * upwards to the root beginning at the given leaves.
	 *
	 * @param leaves The sha1 or branch names of the leaves to start the walk
	 *     from, in case of an empty list all the branches are walked.
	 * @return the {@link RevWalk} object
	 * @throws IOException if there is a problem reading or writing
	 */
	RevWalk enumerateCommits(String ...leaves)
		throws IOException;

	/**
	 * Returns true if this repository is marked as a repository.
	 * @return true if this is a repository.
	 */
	boolean isPublic();

	/**
	 * Sets this repository to be or private.
	 * @param isChecked true if this repository should be public
	 * @throws IOException If there is a problem saving the preference.
	 */
	void setIsPublic(boolean isChecked) throws IOException;


	/**
	 * Update this database to a new schema.
	 * @param newSchema the new schema for the database.
	 * @throws IOException If there is a problem with the update.
	 */
	void updateDatabase(String branch, Schema newSchema) throws IOException;

}
