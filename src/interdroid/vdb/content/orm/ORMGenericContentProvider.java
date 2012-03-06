package interdroid.vdb.content.orm;

import interdroid.vdb.content.DatabaseInitializer;
import interdroid.vdb.content.GenericContentProvider;
import interdroid.vdb.persistence.api.VdbInitializer;


// TODO: (nick) Add support for sub-tables using the avro additions to the metadata.

public class ORMGenericContentProvider extends GenericContentProvider {

    public ORMGenericContentProvider(String namespace, Class<?> schemaClasses) {
        super(namespace, new ORMMetadata(namespace, schemaClasses));
    }

    @Override
    public final VdbInitializer buildInitializer() {
        return new DatabaseInitializer(mNamespace, mMetadata);
    }
}
