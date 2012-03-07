package interdroid.vdb.content.orm;

import interdroid.vdb.content.metadata.Metadata;

/**
 * Represents metadata for a database using the ORM system.
 *
 * @author nick &lt;palmer@cs.vu.nl&gt;
 *
 */
public class ORMMetadata extends Metadata {

    /**
     * Construct metadata from the given schema class.
     * @param namespace the namespace for the database.
     * @param schemaClasses the class with annotations.
     */
    public ORMMetadata(final String namespace,
            final Class<?>... schemaClasses) {
        super(namespace);
        // TODO: Make sure all classes are in the same namespace?
        for (Class<?> clazz : schemaClasses) {
            ORMEntityInfo entityInfo = new ORMEntityInfo(clazz);
            put(entityInfo);
        }
    }
}
