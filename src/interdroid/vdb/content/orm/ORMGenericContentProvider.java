package interdroid.vdb.content.orm;

import interdroid.vdb.content.GenericContentProvider;


// TODO: (nick) Add support for sub-tables using the avro additions to the metadata.

public class ORMGenericContentProvider extends GenericContentProvider {

	public ORMGenericContentProvider(String namespace, Class<?> schemaClasses) {
		super(namespace, new ORMMetadata(namespace, schemaClasses));
	}
}
