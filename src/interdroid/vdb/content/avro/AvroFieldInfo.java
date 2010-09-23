package interdroid.vdb.content.avro;

import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;

import interdroid.vdb.content.metadata.DatabaseFieldType;
import interdroid.vdb.content.metadata.FieldInfo;
import interdroid.vdb.content.metadata.Metadata;

public class AvroFieldInfo extends FieldInfo {

	public Schema schema_;

	public AvroFieldInfo(Field field) {
		this(field, false);
	}

	protected AvroFieldInfo(Field field, boolean isKey) {
		super(field.name(), getFieldType(field.schema()), isKey);
		schema_ = field.schema();
	}

	protected AvroFieldInfo(Schema schema, Metadata avroMetadata) {
		super(schema.getName(), getFieldType(schema), false);
		schema_ = schema;
	}

	private static DatabaseFieldType getFieldType(Schema schema) {
		switch (schema.getType()) {
		case BYTES:
		case FIXED:
			// TODO: (nick) These should probably be handled using external streams
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
			throw new RuntimeException("Unsupported Avro Field Type: " + schema.toString());
		}
	}

}
