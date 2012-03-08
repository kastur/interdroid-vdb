package interdroid.vdb.content.avro;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;
import org.apache.avro.Schema.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import interdroid.vdb.content.GenericContentProvider;
import interdroid.vdb.content.metadata.EntityInfo;
import interdroid.vdb.content.metadata.FieldInfo;

/**
 * This represents an entity in an avro schema for the database layer.
 *
 * @author nick &lt;palmer@cs.vu.nl&gt;
 *
 */
public class AvroEntityInfo extends EntityInfo {
    /**
     * Access to logger.
     */
    private static final Logger LOG =
            LoggerFactory.getLogger(AvroEntityInfo.class);

    // TODO: (nick) Support sort order from the schema as default sort order.
    // TODO: (nick) Support default values from the schema
    // TODO: (nick) Support properties to specify what the key is instead of/in addition to supporting implicit keys we use now
    // TODO: (nick) Support cross namespace entities. We could embed the URI for the entity in the parent_id instead of an integer id.
    // TODO: (nick) Fixed are named. Those should have their own table probably or they will break.

    /**
     * The schema for the entity.
     */
    private Schema mSchema;

    /**
     * Construct an entity.
     * @param schema the schema for the entity
     * @param avroMetadata the metadata for the database
     */
    public AvroEntityInfo(final Schema schema,
            final AvroMetadata avroMetadata) {
        this(schema, avroMetadata, null);
    }

    /**
     * Construct an entity with a parent entity pointer.
     * @param schema the schema for the entity
     * @param avroMetadata the metadata forthe database
     * @param parentEntity the parent entity for this entity
     */
    public AvroEntityInfo(final Schema schema, final AvroMetadata avroMetadata,
            final EntityInfo parentEntity) {
        mSchema = schema;
        this.parentEntity = parentEntity;
        if (parentEntity != null
                && !this.mSchema.getNamespace().equals(
                        parentEntity.namespace())) {
            throw new RuntimeException(
                    "Only entities in the same namespace"
                            + " are currently supported");
        }
        avroMetadata.put(this);
        parseSchema(avroMetadata, parentEntity);
        if (parentEntity != null) {
            parentEntity.children.add(this);
        }
    }

