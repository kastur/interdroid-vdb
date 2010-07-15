package interdroid.vdb.content.metadata;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public abstract class EntityInfo {

	public final Map<String,FieldInfo> fields = new HashMap<String, FieldInfo>();
	public FieldInfo idField;

	public abstract String name();

	public abstract String contentType();

	public abstract String itemContentType();

	public Collection<FieldInfo> getFields() {
		return fields.values();
	}

}