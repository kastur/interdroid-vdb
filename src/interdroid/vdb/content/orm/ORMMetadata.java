package interdroid.vdb.content.orm;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import interdroid.vdb.content.metadata.EntityInfo;
import interdroid.vdb.content.metadata.Metadata;

public class ORMMetadata implements Metadata {
	private final Map<String,EntityInfo> persistentEntities = new HashMap<String, EntityInfo>();
	private final Map<String,EntityInfo> entityInfosByName = new HashMap<String, EntityInfo>();

	public ORMMetadata(Class<?>... schemaClasses) {
		for (Class<?> clazz : schemaClasses) {
			ORMEntityInfo entityInfo = new ORMEntityInfo(clazz);
			persistentEntities.put(clazz.getName(), entityInfo);
			entityInfosByName.put(entityInfo.name(), entityInfo);
		}
	}

	public Collection<EntityInfo> getEntities() {
		return persistentEntities.values();
	}


	public EntityInfo getEntity(String name) {
		if (!entityInfosByName.containsKey(name)) {
			throw new IllegalStateException("Unexpected entity name" + name);
		}
		return entityInfosByName.get(name);
	}
}
