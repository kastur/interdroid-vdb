package interdroid.vdb.content.metadata;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface DbField {
	public enum DatabaseFieldTypes {
		INTEGER,
		REAL,
		TEXT,
		BLOB
	}

	DatabaseFieldTypes dbType() default DatabaseFieldTypes.TEXT;
	boolean isID() default false;
}
