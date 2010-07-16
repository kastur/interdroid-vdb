package interdroid.vdb.content.metadata;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class EntityInfo {

	public final Map<String,FieldInfo> fields = new HashMap<String, FieldInfo>();
	public final List<FieldInfo> key = new ArrayList<FieldInfo>();
	public Map<Integer, String> enumValues;
	public EntityInfo parentEntity;
	public final List<EntityInfo> children = new ArrayList<EntityInfo>();

	public abstract String name();

	public abstract String namespace();

	public final String namespaceDot() {
		return (namespace() == null || "".equals(namespace()) ? "" : namespace() + ".");
	}

	public final String getFullName() {
		if (namespace() == null || "".equals(namespace())) {
			return name();
		}
		else {
			return namespace() + "." + name();
		}
	}

	public abstract String contentType();

	public abstract String itemContentType();

	public Collection<FieldInfo> getFields() {
		return fields.values();
	}

}