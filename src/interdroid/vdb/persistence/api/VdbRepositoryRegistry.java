package interdroid.vdb.persistence.api;

import interdroid.vdb.content.VdbProviderRegistry;
import interdroid.vdb.persistence.impl.VdbRepositoryImpl;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

//import org.eclipse.jgit.transport.SshConfigSessionFactory;
//import org.eclipse.jgit.transport.SshSessionFactory;
//import org.eclipse.jgit.transport.OpenSshConfig.Host;
import org.eclipse.jgit.lib.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//import com.jcraft.jsch.Session;

import android.content.Context;

public class VdbRepositoryRegistry {
	private static final Logger logger = LoggerFactory.getLogger(VdbRepositoryRegistry.class);

	private static VdbRepositoryRegistry singletonInstance__;

	private VdbRepositoryRegistry()
	{
//		// disable strict host checking for push/pull ssh connections
//		SshSessionFactory.setInstance( new SshConfigSessionFactory() {
//			@Override
//			protected void configure(Host hc, Session session)
//			{
//				session.setConfig("StrictHostKeyChecking", "no");
//			}
//		});
	}

	public static synchronized VdbRepositoryRegistry getInstance()
	{
		if (singletonInstance__ == null) {
			singletonInstance__ = new VdbRepositoryRegistry();
		}
		return singletonInstance__;
	}


	private Map<String,VdbRepositoryImpl> repositories_
		= new HashMap<String, VdbRepositoryImpl>();

	public synchronized VdbRepository addRepository(Context context,
			String repositoryName, VdbInitializer initializer)
	throws IOException
	{
		logger.debug("Adding repo: {}", repositoryName);

		VdbRepositoryImpl repo;
		if (repositories_.containsKey(repositoryName)) {
			repo = repositories_.get(repositoryName);
//			throw new IllegalStateException("Repository " + repositoryName
//					+ " was already registered");
		} else {
			File repoDir = context.getDir("git-" + repositoryName,
					Context.MODE_WORLD_READABLE | Context.MODE_WORLD_WRITEABLE);
			// If it does not exist, it will be initialized
			repo = new VdbRepositoryImpl(repositoryName,
					repoDir, initializer);
			repositories_.put(repositoryName, repo);
		}
		return repo;
	}

	public synchronized VdbRepository getRepository(Context context, String repositoryName) throws IOException
	{
		logger.debug("Getting repository: {} : {}", repositoryName, repositories_.size());
		// Make sure the repository has been initialized.
		new VdbProviderRegistry(context).initByName(repositoryName);
		if (logger.isDebugEnabled()) {
			for(String repo : repositories_.keySet()) {
				logger.debug("Repo: {}", repo);
			}
		}
		return repositories_.get(repositoryName);
	}

	public Repository getJGitRepository(Context context, String name) throws IOException {
		VdbRepository repo = getRepository(context, name);
		if (repo != null) {
			return ((VdbRepositoryImpl) repo).getGitRepository();
		}
		return null;
	}
}
