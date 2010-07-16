package interdroid.vdb.content.avro;

import java.util.List;

import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;
import org.apache.avro.Schema.Type;

import interdroid.vdb.content.metadata.DatabaseFieldType;
import interdroid.vdb.content.metadata.FieldInfo;
import interdroid.vdb.content.metadata.Metadata;

public class AvroFieldInfo extends FieldInfo {

	public Schema schema_;

	public AvroFieldInfo(Schema.Field field) {
		this(field, false);
	}

	protected AvroFieldInfo(Schema.Field field, boolean isKey) {
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
			// We allow unions of any single of the above types and NULL.
			List<Field>fields = schema.getFields();
			if (fields.size() == 2) {
				boolean zeroIsNull = fields.get(0).schema().getType() == Type.NULL;
				boolean oneIsNull = fields.get(1).schema().getType() == Type.NULL;
				if ((zeroIsNull || oneIsNull) && (!zeroIsNull && !oneIsNull)) {
					if (zeroIsNull) {
						return getFieldType(fields.get(0).schema());
					} else {
						return getFieldType(fields.get(1).schema());
					}
				}
			}
			// Intentional Fall Through
		default:
		case NULL:
			throw new RuntimeException("Unsupported Avro Field Type: " + schema.toString());
		}
	}

}
