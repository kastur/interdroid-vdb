package interdroid.vdb.content.orm;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import interdroid.vdb.content.metadata.DatabaseFieldTypes;

@Retention(RetentionPolicy.RUNTIME)
public @interface DbField {
	DatabaseFieldTypes dbType() default DatabaseFieldTypes.TEXT;
	boolean isID() default false;
}