    /**
     * Parses the schema for a database.
     * @param avroMetadata the metadata for the database
     * @param parentEntity the parent entity for the current entity
     */
    private void parseSchema(final AvroMetadata avroMetadata,
            final EntityInfo parentEntity) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Constructing avro entity with name: "
                    + name() + " in namespace: " + namespace()
                    + " schema: " + mSchema);
        }

        // Every entity gets an _id int field as a primary key
        FieldInfo keyField = new AvroFieldInfo(
                new Field(AvroContentProvider.ID_COLUMN_NAME,
                        Schema.create(Schema.Type.INT), null, null), true);
        fields.put(keyField.fieldName, keyField);
        this.key.add(keyField);

        // Sub entities get columns which reference their parent key
        // TODO: (nick) For cross namespace this should be a string with the URI
        if (parentEntity != null) {
            for (FieldInfo field : parentEntity.key) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Adding parent key field.");
                }
                keyField = new AvroFieldInfo(
                        new Field(GenericContentProvider.PARENT_COLUMN_PREFIX
                                + field.fieldName,
                                ((AvroFieldInfo) field).getSchema(),
                                null, null), false);
                keyField.targetEntity = parentEntity;
                keyField.targetField = field;
                fields.put(keyField.fieldName, keyField);
            }
        }

        switch (mSchema.getType()) {
        case ENUM:
            parseEnum(avroMetadata);
            break;
        case RECORD:
            parseRecord(avroMetadata);
            break;
        default:
            throw new RuntimeException("Unsupported entity type: " + mSchema);
        }
    }

    /**
     * Parses an enumeration schema.
     * @param avroMetadata the metadata for the database
     */
    private void parseEnum(final AvroMetadata avroMetadata) {
        AvroFieldInfo field = new AvroFieldInfo(
                new Schema.Field(AvroContentProvider.VALUE_COLUMN_NAME,
                        Schema.create(Schema.Type.STRING), null, null), false);
        fields.put(field.fieldName, field);
        setEnumValues(mSchema);
    }

    /**
     * Parses a record schema.
     * @param avroMetadata the metadata for the database
     */
    private void parseRecord(final AvroMetadata avroMetadata) {
        // Walk the fields in the record constructing
        // either primitive fields or entity fields
        for (Field field: mSchema.getFields()) {
            switch (field.schema().getType()) {
            case ARRAY:
            case MAP:
            case ENUM:
            case RECORD:
                parseTableField(avroMetadata, field);
                break;
            case UNION:
                parseUnionField(avroMetadata, field);
                break;
            case FIXED:
            case FLOAT:
            case INT:
            case LONG:
            case BOOLEAN:
            case BYTES:
            case DOUBLE:
            case STRING:
            case NULL:
                parseColumnField(field);
                break;
            default:
                throw new RuntimeException("Unsupported type: "
                        + field.schema());
            }
        }
    }

    /**
     * Parses a simple column field.
     * @param field the field to parse
     */
    private void parseColumnField(final Field field) {
        FieldInfo fieldInfo = new AvroFieldInfo(field, false);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Adding field: " + fieldInfo.fieldName);
        }
        fields.put(fieldInfo.fieldName, fieldInfo);
    }

    /**
     * Parses a union field which ends up as multiple columns.
     * @param avroMetadata the metadata for the database
     * @param field the field to parse
     */
    private void parseUnionField(final AvroMetadata avroMetadata,
            final Field field) {
        // Unions get three fields, one to hold the type the value has,
        // one to hold the name if it is a named type and one for
        // the value
        // We are abusing SQLite manifest typing on the value column
        // with good reason
        FieldInfo typeField = new AvroFieldInfo(new Field(field.name()
                + AvroContentProvider.TYPE_COLUMN_NAME,
                Schema.create(Schema.Type.STRING), null, null), true);
        fields.put(typeField.fieldName, typeField);
        FieldInfo typeNameField = new AvroFieldInfo(
                new Field(field.name()
                        + AvroContentProvider.TYPE_NAME_COLUMN_NAME,
                        Schema.create(Schema.Type.STRING), null, null),
                        true);
        fields.put(typeNameField.fieldName, typeNameField);

        if (LOG.isDebugEnabled()) {
            LOG.debug("Adding union field: " + field.name());
        }

        // Make sure all of the possible inner types for the union exist
        for (Schema innerType : field.schema().getTypes()) {
            switch (innerType.getType()) {
            case ARRAY:
            case MAP:
                fetchOrBuildEntity(avroMetadata,
                        innerType, field.name(), this);
                break;
            case ENUM:
            case RECORD:
                fetchOrBuildEntity(avroMetadata,
                        innerType, innerType.getName(), this);
                break;
            default:
                // Nothing to do.
            }
        }

        parseColumnField(field);
    }

    /**
     * Parses a field which gets a table.
     * @param avroMetadata the metadata for the database
     * @param field the field to parse
     */
    private void parseTableField(final AvroMetadata avroMetadata,
            final Field field) {
        FieldInfo fieldInfo = new AvroFieldInfo(field);
        fields.put(fieldInfo.fieldName, fieldInfo);
        EntityInfo innerType = fetchOrBuildEntity(
                avroMetadata, field.schema(), field.name(), this);
        fieldInfo.targetEntity = innerType;
        // TODO: Support for complex keys.
        fieldInfo.targetField = innerType.key.get(0);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Adding sub-table field: "
                    + fieldInfo.fieldName);
        }
    }

    /**
     * Retrieves an entity from the metadata for the DB or builds it
     * if it doesn't already exist.
     * @param avroMetadata the metadata for the db
     * @param fieldSchema the schema for the field
     * @param fieldName the name for the field
     * @param parent the parent entity if any
     * @return the info for the entity
     */
    private EntityInfo fetchOrBuildEntity(final AvroMetadata avroMetadata,
            final Schema fieldSchema, final String fieldName,
            final EntityInfo parent) {
        EntityInfo innerType;

        switch (fieldSchema.getType()) {
        case ARRAY:
            innerType = fetchOrBuildArray(avroMetadata, fieldSchema, fieldName,
                    parent);
            break;
        case ENUM:
            innerType = fetchOrBuildEnum(avroMetadata, fieldSchema);
            break;
        case MAP:
            innerType = fetchOrBuildMap(avroMetadata, fieldSchema, fieldName,
                    parent);
            break;
        case RECORD:
            innerType = fetchOrBuildRecord(avroMetadata, fieldSchema);
            break;
        case UNION:
        case BOOLEAN:
        case BYTES:
        case DOUBLE:
        case FIXED:
        case FLOAT:
        case INT:
        case LONG:
        case NULL:
        case STRING:
        default:
            throw new RuntimeException("Unsupported type: " + fieldSchema);
        }

        return innerType;
    }

    /**
     * Fetches or builds a record entity.
     * @param avroMetadata the metadata for the db
     * @param fieldSchema the schema for the field
     * @return the entity info
     */
    private EntityInfo fetchOrBuildRecord(final AvroMetadata avroMetadata,
            final Schema fieldSchema) {
        EntityInfo innerType;
        innerType = avroMetadata.getEntity(fieldSchema.getFullName());

        // Construct the inner type entity.
        if (innerType == null) {
            innerType = new AvroEntityInfo(fieldSchema, avroMetadata);
        }
        return innerType;
    }

    /**
     * Fetches or builds a map entity.
     * @param avroMetadata the metadata for the db
     * @param fieldSchema the schema for the field
     * @param fieldName the name of the field
     * @param parent the parent entity if any
     * @return the entity info
     */
    private EntityInfo fetchOrBuildMap(final AvroMetadata avroMetadata,
            final Schema fieldSchema, final String fieldName,
            final EntityInfo parent) {
        EntityInfo innerType;
        // Now we need to build an association table
        innerType = buildMapAssociationTable(avroMetadata,
                fieldSchema, fieldName, parent);

        // Construct the target type if required.
        switch (fieldSchema.getElementType().getType()) {
        case RECORD:
        case ENUM:
        case ARRAY:
        case MAP:
            // Make sure the target type exists.
            fetchOrBuildEntity(avroMetadata,
                    fieldSchema.getValueType(), fieldName, innerType);
            break;
        case BOOLEAN:
        case BYTES:
        case DOUBLE:
        case FIXED:
        case FLOAT:
        case INT:
        case LONG:
        case NULL:
        case STRING:
        case UNION:
            break;
        default:
            throw new RuntimeException("Unsupported type: " + fieldSchema);
        }
        return innerType;
    }

    /**
     * Fetches or builds an enumeration entity.
     * @param avroMetadata the metadata for the db
     * @param fieldSchema the schema for the field
     * @return the entity info
     */
    private EntityInfo fetchOrBuildEnum(final AvroMetadata avroMetadata,
            final Schema fieldSchema) {
        EntityInfo innerType;
        innerType = avroMetadata.getEntity(fieldSchema.getFullName());

        if (innerType == null) {
            // Enums are built with no parent
            // since we point to them with an integer key
            innerType = new AvroEntityInfo(fieldSchema, avroMetadata, null);
        }
        return innerType;
    }

    /**
     * Fetches or builds an array entity.
     * @param avroMetadata the metadata for the db
     * @param fieldSchema the schema for the field
     * @param fieldName the name of the field
     * @param parent the parent entity if any
     * @return the entity info
     */
    private EntityInfo fetchOrBuildArray(final AvroMetadata avroMetadata,
            final Schema fieldSchema, final String fieldName,
            final EntityInfo parent) {
        EntityInfo innerType;
        // Build the association type
        innerType = buildArrayAssociationTable(
                avroMetadata, fieldSchema, fieldName, parent);

        // Construct the target type if required.
        switch (fieldSchema.getElementType().getType()) {
        case RECORD:
        case ENUM:
            // Make sure the target type exists.
            fetchOrBuildEntity(avroMetadata,
                    fieldSchema.getElementType(), fieldName, innerType);
            break;
        case ARRAY:
            // Make sure the target type exists.
            fetchOrBuildEntity(avroMetadata,
                    fieldSchema.getElementType(), fieldName, innerType);
            break;
        case MAP:
            // Make sure the target type exists.
            fetchOrBuildEntity(avroMetadata,
                    fieldSchema.getElementType(), fieldName, innerType);
            break;
        case BOOLEAN:
        case BYTES:
        case DOUBLE:
        case FIXED:
        case FLOAT:
        case INT:
        case LONG:
        case NULL:
        case STRING:
        case UNION:
            break;
        default:
            throw new RuntimeException("Unsupported type: " + fieldSchema);
        }
        return innerType;
    }

    /**
     * Builds an association table for a map.
     * @param avroMetadata the metadata for the db
     * @param fieldSchema the schema for the field
     * @param fieldName the name of the field
     * @param parent the parent entity if any
     * @return the info for the map entity
     */
    private EntityInfo buildMapAssociationTable(final AvroMetadata avroMetadata,
            final Schema fieldSchema, final String fieldName,
            final EntityInfo parent) {
        List<Field>mapFields = new ArrayList<Field>();
        mapFields.add(new Schema.Field(
                AvroContentProvider.KEY_COLUMN_NAME,
                Schema.create(Schema.Type.STRING), null, null));
        // Maps of unions get an extra type field
        if (fieldSchema.getType() == Type.UNION) {
            mapFields.add(new Schema.Field(AvroContentProvider.TYPE_COLUMN_NAME,
                    Schema.create(Schema.Type.STRING), null, null));
        }
        mapFields.add(new Schema.Field(fieldName,
                Schema.create(Schema.Type.BYTES), null, null));
        Schema mapSchema = Schema.createRecord(getFullName()
                + AvroContentProvider.MAP_TABLE_INFIX + fieldName,
                null, mSchema.getNamespace(), false);
        mapSchema.setFields(mapFields);
        return new AvroEntityInfo(mapSchema, avroMetadata, parent);
    }

    /**
     * Builds an association table for an array.
     * @param avroMetadata the metadata for the db
     * @param fieldSchema the schema for the field
     * @param fieldName the name of the field
     * @param parent the parent entity if any
     * @return the info for the array entity
     */
    private EntityInfo buildArrayAssociationTable(
            final AvroMetadata avroMetadata, final Schema fieldSchema,
            final String fieldName, final EntityInfo parent) {
        List<Field>arrayFields = new ArrayList<Field>();
        // Arrays of unions get an extra type field
        if (fieldSchema.getType() == Type.UNION) {
            arrayFields.add(new Schema.Field(
                    AvroContentProvider.TYPE_COLUMN_NAME,
                    Schema.create(Schema.Type.STRING), null, null));
        }
        arrayFields.add(new Schema.Field(fieldName,
                Schema.create(Schema.Type.BYTES), null, null));
        Schema mapSchema = Schema.createRecord(getFullName()
                + AvroContentProvider.ARRAY_TABLE_INFIX + fieldName,
                null, mSchema.getNamespace(), false);
        mapSchema.setFields(arrayFields);
        return new AvroEntityInfo(mapSchema, avroMetadata, parent);
    }

    @Override
    public final String name() {
        return mSchema.getName();
    }

    /**
     * @return the namespace for the schema
     */
    public final String namespace() {
        return mSchema.getNamespace();
    }

    @Override
    public final String contentType() {
        return "vnd.android.cursor.dir/vnd." + namespaceDot()  + name();
    }

    @Override
    public final String itemContentType() {
        return "vnd.android.cursor.item/vnd." + namespaceDot() + name();
    }

    /**
     * Sets the values for an enumeration.
     * @param fieldSchema the schema for the field
     */
    private void setEnumValues(final Schema fieldSchema) {
        enumValues = new HashMap<Integer, String>();
        for (String value : fieldSchema.getEnumSymbols()) {
            enumValues.put(fieldSchema.getEnumOrdinal(value), value);
        }
    }

}
