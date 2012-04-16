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

import org.apache.avro.Schema;

import android.database.sqlite.SQLiteDatabase;

/**
 * Represents a checkout in the VDB system.
 *
 * @author nick &lt;palmer@cs.vu.nl&gt;
 *
 */
public interface VdbCheckout {
	/**
	 * Returns an sqlite database handler set to read only mode for
	 * the given branch.
	 *
	 * @return sqlite db handler
	 * @throws IOException if there is a problem reading or writing
	 */
	SQLiteDatabase getReadOnlyDatabase()
		throws IOException;

	/**
	 * Returns an sqlite database handler set to read write mode for
	 * the given branch.
	 *
	 * If there is a commit in progress on the branch, it may block
	 * until the commit is finished.
	 *
	 * @param branchName
	 * @return sqlite db handler
	 * @throws IOException if there is a problem reading or writing
	 */
	SQLiteDatabase getReadWriteDatabase()
		throws IOException;

	/**
	 * Commits a new version to the specified branch with the current
	 * database snapshot of that branch.
	 *
	 * Blocks until all the RW databases associated with branchName have been
	 * released so that we do not commit while the database is written to.
	 *
	 * @param authorName the name of the author
	 * @param authorEmail the email of the author
	 * @param msg the message for the commit
	 *
	 * @throws MergeInProgressException if the checkout is in merging mode but
	 * the merge has not yet been marked as resolved.
	 * @throws IOException if there is a problem reading or writing
	 */
	void commit(String authorName, String authorEmail, String msg)
		throws IOException, MergeInProgressException;

	/**
	 * Users need to release handlers obtained by calling get[RO/RW]Database
	 * by calling this method right after they are done with them.
	 *
	 * Commits are delayed until all write handles for a branch are released
	 * so this means handles need to be held for short intervals spanning
	 * single queries usually.
	 *
	 *  TODO(emilian): Also need to handle write cursors or figure out some
	 *  smarter way to obtain lock on db.
	 */
	void releaseDatabase();

	/**
	 * Reverts the database to the last saved stated, as found in the
	 * last commit on this branch. The checkout should be a local branch.
	 *
	 * Also, if the checkout was in merging mode then it is put back into
	 * normal mode.
	 *
	 * @throws IOException if there is a problem reading or writing
	 */
	void revert()
		throws IOException;

	/**
	 * Marks the current checkout (has to be a local branch) as a merge
	 * checkout for merging in the changes from the commit named by
	 * <code>theirSha1</code>. <br />
	 *
	 * When the checkout is in merging mode, the database handles returned
	 * by get*Database have 4 databases attached:
	 * <ol>
	 * <li> <b> master </b>
	 *     points to this checkout's sqlite database. It should be used
	 * to store the merge results. This is the default database, queries
	 * with tables not specifying a database touch this database.
	 * </li>
	 * <li> <b> theirs </b>
	 *     points to the database corresponding to the commit being
	 * merged in.
	 * </li>
	 * <li> <b> ours </b>
	 *     points to the database for the last commit that was made on
	 * the branch.
	 * </li>
	 * <li> <b> base </b>
	 *     points to the database of the base commit. Changes from theirs
	 * and ours should be considered relative to this common ancestor.
	 * </li>
	 * </ol>
	 *
	 * After the successful return of this function, the checkout will
	 * be marked for merging. The merge will be in the PENDING phase
	 * and all commits against it will fail with
	 * {@link MergeInProgressException}.
	 *
	 * @param theirSha1 the sha1 of the version to be moved
	 * @throws MergeInProgressException if the checkout was already marked
	 *     as a merge checkout.
	 * @throws DirtyCheckoutException if the checkout contained
	 *     modifications. The checkout should be made clean by committing
	 *     or reverting these modifications.
	 * @throws IOException if there is a problem reading or writing
	 */
	void startMerge(String theirSha1)
		throws MergeInProgressException, DirtyCheckoutException, IOException;

	/**
	 * Retrieves the merge information for this checkout. If it's not in merge
	 * mode <code>null</code> will be returned.
	 * @return the current merge information for this checkout.
	 */
	MergeInfo getMergeInfo();

	/**
	 * Marks the current merge as resolved, the next commit will not
	 * throw {@link MergeInProgressException} and will result in a commit
	 * with two parents.
	 */
	void doneMerge();

	/**
	 * Deletes the checkout and invalidates this object. An exclusive write
	 * lock will be obtained on the database and all files on disc will
	 * be deleted.
	 *
	 * All future access to this object will result in an IllegalStateException.
	 *
	 * Even if the checkout was for a local branch, the branch itself is
	 * not deleted. A future {@link VdbRepository#getBranch(String)}
	 * call may return another valid checkout for this same branch which
	 * will behave as if {@link #revert()} was called on it.
	 * @throws IOException if there is a problem reading or writing
	 */
	void delete() throws IOException;


	/**
	 * Returns the schema for this repository if it is an Avro based repository,
	 * otherwise, this returns an empty string.
	 * @throws IOException if there is a problem reading or writing.
	 * @return the schema as a string
	 */
	String getSchema() throws IOException;

	/**
	 * Update the database for this checkout to the given schema.
	 * @param newSchema the new schema for the database.
	 * @throws IOException  if there is a problem reading or writing.
	 */
	void updateDatabase(Schema newSchema) throws IOException;

}
