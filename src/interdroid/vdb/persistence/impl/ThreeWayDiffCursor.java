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
