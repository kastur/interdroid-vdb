package interdroid.vdb.content.avro;

import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;

import interdroid.vdb.content.metadata.DatabaseFieldType;
import interdroid.vdb.content.metadata.FieldInfo;

/**
 * A FieldInfo for fields from a parsed Avro schema.
 *
 * @author nick &lt;palmer@cs.vu.nl&gt;
 *
 */
public class AvroFieldInfo extends FieldInfo {

    /**
     * The schema for the field.
     */
    private Schema mSchema;

    /**
     * @return the schema for this field.
     */
    public Schema getSchema() {
        return mSchema;
    }

    /**
     * Construct from an Avro Field.
     *
     * @param field the field to represent
     */
    public AvroFieldInfo(final Field field) {
        this(field, false);
    }

    /**
     * Construct from a field which is a key.
     *
     * @param field the field to represent
     * @param isKey true if this is a key field
     */
    protected AvroFieldInfo(final Field field, final boolean isKey) {
        super(field.name(), getFieldType(field.schema()), isKey);
        mSchema = field.schema();
    }

    /**
     * Construct from a schema for a field.
     * @param schema the schema for the field
     */
    protected AvroFieldInfo(final Schema schema) {
        super(schema.getName(), getFieldType(schema), false);
        mSchema = schema;
    }

    /**
     * Returns the field type for the given schema.
     * @param schema the schema the type is desired for
     * @return the type of database field used to represent this schema
     */
    private static DatabaseFieldType getFieldType(final Schema schema) {
        switch (schema.getType()) {
        case BYTES:
        case FIXED:
            // TODO: (nick) These should probably be handled using streams
            return DatabaseFieldType.BLOB;
        case DOUBLE:
        case FLOAT:
            return DatabaseFieldType.REAL_NUMBER;
        case INT:
        case LONG:
        case BOOLEAN:
            return DatabaseFieldType.INTEGER;
        case STRING:
            return DatabaseFieldType.TEXT;
        case ARRAY:
            return DatabaseFieldType.ONE_TO_MANY_INT;
        case RECORD:
        case ENUM:
            return DatabaseFieldType.ONE_TO_ONE;
        case MAP:
            return DatabaseFieldType.ONE_TO_MANY_STRING;
        case UNION:
            return DatabaseFieldType.BLOB;
        case NULL:
            return DatabaseFieldType.INTEGER;
        default:
            throw new RuntimeException("Unsupported Avro Field Type: "
                    + schema.toString());
        }
    }

}
