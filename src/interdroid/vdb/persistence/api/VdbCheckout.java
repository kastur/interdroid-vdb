package interdroid.vdb.persistence.api;

import java.io.FileNotFoundException;
import java.io.IOException;

import android.database.sqlite.SQLiteDatabase;

public interface VdbCheckout {
	/**
	 * Returns an sqlite database handler set to read only mode for
	 * the given branch.
	 *
	 * @param branchName
	 * @return sqlite db handler
	 */
	public SQLiteDatabase getReadOnlyDatabase()
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
	 */
	public SQLiteDatabase getReadWriteDatabase()
	 	throws IOException;

	/**
	 * Commits a new version to the specified branch with the current
	 * database snapshot of that branch.
	 *
	 * Blocks until all the RW databases associated with branchName have been
	 * released so that we do not commit while the database is written to.
	 *
	 * @throws MergeInProgressException if the checkout is in merging mode but
	 * the merge has not yet been marked as resolved.
	 */
	public void commit(String authorName, String authorEmail, String msg)
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
	public void releaseDatabase();

	/**
	 * Reverts the database to the last saved stated, as found in the
	 * last commit on this branch. The checkout should be a local branch.
	 *
	 * Also, if the checkout was in merging mode then it is put back into
	 * normal mode.
	 *
	 * @throws IOException
	 */
	public void revert()
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
	 * 		points to this checkout's sqlite database. It should be used
	 * to store the merge results. This is the default database, queries
	 * with tables not specifying a database touch this database.
	 * </li>
	 * <li> <b> theirs </b>
	 * 		points to the database corresponding to the commit being
	 * merged in.
	 * </li>
	 * <li> <b> ours </b>
	 * 		points to the database for the last commit that was made on
	 * the branch.
	 * </li>
	 * <li> <b> base </b>
	 * 		points to the database of the base commit. Changes from theirs
	 * and ours should be considered relative to this common ancestor.
	 * </li>
	 * </ol>
	 *
	 * After the successful return of this function, the checkout will
	 * be marked for merging. The merge will be in the PENDING phase
	 * and all commits against it will fail with {@link MergeInProgressException}.
	 *
	 * @param theirSha1
	 * @throws MergeInProgressException if the checkout was already marked
	 * 		as a merge checkout.
	 * @throws DirtyCheckoutException if the checkout contained
	 * 		modifications. The checkout should be made clean by committing
	 * 		or reverting these modifications.
	 * @throws IOException
	 */
	public void startMerge(String theirSha1)
		throws MergeInProgressException, DirtyCheckoutException, IOException;

	/**
	 * Retrieves the merge information for this checkout. If it's not in merge
	 * mode <code>null</code> will be returned.
	 */
	public MergeInfo getMergeInfo();

	/**
	 * Marks the current merge as resolved, the next commit will not
	 * throw {@link MergeInProgressException} and will result in a commit
	 * with two parents.
	 */
	public void doneMerge()
		throws IllegalStateException;

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
	 * @throws IOException
	 */
	public void delete();


	/**
	 * Returns the schema for this repository if it is an Avro based repository,
	 * otherwise, this returns an empty string.
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public String getSchema() throws FileNotFoundException, IOException;
}
