package interdroid.vdb.content.avro;

import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;

import android.util.Log;

import interdroid.vdb.content.metadata.EntityInfo;
import interdroid.vdb.content.metadata.FieldInfo;

public class AvroEntityInfo extends EntityInfo {

	public static final String TAG = "AvroProvider";

	private Schema schema_;

	public AvroEntityInfo(Schema schema, AvroMetadata avroMetadata) {
		schema_ = schema;
		parseSchema(avroMetadata);
	}

	private void parseSchema(AvroMetadata avroMetadata) {
		// TODO: (nick) Support sub-records and setup foreign keys to map them.
		for (Field field: schema_.getFields()) {
			FieldInfo fieldInfo = new AvroFieldInfo(field);
			fields.put(fieldInfo.fieldName, fieldInfo);
			if (fieldInfo.isID) {
				if (idField == null) {
					idField = fieldInfo;
				} else {
					throw new IllegalArgumentException("The class specified more than one id field.");
				}
			}
		}
		if (idField == null) {
			throw new IllegalArgumentException("The class did not specify an id field.");
		}
		Log.d(TAG, "Constructed avro entity with name: " + name() + " and id: " + idField.fieldName);
	}

	@Override
	public String name() {
		Log.d(TAG, "Returning full name: " + schema_.getFullName());
		return schema_.getFullName();
	}

	@Override
	public String contentType() {
		return "vnd.android.cursor.dir/vnd." + name();
	}

	@Override
	public String itemContentType() {
		return "vnd.android.cursor.item/vnd." + name();
	}

}
