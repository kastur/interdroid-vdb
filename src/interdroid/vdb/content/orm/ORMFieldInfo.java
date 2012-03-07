package interdroid.vdb.content.orm;

import interdroid.vdb.content.metadata.FieldInfo;

import java.lang.reflect.Field;

/**
 * This class represents a field in the ORM system.
 *
 * @author nick &lt;palmer@cs.vu.nl&gt;
 *
 */
public final class ORMFieldInfo extends FieldInfo {

    /**
     * Constructs a field info for the given field and options.
     * @param f the field to construct from
     * @param fieldOpt the annotations for this field
     */
    private ORMFieldInfo(final Field f, final DbField fieldOpt) {
        super(f.getName(), fieldOpt.dbType(), fieldOpt.isID());
    }

    /**
     * Build information for a field.
     * @param f the field to build for
     * @return the constructed FieldInfo or null
     */
    public static FieldInfo buildInfo(final Field f) {
        DbField fieldOpt = f.getAnnotation(DbField.class);
        if (fieldOpt == null) {
            return null;
        }
        return new ORMFieldInfo(f, fieldOpt);
    }
}
