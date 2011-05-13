package interdroid.vdb.content.avro;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import interdroid.vdb.content.EntityUriBuilder;
import interdroid.vdb.content.VdbConfig.RepositoryConf;
import interdroid.vdb.content.VdbMainContentProvider;
import interdroid.vdb.content.VdbProviderRegistry;
import interdroid.vdb.content.metadata.DatabaseFieldType;
import interdroid.vdb.content.orm.DbEntity;
import interdroid.vdb.content.orm.DbField;
import interdroid.vdb.content.orm.ORMGenericContentProvider;

import org.apache.avro.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

public class AvroProviderRegistry extends ORMGenericContentProvider {
	private static final Logger logger = LoggerFactory.getLogger(AvroProviderRegistry.class);

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
			Uri.withAppendedPath(EntityUriBuilder.branchUri(VdbMainContentProvider.AUTHORITY, AvroProviderRegistry.NAMESPACE, "master"), AvroProviderRegistry.NAME);

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

	public static final Uri URI = Uri.withAppendedPath(EntityUriBuilder.branchUri(VdbMainContentProvider.AUTHORITY,
			NAMESPACE, "master"), NAME);

	public List<RepositoryConf> getAllRepositories() {
		Cursor c = null;
		ArrayList<RepositoryConf> result = new ArrayList<RepositoryConf>();
		try {
			c = query(URI, new String[]{KEY_NAMESPACE, KEY_SCHEMA}, null, null, null);
			if (c != null) {
				int namespaceIndex = c.getColumnIndex(KEY_NAMESPACE);
				int schemaIndex = c.getColumnIndex(KEY_SCHEMA);
				while (c.moveToNext()) {
					result.add(new RepositoryConf(c.getString(namespaceIndex), c.getString(schemaIndex)));
				}
			}
		} finally {
			if (c != null) {
				try {
					c.close();
				} catch (Exception e) {};
			}
		}

		return result;
	}

	public void registerSchema(Context context, Schema schema) {
		// Have we already registered?
		Cursor c = null;
		try {
			logger.debug("Checking for registration of {}", schema.getName());
			logger.debug("Querying against URI: {}", URI);
			c = query(URI,
					new String[] {KEY_SCHEMA},
					KEY_NAME +" = ?", new String[] {schema.getName()}, null);
			logger.debug("Got cursor: {}", c);
			if (c != null) {
				if (c.getCount() == 0) {
					logger.debug("Not already registered.");
					ContentValues values = new ContentValues();
					values.put(KEY_SCHEMA, schema.toString());
					values.put(KEY_NAME, schema.getName());
					values.put(KEY_NAMESPACE, schema.getNamespace());
					insert(URI, values);

					try {
						VdbProviderRegistry registry = new VdbProviderRegistry(context);
						registry.registerRepository(new RepositoryConf(schema.getNamespace(), schema.toString()));
					} catch (IOException e) {
						throw new RuntimeException("Error registering repository", e);
					}

				} else {
					// Do we need to update the schema then?
					logger.debug("Checking if we need to update.");
					c.moveToFirst();
					Schema currentSchema = Schema.parse(c.getString(c.getColumnIndex(KEY_SCHEMA)));
					if (! schema.equals(currentSchema)) {
						// TODO: Migrate the database to the new schema and check it in.
						ContentValues values = new ContentValues();
						values.put(KEY_SCHEMA, schema.toString());
						update(URI,
								values, KEY_NAME +" = ?", new String[]{AvroProviderRegistry.KEY_NAME});
					}
				}
			} else {
				logger.error("Unexpected error registering schema");
				throw new RuntimeException("Unable to query Schema Registry!");
			}
		} finally {
			if (c != null) {
				c.close();
			}
		}
		logger.debug("Schema registration complete.");
	}

	public static AvroProviderRegistry getInstance(Context context) {
		try {
			AvroProviderRegistry result = (AvroProviderRegistry) new VdbProviderRegistry(context).get(URI);
			result.attachInfo(context, null);
			return result;
		} catch (IOException e) {
			throw new RuntimeException("Unable to get AvroProviderRegistry instance");
		}
	}

}
