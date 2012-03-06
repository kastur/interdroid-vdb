package interdroid.vdb.content;

import interdroid.vdb.content.metadata.DatabaseFieldType;
import interdroid.vdb.content.metadata.EntityInfo;
import interdroid.vdb.content.metadata.FieldInfo;
import interdroid.vdb.content.metadata.Metadata;
import interdroid.vdb.persistence.api.VdbInitializer;

import java.util.ArrayList;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.ContentValues;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;

/**
 * An initializer for a database. This is used to build the initial
 * schema for the database.
 *
 * @author nick &lt;palmer@cs.vu.nl&gt;
 *
 */
public class DatabaseInitializer implements VdbInitializer {
    /**
     * The logger.
     */
    private static final Logger LOG = LoggerFactory
            .getLogger(DatabaseInitializer.class);

    /** The metadata for this database. */
    private final Metadata mDbMetadata;
    /** The namespace for this database. */
    private final String mNamespace;
    /** The schema for this database as a string. */
    private final String mSchema;

    /**
     * Constructs a database intitializer with no schema.
     * @param namespace the namespace for the database
     * @param metadata the metadata for the database.
     */
    public DatabaseInitializer(final String namespace,
            final Metadata metadata) {
        mDbMetadata = metadata;
        mNamespace = namespace;
        mSchema = "";
    }

    /**
     * Constructs a database inititalizer.
     * @param namespace the namespace for the database
     * @param metadata the metadata for the database
     * @param schema the schema as a string.
     */
    public DatabaseInitializer(final String namespace,
            final Metadata metadata, final String schema) {
        mDbMetadata = metadata;
        mNamespace = namespace;
        mSchema = schema;
    }

    @Override
    public final void onCreate(final SQLiteDatabase db) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Initializing database for: "
                    + mNamespace);
        }
        // Keep track of what has been built as we go so as not to duplicate
        HashMap<String, String> built = new HashMap<String, String>();
        for (EntityInfo entity : mDbMetadata.getEntities()) {
            // Only handle root entities.
            // Children get recursed so foreign key constraints all point up
            if (entity.parentEntity == null) {
                buildTables(db, entity, built);
            }
        }
    }

    /**
     * Builds the tables for this database for the given entity.
     * @param db the database to build in
     * @param entity the entity to build tables for
     * @param built the hash of already built tables
     */
    private void buildTables(final SQLiteDatabase db,
            final EntityInfo entity, final HashMap<String, String> built) {
        boolean firstField = true;
        ArrayList<EntityInfo>children = new ArrayList<EntityInfo>();
        if (built.containsKey(entity.name())) {
            LOG.debug("Already built: {}", entity.name());
            return;
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Creating table for: "
                    + entity.namespace() + " : " + entity.name()
                    + ":"
                    + GenericContentProvider.escapeName(mNamespace, entity));
        }

        db.execSQL("DROP TABLE IF EXISTS "
                + GenericContentProvider.escapeName(mNamespace, entity));

        StringBuilder createSql = new StringBuilder("CREATE TABLE ");
        createSql.append(GenericContentProvider.escapeName(mNamespace, entity));
        createSql.append('(');
        for (FieldInfo field : entity.getFields()) {
            switch (field.dbType) {
            case ONE_TO_MANY_INT:
            case ONE_TO_MANY_STRING:
                // Skip these since they are handled by putting the
                // key for this one in the targetEntity
                // but queue the child to be handled when we are
                // done with this table.
                LOG.debug("Queueing Target Entity: ", field.targetEntity);
                children.add(field.targetEntity);
                break;
            case ONE_TO_ONE:
                LOG.debug("Building child table: ", field.targetEntity);
                // First we need to build the child table so we can
                // do the foreign key on this one
                buildTables(db, field.targetEntity, built);

                if (!firstField) {
                    createSql.append(",\n");
                } else {
                    firstField = false;
                }
                createSql.append(
                        DatabaseUtils.sqlEscapeString(field.fieldName));
                createSql.append(' ');
                createSql.append(DatabaseFieldType.INTEGER);
                createSql.append(" REFERENCES ");
                createSql.append(
                        GenericContentProvider.escapeName(mNamespace,
                                field.targetEntity));
                createSql.append('(');
                createSql.append(field.targetField.fieldName);
                createSql.append(')');
                createSql.append(" DEFERRABLE");
                LOG.debug("Create SQL now: {}", createSql);
                break;
            default:
                if (!firstField) {
                    createSql.append(",\n");
                } else {
                    firstField = false;
                }
                createSql.append(
                        DatabaseUtils.sqlEscapeString(field.fieldName));
                createSql.append(' ');
                createSql.append(field.dbTypeName());
                if (field.targetEntity != null) {
                    createSql.append(" REFERENCES ");
                    createSql.append(
                            GenericContentProvider.escapeName(mNamespace,
                                    field.targetEntity));
                    createSql.append('(');
                    createSql.append(field.targetField.fieldName);
                    createSql.append(") DEFERRABLE");
                }
                LOG.debug("Create SQL Default: {}",
                        createSql);
                break;
            }
        }

        // Now add the primary key constraint
        createSql.append(", ");
        createSql.append(" PRIMARY KEY (");
        firstField = true;
        for (FieldInfo field : entity.key) {
            if (!firstField) {
                createSql.append(", ");
            } else {
                firstField = false;
            }
            createSql.append(field.fieldName);
        }
        createSql.append(')');

        // Close the table
        createSql.append(")");

        if (LOG.isDebugEnabled()) {
            LOG.debug("Creating: "
                    + createSql.toString());
        }
        db.execSQL(createSql.toString());

        // Now process any remaining children
        for (EntityInfo child : children) {
            buildTables(db, child, built);
        }

        // Now fill in any enumeration values
        if (entity.enumValues != null) {
            ContentValues values = new ContentValues();
            for (Integer ordinal : entity.enumValues.keySet()) {
                String value = entity.enumValues.get(ordinal);
                values.clear();
                values.put("_id", ordinal);
                values.put("_value", value);
                db.insert(GenericContentProvider.escapeName(mNamespace, entity),
                        "_id", values);
            }
        }
        built.put(entity.name(), entity.name());
    }

    @Override
    public final String getSchema() {
        return mSchema;
    }
}
