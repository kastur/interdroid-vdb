package interdroid.vdb.content.avro;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.avro.Schema;

import android.util.Log;

import interdroid.vdb.content.metadata.EntityInfo;
import interdroid.vdb.content.metadata.Metadata;

public class AvroMetadata implements Metadata {
	private static final String TAG = "Avro";
	private final Schema schema_;
	private Map<String, EntityInfo>entities = new HashMap<String, EntityInfo>();

	public AvroMetadata(String schema) {
		schema_ = Schema.parse(schema);
		parseSchema();
	}

	private void parseSchema() {
		AvroEntityInfo info = new AvroEntityInfo(schema_, this);
		Log.d(TAG, "Loded entity: " + info.name());
		entities.put(info.name(), info);
	}

	@Override
	public Collection<EntityInfo> getEntities() {
		return entities.values();
	}

	@Override
	public EntityInfo getEntity(String name) {
		return entities.get(name);
	}

}
