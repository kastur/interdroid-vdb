package interdroid.vdb.content.orm;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import interdroid.vdb.content.metadata.DatabaseFieldType;

@Retention(RetentionPolicy.RUNTIME)
public @interface DbField {
	DatabaseFieldType dbType() default DatabaseFieldType.TEXT;
	boolean isID() default false;
}
