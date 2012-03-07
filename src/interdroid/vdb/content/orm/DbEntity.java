package interdroid.vdb.content.orm;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * The annotation for database entities.
 *
 * @author nick &lt;palmer@cs.vu.nl&gt;
 *
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface DbEntity {
    /**
     * the name for this entity.
     */
    String name();
    /**
     * the list content type for this entity.
     */
    String contentType();
    /**
     * the item content type for this entity.
     */
    String itemContentType();
}
