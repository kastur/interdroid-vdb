package interdroid.vdb.content.avro;

import java.io.IOException;

import org.apache.avro.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.Context;
import android.content.pm.ProviderInfo;

import interdroid.vdb.content.DatabaseInitializer;
import interdroid.vdb.content.GenericContentProvider;
import interdroid.vdb.content.metadata.Metadata;
import interdroid.vdb.persistence.api.VdbInitializer;

/**
 * This class implements content providers based on an avro schema.
 *
 * @author nick &lt;palmer@cs.vu.nl&gt;
 *
 */
public class AvroContentProvider extends GenericContentProvider {
    /**
     * Access to logger.
     */
    private static final Logger LOG =
            LoggerFactory.getLogger(AvroContentProvider.class);

    /**
     * The schema for this provider.
     */
    private final Schema mSchema;

    /**
     * The value column name.
     */
    public static final String VALUE_COLUMN_NAME =
            GenericContentProvider.SEPARATOR + "value";
    /**
     * The key column name.
     */
    public static final String KEY_COLUMN_NAME =
            GenericContentProvider.SEPARATOR + "key";
    /**
     * The type column name.
     */
    public static final String TYPE_COLUMN_NAME =
            GenericContentProvider.SEPARATOR + "type";
    /**
     * The ID column name.
     */
    public static final String ID_COLUMN_NAME =
            GenericContentProvider.SEPARATOR + "id";
    /**
     * The infix for array tables.
     */
    public static final String ARRAY_TABLE_INFIX =
            GenericContentProvider.SEPARATOR;
    /**
     * The infix for map tables.
     */
    public static final String MAP_TABLE_INFIX =
            GenericContentProvider.SEPARATOR;
    /**
     * The type name column.
     */
    public static final String TYPE_NAME_COLUMN_NAME =
            TYPE_COLUMN_NAME + GenericContentProvider.SEPARATOR + "name";
    /**
     * The type uri column.
     */
    public static final String TYPE_URI_COLUMN_NAME =
            TYPE_COLUMN_NAME + GenericContentProvider.SEPARATOR + "uri";

    /**
     * Constructs a provider for the given schema.
     * @param schema the schema to act as a content provider for
     */
    public AvroContentProvider(final Schema schema) {
        super(schema.getNamespace(), makeMetadata(schema));
        mSchema = schema;
    }

    /**
     * Constructs the metadata for this schema.
     * @param schema the schema to build metdata for
     * @return the metadata for this schema
     */
    private static Metadata makeMetadata(final Schema schema) {
        return new AvroMetadata(schema);
    }

    /**
     * Constructs a provider for the given schema, parsing it first.
     * @param schema the schema as a string.
     */
    public AvroContentProvider(final String schema) {
        this(Schema.parse(schema));
    }

    /**
     * @return the initializer for the database for this provider
     */
    public final VdbInitializer buildInitializer() {
        LOG.debug("Building initializer.");
        return new DatabaseInitializer(mNamespace, mMetadata,
                mSchema.toString());
    }

    @Override
    protected final void onAttach(final Context context,
            final ProviderInfo info) {

        // Make sure we are registered.
        LOG.debug("attachInfo");
        LOG.debug("Registering schema: {}", mSchema.getName());
        try {
            AvroSchemaRegistrationHandler.registerSchema(context, mSchema);
        } catch (IOException e) {
            LOG.error("Unexpected exception registering", e);
        }
        LOG.debug("Schema registered.");
    }
}

