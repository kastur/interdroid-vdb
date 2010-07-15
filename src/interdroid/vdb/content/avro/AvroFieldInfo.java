package interdroid.vdb.content.avro;

import java.util.List;

import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;
import org.apache.avro.Schema.Type;

import interdroid.vdb.content.metadata.DatabaseFieldTypes;
import interdroid.vdb.content.metadata.FieldInfo;

public class AvroFieldInfo extends FieldInfo {

	protected AvroFieldInfo(Schema.Field field) {
		super(field.name(), getFieldType(field), isFieldId(field));
	}

	private static boolean isFieldId(Field field) {
		return ("_id".equals(field.name()) && getFieldType(field) == DatabaseFieldTypes.INTEGER);
	}

	private static DatabaseFieldTypes getFieldType(Field field) {
		switch (field.schema().getType()) {
		case BYTES:
		case FIXED:
			return DatabaseFieldTypes.BLOB;
		case DOUBLE:
		case FLOAT:
			return DatabaseFieldTypes.REAL;
		case INT:
		case LONG:
		case BOOLEAN:
			return DatabaseFieldTypes.INTEGER;
		case STRING:
			return DatabaseFieldTypes.TEXT;
		case UNION:
			// We allow unions of any single of the above types and NULL.
			Schema unionSchema = field.schema();
			List<Field>fields = unionSchema.getFields();
			if (fields.size() == 2) {
				boolean zeroIsNull = fields.get(0).schema().getType() == Type.NULL;
				boolean oneIsNull = fields.get(1).schema().getType() == Type.NULL;
				if ((zeroIsNull || oneIsNull) && (!zeroIsNull && !oneIsNull)) {
					if (zeroIsNull) {
						return getFieldType(fields.get(0));
					} else {
						return getFieldType(fields.get(1));
					}
				}
			}
		case NULL:
		case ARRAY:
		case MAP:
		case RECORD:
		default:
			throw new RuntimeException("Unsupported Avro Field Type: " + field);
		}
	}

}
