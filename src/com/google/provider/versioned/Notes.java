package com.google.provider.versioned;

import interdroid.vdb.content.EntityUriBuilder;
import interdroid.vdb.content.metadata.DbEntity;
import interdroid.vdb.content.metadata.DbField;
import interdroid.vdb.content.metadata.DbField.DatabaseFieldTypes;

import com.example.android.notepad.NotePad;

import android.content.ContentValues;
import android.content.res.Resources;
import android.net.Uri;

@DbEntity(name="notes",
		itemContentType = "vnd.android.cursor.item/vnd.google.note",
		contentType = "vnd.android.cursor.dir/vnd.google.note")
public class Notes {
	private Notes() {}

	public static final Uri CONTENT_URI =
		Uri.withAppendedPath(EntityUriBuilder.branchUri("notes", "master"), "notes");

	@DbField(isID=true, dbType=DatabaseFieldTypes.INTEGER)
	public static final String _ID = "_id";

	@DbField(dbType=DatabaseFieldTypes.TEXT)
	public static final String TITLE = "title";

	@DbField(dbType=DatabaseFieldTypes.TEXT)
	public static final String NOTE = "note";

	@DbField(dbType=DatabaseFieldTypes.INTEGER)
	public static final String CREATED_DATE = "created";

	@DbField(dbType=DatabaseFieldTypes.INTEGER)
	public static final String MODIFIED_DATE = "modified";

	public static void preInsertHook(ContentValues values) {
        Long now = Long.valueOf(System.currentTimeMillis());

        // Make sure that the fields are all set
        if (!values.containsKey(CREATED_DATE)) {
            values.put(CREATED_DATE, now);
        }

        if (!values.containsKey(MODIFIED_DATE)) {
            values.put(MODIFIED_DATE, now);
        }

        if (!values.containsKey(TITLE)) {
            Resources r = Resources.getSystem();
            values.put(NotePad.Notes.TITLE, r.getString(android.R.string.untitled));
        }

        if (!values.containsKey(NOTE)) {
            values.put(NotePad.Notes.NOTE, "");
        }
	}
}
