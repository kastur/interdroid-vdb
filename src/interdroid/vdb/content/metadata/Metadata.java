package interdroid.vdb.content.metadata;

import java.util.Collection;

public interface Metadata {

	public Collection<EntityInfo> getEntities();

	public EntityInfo getEntity(String name);

}