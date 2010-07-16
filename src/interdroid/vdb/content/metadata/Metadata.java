package interdroid.vdb.content.metadata;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import android.util.Log;

public abstract class Metadata {

	private static final String TAG = "Metadata";

	// The entities that live inside this database
	protected Map<String, EntityInfo> entities = new HashMap<String, EntityInfo>();

	// The name of the database.
	public final String namespace_;

	protected Metadata(String namespace) {
		namespace_ = namespace;
	}

	public Collection<EntityInfo> getEntities() {
		return entities.values();
	}

	public EntityInfo getEntity(String name) {
		Log.d(TAG, "Checking for entity: " + name);
		EntityInfo result = entities.get(name);
		// Check the default namespace then if we don't have a match
		if (result == null && !name.startsWith(namespace_)) {
			Log.d(TAG, "Checking for entity: " + namespace_ + "." + name);
			result = entities.get(namespace_ + "." + name);
		}
		return result;
	}

	public void put(EntityInfo entityInfo) {
		if (getEntity(entityInfo.getFullName()) == null) {
			Log.d(TAG, "Adding entity: " + entityInfo.getFullName());
			entities.put(entityInfo.getFullName(), entityInfo);
		}
	}
}