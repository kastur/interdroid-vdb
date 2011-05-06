package interdroid.vdb.content.orm;

import interdroid.vdb.content.metadata.Metadata;

public class ORMMetadata extends Metadata {

	public ORMMetadata(String namespace, Class<?>... schemaClasses) {
		super(namespace);
		// TODO: Make sure all classes are in the same namespace
		for (Class<?> clazz : schemaClasses) {
			ORMEntityInfo entityInfo = new ORMEntityInfo(clazz);
			put(entityInfo);
		}
	}
}
