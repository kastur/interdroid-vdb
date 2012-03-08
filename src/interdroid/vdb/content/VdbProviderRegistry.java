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
package interdroid.vdb.content;

import interdroid.vdb.Authority;
import interdroid.vdb.content.EntityUriMatcher.MatchType;
import interdroid.vdb.content.EntityUriMatcher.UriMatch;
import interdroid.vdb.content.VdbConfig.RepositoryConf;
import interdroid.vdb.content.avro.AvroContentProvider;
import interdroid.vdb.content.avro.AvroProviderRegistry;
import interdroid.vdb.content.avro.AvroSchemaRegistrationHandler;
import interdroid.vdb.persistence.api.VdbInitializer;
import interdroid.vdb.persistence.api.VdbRepository;
import interdroid.vdb.persistence.api.VdbRepositoryRegistry;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.ContentProvider;
import android.content.Context;
import android.net.Uri;

/**
 * This class acts as a registry for content providers within the VDB system.
 *
 * @author nick &lt;palmer@cs.vu.nl&gt;
 *
 */
public class VdbProviderRegistry {
    /**
     * Access to logger.
     */
    private static final Logger LOG =
            LoggerFactory.getLogger(VdbProviderRegistry.class);

    /**
     * The base for all types.
     */
    private static final String BASE_TYPE = "vnd." + Authority.VDB;

    /**
     * The context the registry is working in.
     */
    private final Context mContext;

    /**
     * Hash of information about the repositories we know about.
     */
    private static final Map<String, RepositoryInfo> REPOS =
            new HashMap<String, RepositoryInfo>();

    /**
     * Constant for the key of the repository name.
     */
    public static final String REPOSITORY_NAME = "repoName";
    /**
     * Constant for the key if the repository is a peer repo.
     */
    public static final String REPOSITORY_IS_PEER = "isPeer";
    /**
     * Constant for the key if the repository is public.
     */
    public static final String REPOSITORY_IS_PUBLIC = "isPublic";
    /**
     * Constant for the ID column.
     */
    private static final String REPOSITORY_ID = "id_";

    /**
     * A class which holds information about a repository.
     * @author nick &lt;palmer@cs.vu.nl&gt;
     *
     */
    private static class RepositoryInfo {
        /**
         * The configuration for the repository.
         */
        private final RepositoryConf mConf;

        /**
         * The generic content provider for this repository.
         */
        private GenericContentProvider mProvider = null;

        /**
         * Constructs with the given configuration.
         * @param conf the configuration to construct with
         */
        public RepositoryInfo(final RepositoryConf conf) {
            mConf = conf;
        }
    }

    /**
     * Constructs the provider registry for use in the given context.
     * @param context the context to work in
     * @throws IOException if the database can not be accessed
     */
    public VdbProviderRegistry(final Context context) throws IOException {
        mContext = context;

        if (REPOS.size() == 0) {
            LOG.debug("Initializing static repositories.");
            try {
                VdbConfig config = new VdbConfig(context);
                initializeAll(config.getRepositories());
            } catch (Exception e) {
                // Ignore.
                LOG.warn("Ignoring error while fetching ORM repositories.", e);
            }

            LOG.debug("Initializing Avro Repos.");
            List<RepositoryConf> infos =
                    ((AvroProviderRegistry) get(
                            AvroSchemaRegistrationHandler.URI))
                            .getAllRepositories();
            initializeAll(infos);
            LOG.debug("All repositories registered.");
        }
    }

    /**
     * Initializes all repositories in the given list.
     * @param repositories the list of repositories
     * @throws IOException if something goes wrong.
     */
    private void initializeAll(final List<RepositoryConf> repositories)
            throws IOException {
        // Initialize all the child content providers, one for each repository.
        for (RepositoryConf repoConf : repositories) {
            registerRepository(repoConf);
        }
    }

    /**
     * Adds the given repository to the registry.
     * @param repoConf the configuration for the repository
     */
    public final void registerRepository(final RepositoryConf repoConf) {
        RepositoryInfo repoInfo = new RepositoryInfo(repoConf);
        if (!REPOS.containsKey(repoInfo.mConf.getName())) {
            LOG.debug("Storing into repoInfos: {}", repoInfo.mConf.getName());
            REPOS.put(repoInfo.mConf.getName(), repoInfo);
        }
    }

    /**
     * Initializes the given repo.
     * @param context the context to work in
     * @param name the repository name
     * @param initializer the initializer for the repository
     * @throws IOException if the repo cannot be initialized
     */
    private void initializeRepo(final Context context, final String name,
            final VdbInitializer initializer) throws IOException {
        LOG.debug("Initializing repository: {}", name);
        VdbRepositoryRegistry.getInstance().addRepository(context,
                name, initializer);
    }

