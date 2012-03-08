package interdroid.vdb.persistence.impl;

import interdroid.vdb.persistence.impl.MergeHelper.Database;
import interdroid.vdb.persistence.impl.MergeHelper.DiffResult;
import interdroid.vdb.persistence.impl.MergeHelper.TableMetadata;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/**
 * A cursor for managing a three way diff.
 *
 * @author nick &lt;palmer@cs.vu.nl&gt;
 *
 */
public class ThreeWayDiffCursor {
    /**
     * The metadata for the table.
     */
    private TableMetadata mTableInfo;
    /**
     * Cursor for theirs vs base.
     */
    private Cursor mTheirs;
    /**
     * Cursor for ours vs base.
     */
    private Cursor mOurs;

    /**
     * This cursor will be used for extracting the row primary key values.
     * If null then there is no current row.
     */
    private Cursor mRow;

    /**
     * If the rowCursor is not null then these will hold the diff result
     * for the current row for our side of the diff.
     */
    private DiffResult mRowStateOurs;
    /**
     * If the rowCursor is not null then these will hold the diff result
     * for the current row for their side of the diff.
     */
    private DiffResult mRowStateTheirs;

    /**
     * Cache the column indexes for the diff result column for our side.
     */
    private int mIndexOursDiffState;
    /**
     * Cache the column indexes for the diff result column for their side.
     */
    private int mIndexTheirsDiffState;

    /**
     * Construct a three way diff cursor.
     * @param helper the merge helper we work inside
     * @param db the database with tables
     * @param table the table we are to merge.
     */
    public ThreeWayDiffCursor(final MergeHelper helper, final SQLiteDatabase db,
            final String table) {
        mTableInfo = helper.getTableMetadata(db, table);
        mTheirs = helper.diff2(db, table, Database.BASE, Database.THEIRS);
        mOurs = helper.diff2(db, table, Database.BASE, Database.OURS);
        mIndexOursDiffState = mOurs.getColumnIndexOrThrow(
                MergeHelper.COL_DIFF_RESULT);
        mIndexTheirsDiffState = mTheirs.getColumnIndexOrThrow(
                MergeHelper.COL_DIFF_RESULT);
    }

    /**
     * Looks for the smallest primary key in the two cursors.
     */
    private void pickSmallestRow() {
        boolean haveTheirs = !mTheirs.isAfterLast()
                && !mTheirs.isBeforeFirst();
        boolean haveOurs   = !mOurs.isAfterLast()
                && !mOurs.isBeforeFirst();
        if (!haveTheirs && !haveOurs) {
            mRow = null;
            return;
        }
        if (haveTheirs && haveOurs) {
            /**
             * Since we are merging, we have to iterate on the pk columns in the
             * same order as the ORDER BY statement in
             * {@link MergeHelper#diff2}.
             **/
            for (int i = 0; i < mTableInfo.mKeyFields.size(); ++i) {
                // TODO: (emilian) rewrite me with strings too
                long ourLong = mOurs.getLong(i);
                long theirLong = mTheirs.getLong(i);

                if (ourLong < theirLong) {
                    haveTheirs = false;
                    break;
                }
                if (theirLong < ourLong) {
                    haveOurs = false;
                    break;
                }
            }
        }
        if (haveOurs) {
            mRowStateOurs = DiffResult.SAME;
            mRowStateTheirs = DiffResult.SAME;
        } else {
            mRowStateOurs = DiffResult.valueOf(
                    mTheirs.getString(mIndexOursDiffState));
            mRowStateTheirs = DiffResult.valueOf(
                    mTheirs.getString(mIndexTheirsDiffState));
        }

    }

    /**
     * Moves both cursors to first and picks the smallest primary key.
     * @return true if the move worked
     */
    public final boolean moveToFirst() {
        mTheirs.moveToFirst();
        mOurs.moveToFirst();
        pickSmallestRow();

        if (mRow != null) {
            return true;
        }
        return false;
    }

    /**
     * Move to the next row.
     * @return true if the move worked.
     */
    public final boolean moveToNext() {
        if (mRow == null) { // already past end
            return false;
        }
        if (!DiffResult.SAME.equals(mRowStateOurs)) {
            mOurs.moveToNext();
        }
        if (!DiffResult.SAME.equals(mRowStateTheirs)) {
            mTheirs.moveToNext();
        }
        if (mRow != null) {
            return true;
        }
        return false;
    }

    /**
     * Checks that we are on a valid row.
     */
    private void checkValidRow() {
        if (mRow == null) {
            throw new IllegalStateException(
                    "Do not query the cursor when there are no more rows.");
        }
    }

    /**
     * @return the state of their side.
     */
    public final DiffResult getStateTheirs() {
        checkValidRow();
        return mRowStateTheirs;
    }

    /**
     * @return the state of our side.
     */
    public final DiffResult getStateOurs() {
        checkValidRow();
        return mRowStateOurs;
    }

    /**
     * @return their side cursor
     */
    public final Cursor getTheirsCursor() {
        return mTheirs;
    }

    /**
     * @return our side cursor
     */
    public final Cursor getOursCursor() {
        return mOurs;
    }
}
