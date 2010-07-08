package interdroid.vdb.persistence.api;

import java.io.Serializable;

/**
 * Container for the information we hold about an ongoing merge.
 * It is written to disc as a marker that a checkout is in merging mode.
 */
public class MergeInfo implements Serializable {
	private static final long serialVersionUID = -3951990737704349098L;

	/**
	 * Sha1 commit identifiers for the references of this merge.
	 */
	public String baseCommit_, theirCommit_, ourCommit_;

	/**
	 * Whether the merge is resolved or still in progress.
	 */
	public boolean resolved_;

	public MergeInfo() {}

	@Override
	public MergeInfo clone()
	{
		MergeInfo copy = new MergeInfo();
		copy.baseCommit_ = baseCommit_;
		copy.theirCommit_ = theirCommit_;
		copy.ourCommit_ = ourCommit_;
		copy.resolved_ = resolved_;
		return copy;
	}
}
