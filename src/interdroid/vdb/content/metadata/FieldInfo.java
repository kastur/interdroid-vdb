package interdroid.vdb.content.metadata;

/**
 * The information for a field in a table.
 *
 * @author nick &lt;palmer@cs.vu.nl&gt;
 *
 */
public abstract class FieldInfo {
    /**
     * The name of the field.
     */
    public final String fieldName;

    /**
     * The type of this field in the database.
     */
    public final DatabaseFieldType dbType;

    /**
     * True if this field is part of the key in the table.
     */
    public final boolean isKey;

    /**
     * The entity this field targets if it is a relation field.
     */
    public EntityInfo targetEntity;

    /**
     * The field this field targets if it is a relation field.
     */
    public FieldInfo targetField;

    /**
     * Construct a FieldInfo.
     * @param fieldName the name of the field
     * @param dbType the type of the field
     * @param isKey true if this is a key field.
     */
    protected FieldInfo(final String fieldName,
            final DatabaseFieldType dbType, final boolean isKey) {
        this.dbType = dbType;
        this.fieldName = fieldName;
        this.isKey = isKey;
    }

    /**
     * @return the name of the database type
     */
    public final String dbTypeName() {
        return dbType.name();
    }

}
