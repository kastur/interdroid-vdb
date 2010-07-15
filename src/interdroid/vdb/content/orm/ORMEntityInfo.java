package interdroid.vdb.content.orm;

import java.lang.reflect.Field;

import interdroid.vdb.content.metadata.EntityInfo;
import interdroid.vdb.content.metadata.FieldInfo;

public class ORMEntityInfo extends EntityInfo {
	public final DbEntity options;
	public final Class<?> clazz;

	public String name() {
		return options.name();
	}

	public String contentType() {
		return options.contentType();
	}

	public String itemContentType() {
		return options.itemContentType();
	}

	public ORMEntityInfo(Class<?> clazz) {
		this.clazz = clazz;

		DbEntity entityOptions = (DbEntity) clazz.getAnnotation(DbEntity.class);
		if (entityOptions == null) {
			throw new IllegalArgumentException("The class is not annotated with EntityOptions.");
		}
		this.options = entityOptions;

		for (Field f : clazz.getFields()) {
			FieldInfo fieldInfo = ORMFieldInfo.buildInfo(f);
			if (fieldInfo != null) {
				fields.put(fieldInfo.fieldName, fieldInfo);
				if (fieldInfo.isID) {
					if (idField == null) {
						idField = fieldInfo;
					} else {
						throw new IllegalArgumentException("The class specified multiple ID columns.");
					}
				}
			}
		}
		if (idField == null) {
			throw new IllegalArgumentException("The class did not specify an id field.");
		}
	}
}

