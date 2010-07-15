package com.google.provider.versioned.avro;

import android.content.ContentValues;
import android.content.res.Resources;
import android.net.Uri;

import interdroid.vdb.content.ContentChangeHandler;
import interdroid.vdb.content.EntityUriBuilder;

public class Notes {
	public static final Uri CONTENT_URI =
		Uri.withAppendedPath(EntityUriBuilder.branchUri("google.notes", "master"), "google.notes");

	public static final String _ID = "_id";

	public static final String TITLE = "title";

	public static final String NOTE = "note";

	public static final String CREATED_DATE = "created";

	public static final String MODIFIED_DATE = "modified";

	/**
	 * The default sort order for this table
	 */
	public static final String DEFAULT_SORT_ORDER = "modified DESC";


	// Register our change handler
	static {
		ContentChangeHandler.register("google.notes", new ContentChangeHandler() {
			public void preInsertHook(ContentValues values) {
				Long now = Long.valueOf(System.currentTimeMillis());

				// Make sure that the fields are all set
				if (!values.containsKey(Notes.CREATED_DATE)) {
					values.put(Notes.CREATED_DATE, now);
				}

				if (!values.containsKey(Notes.MODIFIED_DATE)) {
					values.put(Notes.MODIFIED_DATE, now);
				}

				if (!values.containsKey(Notes.TITLE)) {
					Resources r = Resources.getSystem();
					values.put(Notes.TITLE, r.getString(android.R.string.untitled));
				}

				if (!values.containsKey(Notes.NOTE)) {
					values.put(Notes.NOTE, "");
				}
			}
		});
	}
}
