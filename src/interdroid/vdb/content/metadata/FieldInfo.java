package interdroid.vdb.content.metadata;

import interdroid.vdb.content.metadata.DbField.DatabaseFieldTypes;

import java.lang.reflect.Field;


public class FieldInfo {
	public final String fieldName;
	public final DbField.DatabaseFieldTypes dbType;
	public final boolean isID;
			
	private FieldInfo(Field f, DbField fieldOpt) {
		try {
			fieldName = (String)f.get(null);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
		
		dbType = fieldOpt.dbType();
		isID = fieldOpt.isID();
		if (isID && fieldOpt.dbType() != DatabaseFieldTypes.INTEGER) {
			throw new IllegalArgumentException("Only integer types are supported for the ID field.");
		}
		if (isID && !"_id".equals(fieldName)) {
			throw new IllegalArgumentException("Identifier field should have _id as value.");
		}
	}
	
	public static FieldInfo buildInfo(Field f) {
		DbField fieldOpt = f.getAnnotation(DbField.class);
		if (fieldOpt == null) {
			return null;
		}
		return new FieldInfo(f, fieldOpt);
	}
}
