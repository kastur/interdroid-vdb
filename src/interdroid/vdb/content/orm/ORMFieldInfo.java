package interdroid.vdb.content.orm;

import interdroid.vdb.content.metadata.FieldInfo;

import java.lang.reflect.Field;


public class ORMFieldInfo extends FieldInfo {

	private static String getFieldName(Field f) {
		try {
			return (String)f.get(null);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	private ORMFieldInfo(Field f, DbField fieldOpt) {
		super(getFieldName(f), fieldOpt.dbType(), fieldOpt.isID());
	}

	public static FieldInfo buildInfo(Field f) {
		DbField fieldOpt = f.getAnnotation(DbField.class);
		if (fieldOpt == null) {
			return null;
		}
		return new ORMFieldInfo(f, fieldOpt);
	}
}
