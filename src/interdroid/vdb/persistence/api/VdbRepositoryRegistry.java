package interdroid.vdb.persistence.api;

import interdroid.vdb.content.VdbProviderRegistry;
import interdroid.vdb.persistence.impl.VdbRepositoryImpl;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

//import org.eclipse.jgit.transport.SshConfigSessionFactory;
//import org.eclipse.jgit.transport.SshSessionFactory;
//import org.eclipse.jgit.transport.OpenSshConfig.Host;
import org.eclipse.jgit.lib.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//import com.jcraft.jsch.Session;

import android.content.Context;

/**
 * The registry for repositories known to VDB.
 *
 * @author nick &lt;palmer@cs.vu.nl&gt;
 *
 */
public final class VdbRepositoryRegistry {
    /**
     * Access to logger.
     */
    private static final Logger LOG = LoggerFactory
            .getLogger(VdbRepositoryRegistry.class);

    /**
     * The singleton of this in the sytem.
     */
    private static final VdbRepositoryRegistry SINGLETON =
            new VdbRepositoryRegistry();

    /**
     * Prevent construction.
     */
    private VdbRepositoryRegistry() {
//        // disable strict host checking for push/pull ssh connections
//        SshSessionFactory.setInstance( new SshConfigSessionFactory() {
//            @Override
//            protected void configure(Host hc, Session session)
//            {
//                session.setConfig("StrictHostKeyChecking", "no");
//            }
//        });
    }

    /**
     * @return the instance of the registry.
     */
    public static VdbRepositoryRegistry getInstance() {
        return SINGLETON;
    }

    /**
     * The repositories known to this instance.
     */
    private Map<String, VdbRepositoryImpl> mRepositories
        = new HashMap<String, VdbRepositoryImpl>();

    /**
     * Adds the repository to the registry and constructs it.
     * @param context the context the add is being done in
     * @param repositoryName the name of the repository
     * @param initializer the initializer for the repository
     * @return the constructed repository
     * @throws IOException if reading or writing fails.
     */
    public synchronized VdbRepository addRepository(final Context context,
            final String repositoryName, final VdbInitializer initializer)
    throws IOException {
        LOG.debug("Adding repo: {}", repositoryName);

        VdbRepositoryImpl repo;
        if (mRepositories.containsKey(repositoryName)) {
            repo = mRepositories.get(repositoryName);
        } else {
            File repoDir = context.getDir("git-" + repositoryName,
                    Context.MODE_WORLD_READABLE | Context.MODE_WORLD_WRITEABLE);
            // If it does not exist, it will be initialized
            repo = new VdbRepositoryImpl(repositoryName,
                    repoDir, initializer);
            mRepositories.put(repositoryName, repo);
        }
        return repo;
    }

    /**
     * Returns a repository with the given name.
     * @param context the context being requested from
     * @param repositoryName the name of the repository
     * @return the requested repository
     * @throws IOException if reading or writing fails
     */
    public synchronized VdbRepository getRepository(final Context context,
            final String repositoryName) throws IOException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Getting repository: {} : {}", repositoryName,
                    mRepositories.size());
        }
        // Make sure the repository has been initialized.
        new VdbProviderRegistry(context).initByName(repositoryName);
        if (LOG.isDebugEnabled()) {
            for (String repo : mRepositories.keySet()) {
                LOG.debug("Repo: {}", repo);
            }
        }
        return mRepositories.get(repositoryName);
    }

    /**
     * Returns the underlying jGit repository for the repository with the given
     * name.
     * @param context the context being requested in
     * @param name the name of the desired repository
     * @return the jGit Repository
     * @throws IOException if reading or writing fail
     */
    public Repository getJGitRepository(final Context context,
            final String name) throws IOException {
        VdbRepository repo = getRepository(context, name);
        if (repo != null) {
            return ((VdbRepositoryImpl) repo).getGitRepository();
        }
        return null;
    }
}
