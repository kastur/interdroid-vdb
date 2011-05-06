package interdroid.vdb.content.avro;

import interdroid.vdb.content.EntityUriBuilder;
import interdroid.vdb.content.VdbMainContentProvider.RepositoryInfo;
import interdroid.vdb.content.metadata.DatabaseFieldType;
import interdroid.vdb.content.orm.DbEntity;
import interdroid.vdb.content.orm.DbField;
import interdroid.vdb.content.orm.ORMGenericContentProvider;

import org.apache.avro.Schema;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

public class AvroProviderRegistry extends ORMGenericContentProvider {

	public static final String NAME = "schema_registry";
	public static final String NAMESPACE = "interdroid.vdb.content.avro";
	public static final String FULL_NAME = NAMESPACE + "." + NAME;

	public static final String KEY_SCHEMA = "schema";
	public static final String KEY_NAME = "name";
	public static final String KEY_NAMESPACE = "namespace";

	@DbEntity(name=NAME,
			itemContentType = "vnd.android.cursor.item/" + FULL_NAME,
			contentType = "vnd.android.cursor.dir/" + FULL_NAME)
	public static class RegistryConf {
		// Don't allow instantiation
		private RegistryConf() {}

		/**
		 * The default sort order for this table
		 */
		public static final String DEFAULT_SORT_ORDER = "modified DESC";

		public static final Uri CONTENT_URI =
			Uri.withAppendedPath(EntityUriBuilder.branchUri(AvroProviderRegistry.NAMESPACE, "master"), AvroProviderRegistry.NAME);

		@DbField(isID=true, dbType=DatabaseFieldType.INTEGER)
		public static final String _ID = "_id";

		@DbField(dbType=DatabaseFieldType.TEXT)
		public static final String NAME = KEY_NAME;

		@DbField(dbType=DatabaseFieldType.TEXT)
		public static final String NAMESPACE = KEY_NAMESPACE;

		@DbField(dbType=DatabaseFieldType.TEXT)
		public static final String SCHEMA = KEY_SCHEMA;

	}

	public AvroProviderRegistry() {
		super(NAMESPACE, RegistryConf.class);
	}

	public static final Uri URI = Uri.withAppendedPath(EntityUriBuilder.branchUri(NAMESPACE, "master"), NAME);
	private static RepositoryInfo sRegistryInfo;

	public RepositoryInfo[] getAllRepositories() {
		Cursor c = null;
		RepositoryInfo[] result = null;
		try {
			c = query(URI, new String[]{KEY_NAME, KEY_SCHEMA}, null, null, null);
			int i = 0;
			if (c != null) {
				result = new RepositoryInfo[c.getCount()];
//				result = new RepositoryInfo[c.getCount() + 1];
//				result[i++] = getRegistryRepositoryInfo();
				int nameIndex = c.getColumnIndex(KEY_NAME);
				int schemaIndex = c.getColumnIndex(KEY_SCHEMA);
				while (c.moveToNext()) {
					result[i++] = new RepositoryInfo(c.getString(nameIndex),
							new AvroContentProvider(c.getString(schemaIndex)));
				}
			}
		} finally {
			if (c != null) {
				try {
					c.close();
				} catch (Exception e) {};
			}
			if (result == null) {
				result = new RepositoryInfo[0];
//				result = new RepositoryInfo[1];
//				result[0] = getRegistryRepositoryInfo();
			}
		}

		return result;
	}

	public static RepositoryInfo get(Context context, String repositoryName) {
		RepositoryInfo result = null;

		// Provide this repository if they ask for it
		if (repositoryName == NAME) {
			result = getRegistryRepositoryInfo();
		} else {
			Cursor c = null;
			try {
				c = context.getContentResolver().query(URI, new String[]{KEY_SCHEMA},
						KEY_NAME + " = ?", new String[] {repositoryName}, null);
				if (c != null && c.moveToFirst()) {
					result = new RepositoryInfo(repositoryName,
							new AvroContentProvider(c.getString(c.getColumnIndex(KEY_SCHEMA))));
				}
			} finally {
				if (c != null) {
					try {
						c.close();
					} catch (Exception e) {};
				}
			}
		}

		return result;
	}

	private static synchronized RepositoryInfo getRegistryRepositoryInfo() {
		if (sRegistryInfo == null) {
			sRegistryInfo = new RepositoryInfo(NAME, new AvroProviderRegistry());
		}
		return sRegistryInfo;
	}

	public static void registerSchema(Context context, Schema schema) {
		// Have we already registered?
		Cursor c = null;
		try {
			c = context.getContentResolver().query(URI,
					new String[] {KEY_SCHEMA},
					KEY_NAME +" = ?", new String[] {schema.getName()}, null);
			if (c != null) {
				if (c.getCount() == 0) {
					ContentValues values = new ContentValues();
					values.put(KEY_SCHEMA, schema.toString());
					values.put(KEY_NAME, schema.getName());
					values.put(KEY_NAMESPACE, schema.getNamespace());
					context.getContentResolver().insert(URI, values);
				} else {
					// Do we need to update the schema then?
					Schema currentSchema = Schema.parse(c.getString(c.getColumnIndex(KEY_SCHEMA)));
					if (schema != currentSchema) {
						ContentValues values = new ContentValues();
						values.put(KEY_SCHEMA, schema.toString());
						context.getContentResolver().update(URI,
								values, KEY_NAME +" = ?", new String[]{AvroProviderRegistry.KEY_NAME});
					}
				}
			} else {
				throw new RuntimeException("Unable to query Schema Registry!");
			}
		} finally {
			if (c != null) {
				c.close();
			}
		}
	}

}
