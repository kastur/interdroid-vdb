package interdroid.vdb.content.orm;

import java.lang.reflect.Field;

import interdroid.vdb.content.metadata.EntityInfo;
import interdroid.vdb.content.metadata.FieldInfo;

/**
 * The information for an entity in the database using the ORM system.
 *
 * @author nick &lt;palmer@cs.vu.nl&gt;
 *
 */
public class ORMEntityInfo extends EntityInfo {
    /**
     * The information on this entity.
     */
    private final DbEntity options;

    /**
     * The class for this entity.
     */
    private final Class<?> clazz;

    /**
     * @return the name for this entity
     */
    public final String name() {
        return options.name();
    }

    /**
     * @return the namespace for this entity
     */
    public final String namespace() {
        return clazz.getPackage().getName();
    }

    /**
     * @return the content type for this entity
     */
    public final String contentType() {
        return options.contentType();
    }

    /**
     * @return the item content type for this entity
     */
    public final String itemContentType() {
        return options.itemContentType();
    }

    /**
     * Construct entity information from an annotated class.
     * @param table the class to get table information from
     */
    public ORMEntityInfo(final Class<?> table) {
        this.clazz = table;

        DbEntity entityOptions = clazz.getAnnotation(DbEntity.class);
        if (entityOptions == null) {
            throw new IllegalArgumentException(
                    "The class is not annotated with EntityOptions.");
        }
        this.options = entityOptions;

        for (Field f : clazz.getFields()) {
            FieldInfo fieldInfo = ORMFieldInfo.buildInfo(f);
            if (fieldInfo != null) {
                fields.put(fieldInfo.fieldName, fieldInfo);
                if (fieldInfo.isKey) {
                    this.key.add(fieldInfo);
                }
            }
        }
        if (key.size() == 0) {
            throw new IllegalArgumentException(
                    "The class did not specify an id field.");
        }
    }
}

