package interdroid.vdb.persistence.impl;

import java.util.ArrayList;
import java.util.Map;


import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/**
 * Utility for assisting with a merge.
 *
 * @author nick &lt;palmer@cs.vu.nl&gt;
 *
 */
public class MergeHelper {
    /**
     * The name of the column with a diff result.
     */
    static final String COL_DIFF_RESULT = "diff_result";

    /**
     * The checkout this merge helper is working in.
     */
    //    private final VdbCheckout parentCheckout_;

    /**
     * A cache of table metadata.
     */
    private Map<String, TableMetadata> mMetadataCache;

    /**
     * Construct a merge helper.
     */
    public MergeHelper(/* VdbCheckout checkout */) {
        //        parentCheckout_ = checkout;
    }

    /**
     * The databases we need to do a merge.
     * @author nick &lt;palmer@cs.vu.nl&gt;
     *
     */
    public enum Database {
        /** The master database we are merging into. */
        MASTER(""),
        /** Their database. */
        THEIRS("theirs."),
        /** Our database. */
        OURS("ours."),
        /** The base database. */
        BASE("base.");

        /**
         * The prefix for the database.
         */
        private final String mPrefix;

        /**
         * Construct with the given prefix.
         * @param prefix the prefix for this database.
         */
        private Database(final String prefix) {
            mPrefix = prefix;
        }

        /**
         * @return the prefix for the database.
         */
        public String toString() {
            return mPrefix;
        }
    }

    /**
     * The type of results our difference engine gives.
     *
     * @author nick &lt;palmer@cs.vu.nl&gt;
     *
     */
    public enum DiffResult {
        /** Item was inserted. */
        INSERTED,
        /** Item was deleted. */
        DELETED,
        /** Item was modified. */
        MODIFIED,
        /** No difference. */
        SAME
    }

    /**
     * A class which represents metadata for a table.
     *
     * @author nick &lt;palmer@cs.vu.nl&gt;
     *
     */
    static class TableMetadata {
        /** The name of the table. */
        public final String mTableName;
        /** The key fields in the table. */
        public ArrayList<String> mKeyFields;
        /** The data fields in the table. */
        public ArrayList<String> mNormalFields;

        /**
         * Construct metadata for the given table.
         *
         * @param tableName the name of the table.
         */
        public TableMetadata(final String tableName) {
            mTableName = tableName;
            mKeyFields = new ArrayList<String>();
            mNormalFields = new ArrayList<String>();
        }
    }

    /**
     * @param db the database the table lives in
     * @param tableName the name of the table
     * @return metadata for the requeted table.
     */
    public final TableMetadata getTableMetadata(
            final SQLiteDatabase db, final String tableName) {
        // TODO: (emilian) put the primary key in order
        if (!mMetadataCache.containsKey(tableName)) {
            Cursor c = db.rawQuery(
                    "PRAGMA table_info('" + tableName + "')", null);
            try {
                int nameIndex = c.getColumnIndexOrThrow("name");
                int pkIndex = c.getColumnIndexOrThrow("pk");

                TableMetadata meta = new TableMetadata(tableName);
                c.moveToFirst();
                do {
                    String name = c.getString(nameIndex);
                    if (c.getInt(pkIndex) == 0) {
                        meta.mNormalFields.add(name);
                    } else {
                        meta.mKeyFields.add(name);
                    }
                } while(c.moveToNext());

                if (meta.mKeyFields.size() == 0) {
                    throw new IllegalStateException("The table " + tableName
                            + " has no primary key and is not supported.");
                }
                mMetadataCache.put(tableName, meta);
            } finally {
                c.close();
            }
        }
        return mMetadataCache.get(tableName);
    }

