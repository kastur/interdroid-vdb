package interdroid.vdb.persistence.impl;

import interdroid.vdb.persistence.impl.MergeHelper.Database;
import interdroid.vdb.persistence.impl.MergeHelper.DiffResult;
import interdroid.vdb.persistence.impl.MergeHelper.TableMetadata;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class ThreeWayDiffCursor {
	private TableMetadata tableInfo_;
	private Cursor curTheirs_, curOurs_;
	/**
	 * This cursor will be used for extracting the row primary key values.
	 * If null then there is no current row.
	 */
	private Cursor rowCursor_;
	/**
	 * If the rowCursor is not null then these will hold the diff result
	 * for the current row.
	 */
	private DiffResult rowStateOurs_, rowStateTheirs_;

	/** Cache the column indexes for the diff result column of each diff */
	private int indexOursDiffState_, indexTheirsDiffState_;

	public ThreeWayDiffCursor(MergeHelper helper, SQLiteDatabase db, String table)
	{
		tableInfo_ = helper.getTableMetadata(db, table);
		curTheirs_ = helper.diff2(db, table, Database.BASE, Database.THEIRS);
		curOurs_ = helper.diff2(db, table, Database.BASE, Database.OURS);
		indexOursDiffState_ = curOurs_.getColumnIndexOrThrow(MergeHelper.COL_DIFF_RESULT);
		indexTheirsDiffState_ = curTheirs_.getColumnIndexOrThrow(MergeHelper.COL_DIFF_RESULT);
	}

	private void pickSmallestRow()
	{
		boolean haveTheirs = !curTheirs_.isAfterLast() && !curTheirs_.isBeforeFirst();
		boolean haveOurs   = !curOurs_.isAfterLast() && !curOurs_.isBeforeFirst();
		if (!haveTheirs && !haveOurs) {
			rowCursor_ = null;
			return;
		}
		if (haveTheirs && haveOurs) {
			/**
			 * Since we are merging, we have to iterate on the pk columns in the
			 * same order as the ORDER BY statement in {@link MergeHelper#diff2}.
			 **/
			for (int i = 0; i < tableInfo_.pkFields_.size(); ++i) {
				// TODO(emilian) rewrite me with strings too
				long ourLong = curOurs_.getLong(i);
				long theirLong = curTheirs_.getLong(i);

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
		rowStateOurs_ = haveOurs ? DiffResult.SAME
				: DiffResult.valueOf(curTheirs_.getString(indexOursDiffState_));
		rowStateTheirs_ = haveTheirs ? DiffResult.SAME
				: DiffResult.valueOf(curTheirs_.getString(indexTheirsDiffState_));
	}

	public boolean moveToFirst()
	{
		curTheirs_.moveToFirst();
		curOurs_.moveToFirst();
		pickSmallestRow();

		return rowCursor_ != null ? true : false;
	}

	public boolean moveToNext()
	{
		if (rowCursor_ == null) { // already past end
			return false;
		}
		if (!DiffResult.SAME.equals(rowStateOurs_)) {
			curOurs_.moveToNext();
		}
		if (!DiffResult.SAME.equals(rowStateTheirs_)) {
			curTheirs_.moveToNext();
		}
		return rowCursor_ != null ? true : false;
	}

	private void checkValidRow()
	{
		if (rowCursor_ == null) {
			throw new IllegalStateException(
					"Do not query the cursor when there are no more rows.");
		}
	}

	public DiffResult getStateTheirs()
	{
		checkValidRow();
		return rowStateTheirs_;
	}

	public DiffResult getStateOurs()
	{
		checkValidRow();
		return rowStateOurs_;
	}

	public Cursor getTheirsCursor()
	{
		return curTheirs_;
	}

	public Cursor getOursCursor()
	{
		return curOurs_;
	}
}