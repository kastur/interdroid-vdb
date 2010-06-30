package interdroid.vdb.content.metadata;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class EntityInfo {
	public final DbEntity options;
	public final Class<?> clazz;
	public final Map<String,FieldInfo> fields;
	public FieldInfo idField;
	
	public String name() {
		return options.name();
	}
	
	public String contentType() {
		return options.contentType();
	}
	
	public String itemContentType() {
		return options.itemContentType();
	}
	
	public EntityInfo(Class<?> clazz) {
		this.clazz = clazz;
		fields = new HashMap<String, FieldInfo>();
		
		DbEntity entityOptions = (DbEntity) clazz.getAnnotation(DbEntity.class);
		if (entityOptions == null) {
			throw new IllegalArgumentException("The class is not annotated with EntityOptions.");
		}
		this.options = entityOptions;
		
		for (Field f : clazz.getFields()) {
			FieldInfo fieldInfo = FieldInfo.buildInfo(f);
			if (fieldInfo != null) {
				fields.put(fieldInfo.fieldName, fieldInfo);
				if (fieldInfo.isID) {
					idField = fieldInfo;
				}
			}
		}
		if (idField == null) {
			throw new IllegalArgumentException("The class did not specify an id field.");
		}
	}
}

