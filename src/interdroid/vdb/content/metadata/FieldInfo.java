package interdroid.vdb.content.metadata;

public abstract class FieldInfo {
	public final String fieldName;
	public final boolean isID;
	public final DatabaseFieldTypes dbType;

	protected FieldInfo(String fieldName, DatabaseFieldTypes dbType, boolean isID) {
		this.dbType = dbType;
		this.isID = isID;
		this.fieldName = fieldName;

		if (isID && dbType != DatabaseFieldTypes.INTEGER) {
			throw new IllegalArgumentException("Only integer types are supported for the ID field.");
		}
		if (isID && !"_id".equals(fieldName)) {
			throw new IllegalArgumentException("Identifier field should have _id as value.");
		}
	}

	public String dbTypeName() {
		return dbType.name();
	}

}