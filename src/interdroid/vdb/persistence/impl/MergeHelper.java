package interdroid.vdb.persistence.impl;

import interdroid.vdb.persistence.api.VdbCheckout;

import java.util.Map;
import java.util.Vector;


import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class MergeHelper {
	static final String COL_DIFF_RESULT = "diff_result";

//	private final VdbCheckout parentCheckout_;
	private Map<String,TableMetadata> metadataCache_;

	public MergeHelper(VdbCheckout checkout)
	{
//		parentCheckout_ = checkout;
	}

	public enum Database {
		MASTER(""),
		THEIRS("theirs."),
		OURS("ours."),
		BASE("base.");

		final String prefix_;

		Database(String prefix)
		{
			prefix_ = prefix;
		}
	}

	public enum DiffResult {
		INSERTED,
		DELETED,
		MODIFIED,
		SAME
	}

	static class TableMetadata {
		public final String tableName_;
		public Vector<String> pkFields_;
		public Vector<String> normalFields_;

		public TableMetadata(String tableName)
		{
			tableName_ = tableName;
			// We use TreeSet because we want the order to remain constant
			// from the point we generate a query to the moment we fetch results.
			pkFields_ = new Vector<String>();
			normalFields_ = new Vector<String>();
		}
	}

	TableMetadata getTableMetadata(SQLiteDatabase db, String tableName)
	{
		// TODO(emilian): put the primary key in order
		if (!metadataCache_.containsKey(tableName)) {
			Cursor c = db.rawQuery("PRAGMA table_info('" + tableName + "')", null);
			try {
				int nameIndex = c.getColumnIndexOrThrow("name");
				int pkIndex = c.getColumnIndexOrThrow("pk");

				TableMetadata meta = new TableMetadata(tableName);
				c.moveToFirst();
				do {
					String name = c.getString(nameIndex);
					if (c.getInt(pkIndex) == 0) {
						meta.normalFields_.add(name);
					} else {
						meta.pkFields_.add(name);
					}
				} while(c.moveToNext());

				if (meta.pkFields_.size() == 0) {
					throw new IllegalStateException("The table " + tableName
							+ " has no primary key and is not supported.");
				}
				metadataCache_.put(tableName, meta);
			} finally {
				c.close();
			}
		}
		return metadataCache_.get(tableName);
	}

	private void appendAll(StringBuilder sb, String... args)
	{
		for (String s : args) {
			sb.append(s);
		}
	}

	/**
	 * Builds a query that joins tableOne and tableTwo on the common primaryKey.
	 * Returns the primary key fields from the first table and the other fields
	 * from the second table followed by a state field with the state value.
	 *
	 * Depending on joinType it will be used to retrieve INSERTED, DELETED or
	 * MODIFIED rows. Sometimes joinType will be LEFT OUTER JOIN and a corresponding
	 * row will not be found for tableTwo, this is why we select the primary key
	 * from tableOne.
	 *
	 * @param tableInfo
	 * @param tableOne
	 * @param tableTwo
	 * @param joinType
	 * @param state
	 * @return
	 */
	private StringBuilder buildPkJoinQuery(TableMetadata tableInfo, String tableOne,
			String tableTwo, String joinType, DiffResult state)
	{
		// TODO(emilian) field name escaping everywhere
		StringBuilder qb = new StringBuilder("SELECT ");
		for (int i = 0; i < tableInfo.pkFields_.size(); ++i) {
			String pkColumn = tableInfo.pkFields_.get(i);
			appendAll(qb, tableOne, ".", pkColumn, " AS ", pkColumn);
			qb.append(", ");
		}
		for (int i = 0; i < tableInfo.normalFields_.size(); ++i) {
			String column = tableInfo.normalFields_.get(i);
			appendAll(qb, tableTwo, ".", column, " AS ", column);
			qb.append(", ");
		}
		appendAll(qb, " '", state.name(), "' AS ", COL_DIFF_RESULT);
		appendAll(qb, " FROM ", tableOne, " ", joinType, " ", tableTwo, " ON ");
		for (int i = 0; i < tableInfo.pkFields_.size(); ++i) {
			String pkColumn = tableInfo.pkFields_.get(i);
			if (i > 0) {
				qb.append(" AND ");
			}
			appendAll(qb, tableOne, ".", pkColumn, " = ", tableTwo, ".", pkColumn);
		}
		return qb;
	}

	private String buildQueryDeleted(TableMetadata tableInfo, String tableOne, String tableTwo)
	{
		StringBuilder qb = buildPkJoinQuery(tableInfo, tableOne, tableTwo, "LEFT OUTER JOIN",
				DiffResult.DELETED);
		// only need to test a single column from the second table primary key,
		// either they will all be null or all non null
		appendAll(qb, " WHERE ", tableTwo, ".", tableInfo.pkFields_.get(0), " IS NULL");

		return qb.toString();
	}

	private String buildQueryInserted(TableMetadata tableInfo, String tableOne, String tableTwo)
	{
		// the inserted rows are deleted if comparing the other way round
		StringBuilder qb = buildPkJoinQuery(tableInfo, tableTwo, tableOne, "LEFT OUTER JOIN",
				DiffResult.INSERTED);
		appendAll(qb, " WHERE ", tableOne, ".", tableInfo.pkFields_.get(0), " IS NULL");
		return qb.toString();
	}

	private String buildQueryModified(TableMetadata tableInfo, String tableOne, String tableTwo)
	{
		StringBuilder qb = buildPkJoinQuery(tableInfo, tableOne, tableTwo, "INNER JOIN",
				DiffResult.MODIFIED);
		// we use this to have a valid query when we have no normal fields, it
		// will return 0 rows but have valid metadata.
		appendAll(qb, " WHERE 0 == 1");
		for (int i = 0; i < tableInfo.normalFields_.size(); ++i) {
			String normalColumn = tableInfo.normalFields_.get(i);
			appendAll(qb, " OR ", tableOne, ".", normalColumn, " != ", tableTwo, ".", normalColumn);
			appendAll(qb, " OR ", tableOne, ".", normalColumn, " IS NULL AND ",
					tableTwo, ".", normalColumn, " IS NOT NULL");
			appendAll(qb, " OR ", tableOne, ".", normalColumn, " IS NOT NULL AND ",
					tableTwo, ".", normalColumn, " IS NULL");
		}
		return qb.toString();
	}

	public Cursor diff2(SQLiteDatabase db, String table, Database from, Database to)
	{
		TableMetadata tableInfo = getTableMetadata(db, table);
		String fullFrom = from.prefix_ + table;
		String fullTo = to.prefix_ + table;

		StringBuilder qb = new StringBuilder();
		qb.append(buildQueryDeleted(tableInfo, fullFrom, fullTo));
		qb.append(" UNION ");
		qb.append(buildQueryInserted(tableInfo, fullFrom, fullTo));
		qb.append(" UNION ");
		qb.append(buildQueryModified(tableInfo, fullFrom, fullTo));
		qb.append(" ORDER BY ");
		for (int i = 0; i < tableInfo.pkFields_.size(); ++i) {
			String pkColumn = tableInfo.pkFields_.get(i);
			if (i > 0) {
				qb.append(", ");
			}
			qb.append(pkColumn);
		}

		return db.rawQuery(qb.toString(), null);
	}

	public ThreeWayDiffCursor diff3(SQLiteDatabase db, String table)
	{
		return new ThreeWayDiffCursor(this, db, table);
	}
}
