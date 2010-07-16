package interdroid.vdb.content.orm;

import interdroid.vdb.content.metadata.Metadata;

public class ORMMetadata extends Metadata {

	public ORMMetadata(Class<?>... schemaClasses) {
		super(schemaClasses.getClass().getPackage().getName());
		for (Class<?> clazz : schemaClasses) {
			ORMEntityInfo entityInfo = new ORMEntityInfo(clazz);
			put(entityInfo);
		}
	}
}
