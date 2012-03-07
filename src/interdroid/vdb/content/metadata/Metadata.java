package interdroid.vdb.content.metadata;

import interdroid.vdb.content.EntityUriMatcher.UriMatch;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents the metadata for a database.
 *
 * @author nick &lt;palmer@cs.vu.nl&gt;
 *
 */
public abstract class Metadata {
    /**
     * Access to logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(Metadata.class);

    /**
     * The entities that live inside this database.
     */
    private Map<String, EntityInfo> entities =
            new HashMap<String, EntityInfo>();

    /**
     * The namespaces in this database.
     */
    private Map<String, String> namespaces =
            new HashMap<String, String>();

    /**
     * The namespace for this database.
     */
    private final String mNamespace;

    /**
     * Construct a new metadata for the given namespace.
     * @param namespace the namespace for this database
     */
    protected Metadata(final String namespace) {
        LOG.debug("Constructed metadata namespace: " + namespace);
        mNamespace = namespace;
        namespaces.put(mNamespace, mNamespace);
    }

    /**
     * @return the entities in this database.
     */
    public final Collection<EntityInfo> getEntities() {
        return entities.values();
    }

    /**
     * @param name the name of the entity to return
     * @return information on the named entity or null.
     */
    public final EntityInfo getEntity(final String name) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Checking for entity: " + name);
        }

        EntityInfo result = entities.get(name);

        // Check the namespaces then if we are lacking a match
        if (result == null) {
            for (String namespace : namespaces.keySet()) {
                String namespaceName = namespace + "." + name;
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Checking for entity: {}.", namespaceName);
                }
                result = entities.get(namespaceName);
                if (result != null) {
                    break;
                }
            }
        }

        if (result == null && LOG.isDebugEnabled()) {
            LOG.debug("Not found...");
            for (String key : entities.keySet()) {
                LOG.debug("Existing Entity: {}.", key);
            }
        }

        return result;
    }

    /**
     * @param uriMatch the match to retrieve info for.
     * @return the entity information for the uri contained in the match
     */
    public final EntityInfo getEntity(final UriMatch uriMatch) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Checking for entity named: " + uriMatch.entityName);
        }

        EntityInfo result = getEntity(uriMatch.entityName);

        // Check if this is something inside the parent type(s).
        if (result == null && uriMatch.parentEntityNames != null) {
            String name = uriMatch.entityName;
            for (String parent : uriMatch.parentEntityNames) {
                name = parent + "_" + name;
            }
            result = getEntity(name);
        }

        return result;
    }

    /**
     * Adds an entity to the metadata for this database.
     * @param entityInfo the information on the entity
     */
    public final void put(final EntityInfo entityInfo) {
        if (getEntity(entityInfo.getFullName()) == null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Adding entity: " + entityInfo.getFullName());
            }
            entities.put(entityInfo.getFullName(), entityInfo);
            if (!namespaces.containsKey(entityInfo.namespace())) {
                namespaces.put(entityInfo.namespace(), entityInfo.namespace());
            }
        }
    }
}

