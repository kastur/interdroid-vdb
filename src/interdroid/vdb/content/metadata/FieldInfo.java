package interdroid.vdb.content.metadata;

public abstract class FieldInfo {
	public final String fieldName;
	public final DatabaseFieldType dbType;
	public final boolean isKey;
	public EntityInfo targetEntity;
	public FieldInfo targetField;

	protected FieldInfo(String fieldName, DatabaseFieldType dbType, boolean isKey) {
		this.dbType = dbType;
		this.fieldName = fieldName;
		this.isKey = isKey;
	}

	public String dbTypeName() {
		return dbType.name();
	}

}