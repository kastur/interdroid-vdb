package interdroid.vdb.content.avro;

import interdroid.vdb.Authority;
import interdroid.vdb.content.EntityUriBuilder;
import interdroid.vdb.content.VdbProviderRegistry;
import interdroid.vdb.content.VdbConfig.RepositoryConf;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.avro.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

public class AvroSchemaRegistrationHandler {
	/**
	 * Access to LOG.
	 */
	private static final Logger LOG =
			LoggerFactory.getLogger(AvroSchemaRegistrationHandler.class);

	public static final String NAME = "schema_registry";
	public static final String NAMESPACE = "interdroid.vdb.content.avro";
	public static final String FULL_NAME = NAMESPACE + "." + NAME;

	public static final String KEY_SCHEMA = "schema";
	public static final String KEY_NAME = "name";
	public static final String KEY_NAMESPACE = "namespace";

	public static final Uri URI = Uri.withAppendedPath(EntityUriBuilder.branchUri(Authority.VDB,
			NAMESPACE, "master"), NAME);

	public static List<Map<String, Object>> getAllRepositories(Context context) {
		Cursor c = null;
		List<Map<String, Object>> result = new ArrayList<Map<String,Object>>();
		try {
			c = context.getContentResolver().query(URI,null,
					null, null, KEY_NAMESPACE + " ASC, " + KEY_NAME + " ASC");
			if (c != null) {
				LOG.debug("Cursor has: {}", c.getCount());
				c.moveToFirst();
				int name = c.getColumnIndex(KEY_NAME);
				int namespace = c.getColumnIndex(KEY_NAMESPACE);
				int schema = c.getColumnIndex(KEY_SCHEMA);

				do {
					Map<String, Object> map = new HashMap<String, Object>();
					map.put(KEY_NAME, c.getString(name));
					map.put(KEY_NAMESPACE, c.getString(namespace));
					map.put(KEY_SCHEMA, c.getString(schema));
					result.add(map);
					c.moveToNext();
				} while (!c.isAfterLast());
			}
		} finally {
			if (c != null) {
				c.close();
			}
		}
		LOG.debug("Returning {} repository", result.size());
		return result;
	}

	public static void registerSchema(Context context, Schema schema) throws IOException {
		// Have we already registered?
		Cursor c = null;
		try {
			LOG.debug("Checking for registration of {}", schema.getName());
			LOG.debug("Querying against URI: {}", URI);
			c = context.getContentResolver().query(URI,
					new String[] {KEY_SCHEMA},
					KEY_NAME +" = ?", new String[] {schema.getName()}, null);
			LOG.debug("Got cursor: {}", c);
			if (c != null) {
				if (c.getCount() == 0) {
					LOG.debug("Not already registered.");
					ContentValues values = new ContentValues();
					values.put(KEY_SCHEMA, schema.toString());
					values.put(KEY_NAME, schema.getName());
					values.put(KEY_NAMESPACE, schema.getNamespace());
					context.getContentResolver().insert(URI, values);
				} else {
					// Do we need to update the schema then?
					LOG.debug("Checking if we need to update.");
					c.moveToFirst();
					Schema currentSchema = Schema.parse(c.getString(c.getColumnIndex(KEY_SCHEMA)));
					if (! schema.equals(currentSchema)) {
						LOG.debug("Update required.");

						ContentValues values = new ContentValues();
						values.put(KEY_SCHEMA, schema.toString());
						context.getContentResolver().update(URI,
								values, KEY_NAME +" = ?", new String[]{AvroSchemaRegistrationHandler.KEY_NAME});
					}
				}
			} else {
				LOG.error("Unexpected error registering schema");
				throw new RuntimeException("Unable to query Schema Registry!");
			}
		} finally {
			if (c != null) {
				c.close();
			}
		}
		LOG.debug("Schema registration complete.");
	}
}
