package interdroid.vdb.content.orm;

import interdroid.vdb.content.DatabaseInitializer;
import interdroid.vdb.content.GenericContentProvider;
import interdroid.vdb.persistence.api.VdbInitializer;

/**
 * This content provider supports ORM style decleration of a table.
 * Note that this system does not support related tables.
 *
 * This support could be added but given the power and flexibility of the
 * Avro based content providers it isn't a high priority.
 *
 * @author nick &lt;palmer@cs.vu.nl&gt;
 *
 */
public class ORMGenericContentProvider extends GenericContentProvider {

    /**
     * Construct an ORM content provider for the given information.
     * @param namespace the namespace for the database.
     * @param schemaClasses the class with annotations of bd info.
     */
    public ORMGenericContentProvider(final String namespace,
            final Class<?> schemaClasses) {
        super(namespace, new ORMMetadata(namespace, schemaClasses));
    }

    @Override
    public final VdbInitializer buildInitializer() {
        return new DatabaseInitializer(mNamespace, mMetadata);
    }
}
