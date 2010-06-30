package interdroid.vdb.persistence.api;

import android.database.sqlite.SQLiteDatabase;

public interface VdbInitializer {
	public void onCreate(SQLiteDatabase db);
}