    /**
     * Builds a provider for the given repository info.
     * @param context the context to work in
     * @param info the info on the repository
     * @throws IOException if there is a problem reading or writing the repo
     */
    private void buildProvider(final Context context, final RepositoryInfo info)
            throws IOException {
        LOG.debug("Building provider for: {}", info.mConf.getName());
        try {
            if (info.mProvider == null) {
                if (info.mConf.getAvroSchema() != null) {
                    info.mProvider =
                            new AvroContentProvider(info.mConf.getAvroSchema());
                } else {
                    info.mProvider = (GenericContentProvider)
                            Class.forName(
                                    info.mConf.getContentProvider())
                                    .newInstance();
                }
                initializeRepo(mContext, info.mConf.getName(),
                        info.mProvider.buildInitializer());

                // Do this at the end, since onCreate will be called in child
                // We want everything to be registered prior to this happening.
                LOG.debug("Attaching context: {} to provider.", context);
                info.mProvider.attachInfo(context, null);
                LOG.debug("Initialized Repository: " + info.mConf.getName());
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @param uri the uri a content provider is desired for.
     * @return a content provider for the given uri.
     */
    public final ContentProvider get(final Uri uri) {
        UriMatch match = EntityUriMatcher.getMatch(uri);
        RepositoryInfo info = REPOS.get(match.repositoryName);
        validateUri(uri, info, match);
        try {
            buildProvider(mContext, info);
        } catch (IOException e) {
            throw new RuntimeException("Unable to build provider.", e);
        }
        return info.mProvider;
    }

    /**
     * Validates the given uri against the info and match.
     * @param uri the uri given
     * @param info the info on the repository
     * @param match the match for this uri
     */
    private void validateUri(final Uri uri, final RepositoryInfo info,
            final UriMatch match) {
        if (info == null) {
            throw new IllegalArgumentException(
                    "Bad URI: unregistered repository: "
            + match.repositoryName);
        }
        if (match.type == MatchType.REPOSITORY) {
            throw new IllegalArgumentException(
                    "Bad URI: only repository was specified. " + uri);
        }
    }

    /**
     * @param uri the uri the type is desired for
     * @return the type for the given URI.
     */
    public final String getType(final Uri uri) {
        UriMatch match = EntityUriMatcher.getMatch(uri);
        RepositoryInfo info = REPOS.get(match.repositoryName);
        String type = null;

        if (info == null) {
            throw new IllegalArgumentException(
                    "Bad URI: unregistered repository. " + uri);
        }
        LOG.debug("Getting type: {} : {}", match.entityName, match.type);
        if (match.entityName == null) { // points to actual commit/branch
            switch (match.type) {
            // TODO: These should come from the type short strings.
            case REPOSITORY:
                type = BASE_TYPE + "/repository";
                break;
            case COMMIT:
                type = BASE_TYPE + "/commit";
                break;
            case LOCAL_BRANCH:
                type = BASE_TYPE + "/branch.local";
                break;
            case REMOTE_BRANCH:
                type = BASE_TYPE + "/branch.remote";
                break;
            case REMOTE:
                type = BASE_TYPE + "/remote";
                break;
            default:
                LOG.error("Unknown match type: " + match.type);
                throw new RuntimeException("Unknown match type:" + match.type);
            }
        } else {
            // Make sure provider is initialized
            initByName(info.mConf.getName());
            LOG.debug("Asking provider for type: {}", uri);
            type = info.mProvider.getType(uri);
        }
        LOG.debug("Returning type: {}", type);
        return type;
    }

    /**
     * Initializes the repository provider by name.
     * @param name the name of the repository to initialize
     */
    public final void initByName(final String name) {
        RepositoryInfo info = REPOS.get(name);
        try {
            buildProvider(mContext, info);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @return all repositories we know of.
     */
    public final List<Map<String, Object>> getAllRepositories() {
        ArrayList<Map<String, Object>> repositories =
                new ArrayList<Map<String, Object>>();
        for (RepositoryInfo info : REPOS.values()) {
            // We exclude all interdroid repositories
            if (!info.mConf.getName().startsWith("interdroid.vdb")) {
                HashMap<String, Object> map = new HashMap<String, Object>();
                map.put(REPOSITORY_ID, info.mConf.getName().hashCode());
                map.put(REPOSITORY_NAME, info.mConf.getName());
                repositories.add(map);
            }
        }
        return repositories;
    }

    /**
     * @return a list of all repository names
     */
    public final List<String> getAllRepositoryNames() {
        ArrayList<String> repositories = new ArrayList<String>();
        for (RepositoryInfo info : REPOS.values()) {
            // We exclude all interdroid repositories
            if (!info.mConf.getName().startsWith("interdroid.vdb")) {
                repositories.add(info.mConf.getName());
            }
        }
        return repositories;
    }

    /**
     * @param email the email to search for.
     * @return all repositories for the given email
     * @throws IOException if the database cannot be read
     */
    public final List<Map<String, Object>> getAllRepositories(
            final String email) throws IOException {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        LOG.debug("Getting repos for: {}", email);
        for (RepositoryInfo info : REPOS.values()) {
            // We exclude all interdroid repositories
            if (!info.mConf.getName().startsWith("interdroid.vdb")) {
                Map<String, Object> map = new HashMap<String, Object>();
                map.put(REPOSITORY_NAME, info.mConf.getName());
                // Make sure it is initialized
                initByName(info.mConf.getName());
                // Pull the repo out
                VdbRepository repo = VdbRepositoryRegistry.getInstance()
                        .getRepository(mContext, info.mConf.getName());
                // Is it a peer?
                if (null != repo.getRemoteInfo(email)) {
                    map.put(REPOSITORY_IS_PEER, true);
                } else {
                    map.put(REPOSITORY_IS_PEER, false);
                }
                map.put(REPOSITORY_IS_PUBLIC, repo.isPublic());
                result.add(map);
            }
        }

        return result;
    }
}
