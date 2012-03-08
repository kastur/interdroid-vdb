/*
 * Copyright (c) 2008-2012 Vrije Universiteit, The Netherlands All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * Neither the name of the Vrije Universiteit nor the names of its contributors
 * may be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS ``AS IS''
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
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

