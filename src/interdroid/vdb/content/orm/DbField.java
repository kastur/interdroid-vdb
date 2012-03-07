package interdroid.vdb.content.orm;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import interdroid.vdb.content.metadata.DatabaseFieldType;

/**
 * The annotation for a field in a database.
 *
 * @author nick &lt;palmer@cs.vu.nl&gt;
 *
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface DbField {
    /**
     * The type for the field.
     */
    DatabaseFieldType dbType() default DatabaseFieldType.TEXT;
    /**
     * true if this is the id field.
     */
    boolean isID() default false;
}
