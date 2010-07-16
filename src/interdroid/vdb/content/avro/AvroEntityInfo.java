package interdroid.vdb.content.avro;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;

import android.util.Log;

import interdroid.vdb.content.metadata.DatabaseFieldType;
import interdroid.vdb.content.metadata.EntityInfo;
import interdroid.vdb.content.metadata.FieldInfo;

public class AvroEntityInfo extends EntityInfo {
	private static final String TAG = "AvroEntity";

	// TODO: (nick) Support sort order from the schema as default?
	// TODO: (nick) Support properties to specify what the key is instead of/in addition to supporting implicit keys we use now


	private Schema schema_;

	public AvroEntityInfo(Schema schema, AvroMetadata avroMetadata) {
		this(schema, avroMetadata, null);
	}

	public AvroEntityInfo(Schema schema, AvroMetadata avroMetadata, EntityInfo parentEntity) {
		schema_ = schema;
		this.parentEntity = parentEntity;
		parseSchema(avroMetadata, parentEntity);
		if (parentEntity != null) {
			parentEntity.children.add(this);
		}
	}

	private void parseSchema(AvroMetadata avroMetadata, EntityInfo parentEntity) {
		// Every entity gets an _id int field as a primary key
		FieldInfo keyField = new AvroFieldInfo(new Field("_id", Schema.create(Schema.Type.INT), null, null), true);
		fields.put(keyField.fieldName, keyField);
		this.key.add(keyField);

		// Sub entities get columns which reference their parent key
		if (parentEntity != null) {
			for (FieldInfo field : parentEntity.key) {
				keyField = new AvroFieldInfo(new Field("_parent_"+field.fieldName, ((AvroFieldInfo)field).schema_, null, null), false);
				keyField.targetEntity = parentEntity;
				keyField.targetField = field;
				fields.put(keyField.fieldName, keyField);
			}
		}

		// We handle records special
		if (schema_.getType().equals(Schema.Type.RECORD)) {
			for (Field field: schema_.getFields()) {
				FieldInfo fieldInfo = new AvroFieldInfo(field);
				fields.put(fieldInfo.fieldName, fieldInfo);

				// Handle an array
				if (fieldInfo.dbType == DatabaseFieldType.ONE_TO_MANY_INT) {
					// This is a one to many requiring a map table with int keys

					EntityInfo innerType = avroMetadata.getEntity(field.schema().getElementType().getFullName());
					if (innerType == null) {
						// Add the inner type to the entities
						innerType = new AvroEntityInfo(field.schema().getElementType(), avroMetadata, this);
					}

					fieldInfo.targetEntity = innerType;
					// TODO: Support for complex keys.
					fieldInfo.targetField = innerType.key.get(0);
				}
				// Handle a map
				else if (fieldInfo.dbType == DatabaseFieldType.ONE_TO_MANY_STRING) {
					// This is a one to many requiring a map table

					EntityInfo mapType = avroMetadata.getEntity(field.schema().getFullName());

					if (mapType == null) {
						// Add a map to get from the key to the value
						List<Field>mapFields = new ArrayList<Field>();
						mapFields.add(new Schema.Field("_key", Schema.create(Schema.Type.STRING), null, null));
						mapFields.add(new Schema.Field("_value", field.schema().getValueType(), null, null));
						Schema mapSchema = Schema.createRecord("_map_"+fieldInfo.fieldName, null, schema_.getNamespace(), false);
						mapSchema.setFields(mapFields);
						mapType = new AvroEntityInfo(mapSchema, avroMetadata, this);
					}
					fieldInfo.targetEntity = mapType;
					// TODO: Support for complex keys?
					fieldInfo.targetField = mapType.key.get(0);
				}
				// Handle a sub-record
				else if (fieldInfo.dbType == DatabaseFieldType.ONE_TO_ONE){

					// For enums we need a map of ordinal to string values.
					if (field.schema().getType() == Schema.Type.ENUM) {
						AvroEntityInfo enumType = (AvroEntityInfo) avroMetadata.getEntity(field.schema().getFullName());

						if (enumType == null) {
							// Build the schema for the enumeration values.
							Schema enumSchema = Schema.createRecord(field.schema().getName(), null, schema_.getNamespace(), false);
							ArrayList<Schema.Field> enumFields = new ArrayList<Schema.Field>();
							enumFields.add(new Schema.Field("_value", Schema.create(Schema.Type.STRING), null, null));
							enumSchema.setFields(enumFields);
							// Enumerations are special and get no parent reference since they are idempotent.
							enumType = new AvroEntityInfo(enumSchema, avroMetadata, null);
							enumType.setEnumValues(field);
						}

						fieldInfo.targetEntity = enumType;
						// TODO: Support for complex keys?
						fieldInfo.targetField = enumType.key.get(0);
					} else {
						EntityInfo innerType = avroMetadata.getEntity(field.schema().getFullName());

						// Construct the inner type entity.
						if (innerType == null) {
							innerType = new AvroEntityInfo(field.schema(), avroMetadata);
						}

						fieldInfo.targetEntity = innerType;
						// TODO: Support for complex keys?
						fieldInfo.targetField = fieldInfo.targetEntity.key.get(0);
					}
				}
			}
			// If it isn't a record make a field for it.
		} else {
			FieldInfo fieldInfo = new AvroFieldInfo(schema_, avroMetadata);
			fields.put(schema_.getFullName(), fieldInfo);
		}
		Log.d(TAG, "Constructed avro entity with name: " + name() + " in namespace: " + namespace());
		avroMetadata.put(this);
	}

	@Override
	public String name() {
		return schema_.getName();
	}

	public String namespace() {
		return schema_.getNamespace();
	}

	@Override
	public String contentType() {
		return "vnd.android.cursor.dir/vnd." + namespaceDot()  + name();
	}

	@Override
	public String itemContentType() {
		return "vnd.android.cursor.item/vnd." + namespaceDot() + name();
	}

	private void setEnumValues(Field field) {
		enumValues = new HashMap<Integer, String>();
		for (String value : field.schema().getEnumSymbols()) {
			enumValues.put(field.schema().getEnumOrdinal(value), value);
		}
	}

}
