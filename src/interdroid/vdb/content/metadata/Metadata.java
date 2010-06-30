package interdroid.vdb.content.metadata;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class Metadata {
	private final Map<Class<?>,EntityInfo> persistentEntities = new HashMap<Class<?>, EntityInfo>();
	private final Map<String,EntityInfo> entityInfosByName = new HashMap<String, EntityInfo>();
	
	public Metadata(Class<?>... schemaClasses) {
		for (Class<?> clazz : schemaClasses) {
			EntityInfo entityInfo = new EntityInfo(clazz);
			persistentEntities.put(clazz, entityInfo);
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
