package interdroid.vdb.content.orm;

import interdroid.vdb.content.GenericContentProvider;

public class ORMGenericContentProvider extends GenericContentProvider {

	public ORMGenericContentProvider(String name, Class<?> schemaClasses) {
		super(name, new ORMMetadata(schemaClasses));
	}
}
