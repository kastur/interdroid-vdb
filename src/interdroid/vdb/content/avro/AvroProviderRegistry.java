package interdroid.vdb.content.avro;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import interdroid.vdb.Authority;
import interdroid.vdb.content.EntityUriBuilder;
import interdroid.vdb.content.VdbConfig.RepositoryConf;
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
import android.content.pm.ProviderInfo;
import android.database.Cursor;
import android.net.Uri;

/**
 * This class holds a registry for avro content providers
 * holding the namespace, name and schema for each avro provider.
 *
 * It is a content provider itself, constructed using the ORM
 * system within VDB. Nothing like bootstrapping the system using
 * your own dog food. ;)
 *
 * @author nick &lt;palmer@cs.vu.nl&gt;
 *
 */
public class AvroProviderRegistry extends ORMGenericContentProvider {
    /**
     * Access to logger.
     */
    private static final Logger LOG = LoggerFactory
            .getLogger(AvroProviderRegistry.class);

    /**
     * The registry configuration.
     * @author nick &lt;palmer@cs.vu.nl&gt;
     *
     */
    @DbEntity(name = AvroSchemaRegistrationHandler.NAME,
            itemContentType = "vnd.android.cursor.item/"
    + AvroSchemaRegistrationHandler.FULL_NAME,
            contentType = "vnd.android.cursor.dir/"
    + AvroSchemaRegistrationHandler.FULL_NAME)
    public static final class RegistryConf {
           /**
            * No construction.
            */
        private RegistryConf() { }

        /**
         * The default sort order for this table.
         */
        public static final String DEFAULT_SORT_ORDER = "modified DESC";

        /**
         * The content URI for this type.
         */
        public static final Uri CONTENT_URI =
            Uri.withAppendedPath(EntityUriBuilder.branchUri(
                    Authority.VDB, AvroSchemaRegistrationHandler.NAMESPACE,
                    "master"), AvroSchemaRegistrationHandler.NAME);

        /**
         * The ID field.
         */
        @DbField(isID = true, dbType = DatabaseFieldType.INTEGER)
        public static final String ID = "_id";

        /**
         * The key field.
         */
        @DbField(dbType = DatabaseFieldType.TEXT)
        public static final String NAME =
            AvroSchemaRegistrationHandler.KEY_NAME;

        /**
         * The namespace field.
         */
        @DbField(dbType = DatabaseFieldType.TEXT)
        public static final String NAMESPACE =
            AvroSchemaRegistrationHandler.KEY_NAMESPACE;

        /**
         * The schema field.
         */
        @DbField(dbType = DatabaseFieldType.TEXT)
        public static final String SCHEMA =
            AvroSchemaRegistrationHandler.KEY_SCHEMA;

    }

    /**
     * The context we are working in.
     */
    private Context mContext;

    /**
     * Construct a provider registry.
     */
    public AvroProviderRegistry() {
        super(AvroSchemaRegistrationHandler.NAMESPACE, RegistryConf.class);
    }

    /**
     * Returns all repositories registered with the system.
     * @return the list of repository configurations.
     */
    public final List<RepositoryConf> getAllRepositories() {
        Cursor c = null;
        ArrayList<RepositoryConf> result = new ArrayList<RepositoryConf>();
        try {
            c = query(AvroSchemaRegistrationHandler.URI,
                    new String[]{AvroSchemaRegistrationHandler.KEY_NAMESPACE,
                    AvroSchemaRegistrationHandler.KEY_SCHEMA},
                    null, null, null);
            if (c != null) {
                int namespaceIndex = c.getColumnIndex(
                        AvroSchemaRegistrationHandler.KEY_NAMESPACE);
                int schemaIndex = c.getColumnIndex(
                        AvroSchemaRegistrationHandler.KEY_SCHEMA);
                while (c.moveToNext()) {
                    result.add(new RepositoryConf(
                            c.getString(namespaceIndex),
                            c.getString(schemaIndex)));
                }
            }
        } finally {
            if (c != null) {
                try {
                    c.close();
                } catch (Exception e) {
                    LOG.warn("Exception while closing cursor: ", e);
                }
            }
        }

        return result;
    }

    @Override
    public final void onAttach(final Context context, final ProviderInfo info) {
        super.attachInfo(context, info);
        mContext = context;
    }

    @Override
    public final void onPostUpdate(final Uri uri, final ContentValues values,
            final String where, final String[] whereArgs) {
        migrateDb(values.getAsString(AvroSchemaRegistrationHandler.KEY_SCHEMA));
    }

    @Override
    public final void onPostInsert(final Uri uri,
            final ContentValues userValues) {

        VdbProviderRegistry registry;
        try {
            registry = new VdbProviderRegistry(mContext);
            registry.registerRepository(
                    new RepositoryConf(
                            userValues.getAsString(
                                AvroSchemaRegistrationHandler.KEY_NAMESPACE),
                            userValues.getAsString(
                                AvroSchemaRegistrationHandler.KEY_SCHEMA)));
        } catch (IOException e) {
            throw new RuntimeException("Unable to build registry: ", e);
        }
    }

    /**
     * Migrates a database from one schema to the new schema.
     * @param schemaString the new schema as a string.
     */
    private void migrateDb(final String schemaString) {
        // TODO: Finish implementing database migration on update of schema
        Cursor c = null;
        Schema schema = Schema.parse(schemaString);
        try {
            c = getContext().getContentResolver().query(
                    EntityUriBuilder.branchUri(Authority.VDB,
                            schema.getNamespace(),
                            "master/" + schema.getName()),
                            new String[] {"_id"}, null, null, null);
        } finally {
            try {
                if (c != null) {
                    c.close();
                }
            } catch (Exception e) {
                LOG.error("Caught exception closing cursor", e);
            }
        }
    }

    /**
     * Queries for the schema for the given URI.
     *
     * @param context the context to query in
     * @param uri the uri to retrieve the schema for
     * @return the schema for the given uri
     */
    public static Schema getSchema(final Context context, final Uri uri) {
        Cursor c = null;
        Schema schema = null;
        Uri dbUri = uri;
        try {
            // We expect to deal with internal paths
            if (!Authority.VDB.equals(uri.getAuthority())) {
                LOG.debug("Mapping to native: {}", uri);
                dbUri = EntityUriBuilder.toInternal(uri);
            }
            LOG.debug("Querying for schema for: {} {}", dbUri,
                    dbUri.getPathSegments().get(0));
            c = context.getContentResolver().query(
                    AvroSchemaRegistrationHandler.URI,
                    new String[]{AvroSchemaRegistrationHandler.KEY_SCHEMA},
                    AvroSchemaRegistrationHandler.KEY_NAMESPACE + "=?",
                    new String[] {dbUri.getPathSegments().get(0)}, null);
            if (c != null && c.moveToFirst()) {
                int schemaIndex = c.getColumnIndex(
                        AvroSchemaRegistrationHandler.KEY_SCHEMA);
                String schemaString = c.getString(schemaIndex);
                LOG.debug("Got schema: {}", schemaString);
                schema = Schema.parse(schemaString);
            } else {
                LOG.error("Schema not found.");
            }
        } finally {
            if (c != null) {
                try {
                    c.close();
                } catch (Exception e) {
                    LOG.warn("Exception while closing cursor: ", e);
                }
            }
        }
        return schema;
    }
}
