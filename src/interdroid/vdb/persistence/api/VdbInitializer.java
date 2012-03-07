package interdroid.vdb.persistence.api;

import android.database.sqlite.SQLiteDatabase;

/**
 * An initializer for a database.
 *
 * @author nick &lt;palmer@cs.vu.nl&gt;
 *
 */
public interface VdbInitializer {
    /**
     * Called when the database is created.
     * @param db the database which is being created.
     */
    void onCreate(SQLiteDatabase db);
    /**
     * @return the schema for this database.
     */
    String getSchema();
}
