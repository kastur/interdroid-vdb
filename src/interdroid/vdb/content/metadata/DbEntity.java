package interdroid.vdb.content.metadata;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface DbEntity {
	public String name();
	public String contentType();
	public String itemContentType();
}
