package interdroid.vdb.content.metadata;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a table in the database.
 *
 * @author nick &lt;palmer@cs.vu.nl&gt;
 *
 */
public abstract class EntityInfo {

    /**
     * The fields for this table as a map of name to field info.
     */
    public final Map<String,FieldInfo> fields =
            new HashMap<String, FieldInfo>();
    /**
     * The list of fields for this table.
     */
    public final List<FieldInfo> key = new ArrayList<FieldInfo>();

    /**
     * The values for enumerations.
     */
    public Map<Integer, String> enumValues;

    /**
     * The parent entity if any.
     */
    public EntityInfo parentEntity;

    /**
     * The children of this entity if any.
     */
    public final List<EntityInfo> children = new ArrayList<EntityInfo>();

    /**
     * @return the name of this entity.
     */
    public abstract String name();

    /**
     * @return the namespace for this entity.
     */
    public abstract String namespace();

    /**
     * @return the namespace followed by a dot or the empty string
     */
    public final String namespaceDot() {
        if (namespace() == null || "".equals(namespace())) {
            return "";
        }
        return namespace() + ".";
    }

    /**
     * @return the namespace dot name.
     */
    public final String getFullName() {
        return namespaceDot() + name();
    }

    /**
     * @return the content type for a list of this table.
     */
    public abstract String contentType();

    /**
     * @return the content type for an item in this table.
     */
    public abstract String itemContentType();

    /**
     * @return the fields in this table.
     */
    public final Collection<FieldInfo> getFields() {
        return fields.values();
    }

}
