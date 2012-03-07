package interdroid.vdb.content.avro;

import interdroid.vdb.Authority;
import interdroid.vdb.content.EntityUriBuilder;

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

/**
 * This handler manages registration of a schema with the AvroProviderRegistry.
 *
 * @author nick &lt;palmer@cs.vu.nl&gt;
 *
 */
public final class AvroSchemaRegistrationHandler {
    /**
     * Access to LOG.
     */
    private static final Logger LOG =
            LoggerFactory.getLogger(AvroSchemaRegistrationHandler.class);

    /**
     * No construction.
     */
    private AvroSchemaRegistrationHandler() {

    }

    /*
     * Note, these live in this class instead of the AvroProviderRegistry
     * because we don't want to have to include that class in the client
     * library.
     */

    /**
     * The name for our schema table.
     */
    public static final String NAME = "schema_registry";
    /**
     * The namespace for the schema table.
     */
    public static final String NAMESPACE = "interdroid.vdb.content.avro";
    /**
     * The name and namespace make up the full name.
     */
    public static final String FULL_NAME = NAMESPACE + "." + NAME;

    /**
     * The key for the schema field.
     */
    public static final String KEY_SCHEMA = "schema";
    /**
     * The key for the name of the schema.
     */
    public static final String KEY_NAME = "name";
    /**
     * The key for the namespace of the field.
     */
    public static final String KEY_NAMESPACE = "namespace";

    /**
     * The URI for the provider registry.
     */
    public static final Uri URI = Uri.withAppendedPath(
            EntityUriBuilder.branchUri(Authority.VDB,
                    NAMESPACE, "master"), NAME);

    /**
     * This returns a list of maps with data about the various providers
     * perfect for use in an android ListView.
     *
     * @param context the context to query in
     * @return a list of maps with data from the registration table
     */
    public static List<Map<String, Object>> getAllRepositories(
            final Context context) {
        Cursor c = null;
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        try {
            c = context.getContentResolver().query(URI, null,
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

    /**
     * Register a schema with the provider registry.
     *
     * @param context the context to work in
     * @param schema the schema to register
     * @throws IOException if something goes wrong writing the database
     */
    public static void registerSchema(final Context context,
            final Schema schema) throws IOException {
        // Have we already registered?
        Cursor c = null;
        try {
            LOG.debug("Checking for registration of {}", schema.getName());
            LOG.debug("Querying against URI: {}", URI);
            c = context.getContentResolver().query(URI,
                    new String[] {KEY_SCHEMA},
                    KEY_NAME + " = ?", new String[] {schema.getName()}, null);
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
                    Schema currentSchema =
                            Schema.parse(
                                    c.getString(c.getColumnIndex(KEY_SCHEMA)));

                    if (!schema.equals(currentSchema)) {
                        LOG.debug("Update required.");

                        ContentValues values = new ContentValues();
                        values.put(KEY_SCHEMA, schema.toString());
                        context.getContentResolver().update(URI,
                                values, KEY_NAME + " = ?",
                                new String[]{
                                AvroSchemaRegistrationHandler.KEY_NAME});
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