    /**
     * Append all strings to the string builder.
     * @param sb the string builder to append to
     * @param args the strings to append.
     */
    private void appendAll(final StringBuilder sb, final String... args) {
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
     * MODIFIED rows. Sometimes joinType will be LEFT OUTER JOIN and a
     * corresponding row will not be found for tableTwo, this is why we select
     * the primary key from tableOne.
     *
     * @param tableInfo the metadata for the table
     * @param tableOne the first table
     * @param tableTwo the second table
     * @param joinType the join type for the tables
     * @param state the state of the difference
     * @return a string with the primary key join query
     */
    private StringBuilder buildPkJoinQuery(final TableMetadata tableInfo,
            final String tableOne, final String tableTwo, final String joinType,
            final DiffResult state) {
        // TODO: (emilian) field name escaping everywhere
        StringBuilder qb = new StringBuilder("SELECT ");
        for (int i = 0; i < tableInfo.mKeyFields.size(); ++i) {
            String pkColumn = tableInfo.mKeyFields.get(i);
            appendAll(qb, tableOne, ".", pkColumn, " AS ", pkColumn);
            qb.append(", ");
        }
        for (int i = 0; i < tableInfo.mNormalFields.size(); ++i) {
            String column = tableInfo.mNormalFields.get(i);
            appendAll(qb, tableTwo, ".", column, " AS ", column);
            qb.append(", ");
        }
        appendAll(qb, " '", state.name(), "' AS ", COL_DIFF_RESULT);
        appendAll(qb, " FROM ", tableOne, " ", joinType, " ", tableTwo, " ON ");
        for (int i = 0; i < tableInfo.mKeyFields.size(); ++i) {
            String pkColumn = tableInfo.mKeyFields.get(i);
            if (i > 0) {
                qb.append(" AND ");
            }
            appendAll(qb, tableOne, ".", pkColumn, " = ",
                    tableTwo, ".", pkColumn);
        }
        return qb;
    }

    /**
     * Builds a query for items that have been deleted.
     * @param tableInfo the info on the table
     * @param tableOne the first table
     * @param tableTwo the second table
     * @return an SQL query
     */
    private String buildQueryDeleted(final TableMetadata tableInfo,
            final String tableOne, final String tableTwo) {
        StringBuilder qb = buildPkJoinQuery(tableInfo, tableOne, tableTwo,
                "LEFT OUTER JOIN", DiffResult.DELETED);
        // only need to test a single column from the second table primary key,
        // either they will all be null or all non null
        appendAll(qb, " WHERE ", tableTwo, ".", tableInfo.mKeyFields.get(0),
                " IS NULL");

        return qb.toString();
    }

    /**
     * Build a query for items that have been inserted.
     * @param tableInfo the information on the table
     * @param tableOne the first table
     * @param tableTwo the second table
     * @return an SQL query
     */
    private String buildQueryInserted(final TableMetadata tableInfo,
            final String tableOne, final String tableTwo) {
        // the inserted rows are deleted if comparing the other way round
        StringBuilder qb = buildPkJoinQuery(tableInfo, tableTwo, tableOne,
                "LEFT OUTER JOIN", DiffResult.INSERTED);
        appendAll(qb, " WHERE ", tableOne, ".", tableInfo.mKeyFields.get(0),
                " IS NULL");
        return qb.toString();
    }

    /**
     * Build a query for tiems that have been modified.
     * @param tableInfo the information on the table
     * @param tableOne the first table
     * @param tableTwo the second table
     * @return an SQL query
     */
    private String buildQueryModified(final TableMetadata tableInfo,
            final String tableOne, final String tableTwo) {
        StringBuilder qb = buildPkJoinQuery(tableInfo, tableOne, tableTwo,
                "INNER JOIN", DiffResult.MODIFIED);
        // we use this to have a valid query when we have no normal fields, it
        // will return 0 rows but have valid metadata.
        appendAll(qb, " WHERE 0 == 1");
        for (int i = 0; i < tableInfo.mNormalFields.size(); ++i) {
            String normalColumn = tableInfo.mNormalFields.get(i);
            appendAll(qb, " OR ", tableOne, ".", normalColumn, " != ", tableTwo,
                    ".", normalColumn);
            appendAll(qb, " OR ", tableOne, ".", normalColumn, " IS NULL AND ",
                    tableTwo, ".", normalColumn, " IS NOT NULL");
            appendAll(qb, " OR ", tableOne, ".", normalColumn,
                    " IS NOT NULL AND ", tableTwo, ".", normalColumn,
                    " IS NULL");
        }
        return qb.toString();
    }

    /**
     * Construct a diff2 cursor.
     * @param db the database to work in
     * @param table the table to difference
     * @param from the from database
     * @param to the to database
     * @return a cursor with the differences.
     */
    public final Cursor diff2(final SQLiteDatabase db, final String table,
            final Database from, final Database to) {
        TableMetadata tableInfo = getTableMetadata(db, table);
        String fullFrom = from.mPrefix + table;
        String fullTo = to.mPrefix + table;

        StringBuilder qb = new StringBuilder();
        qb.append(buildQueryDeleted(tableInfo, fullFrom, fullTo));
        qb.append(" UNION ");
        qb.append(buildQueryInserted(tableInfo, fullFrom, fullTo));
        qb.append(" UNION ");
        qb.append(buildQueryModified(tableInfo, fullFrom, fullTo));
        qb.append(" ORDER BY ");
        for (int i = 0; i < tableInfo.mKeyFields.size(); ++i) {
            String pkColumn = tableInfo.mKeyFields.get(i);
            if (i > 0) {
                qb.append(", ");
            }
            qb.append(pkColumn);
        }

        return db.rawQuery(qb.toString(), null);
    }

    /**
     * Construct a diff3 cursor.
     * @param db the database to work in
     * @param table the table to diff
     * @return the diff3 cursor
     */
    public final ThreeWayDiffCursor diff3(final SQLiteDatabase db,
            final String table) {
        return new ThreeWayDiffCursor(this, db, table);
    }
}
