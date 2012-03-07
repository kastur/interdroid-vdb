package interdroid.vdb.persistence.api;

import java.io.Serializable;

/**
 * Container for the information we hold about an ongoing merge.
 * It is written to disc as a marker that a checkout is in merging mode.
 */
public final class MergeInfo implements Serializable {
    /**
     * The serial version UID for this class.
     */
    private static final long serialVersionUID = -3951990737704349098L;

    /**
     * Sha1 commit identifiers for the base revision.
     */
    private String mBase;

    /**
     * Sha1 commit identifier for their commit.
     */
    private String mTheirs;

    /**
     * Sha1 commit identifier for our commit.
     */
    private String mOurs;

    /**
     * Whether the merge is resolved or still in progress.
     */
    private boolean mResolved = false;

    /**
     * Construct a merge info.
     * @param base the base commit SHA1
     * @param theirs their commit SHA1
     * @param ours our commit SHA1
     */
    public MergeInfo(final String base, final String theirs,
            final String ours) {
        mBase = base;
        mTheirs = theirs;
        mOurs = ours;
    }

    @Override
    public MergeInfo clone() {
        MergeInfo copy = new MergeInfo(mBase, mTheirs, mOurs);
        copy.mResolved = mResolved;
        return copy;
    }

    /**
     * @return the mBase
     */
    public String getBase() {
        return mBase;
    }

    /**
     * @return the mTheirs
     */
    public String getTheirs() {
        return mTheirs;
    }

    /**
     * @return the mOurs
     */
    public String getOurs() {
        return mOurs;
    }

    /**
     * @return the mResolved
     */
    public boolean isResolved() {
        return mResolved;
    }

    /**
     * Sets this merge as resolved.
     */
    public void setResolved() {
        this.mResolved = true;
    }
}
