package interdroid.vdb.persistence.api;

import interdroid.vdb.persistence.impl.VdbRepositoryImpl;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jgit.transport.SshConfigSessionFactory;
import org.eclipse.jgit.transport.SshSessionFactory;
import org.eclipse.jgit.transport.OpenSshConfig.Host;

import com.jcraft.jsch.Session;

import android.content.Context;
import android.util.Log;

public class VdbRepositoryRegistry {

	private static final String TAG = "VdbRepoReg";
	private static VdbRepositoryRegistry singletonInstance__;

	private VdbRepositoryRegistry()
	{
		// disable strict host checking for push/pull ssh connections
		SshSessionFactory.setInstance( new SshConfigSessionFactory() {
			@Override
			protected void configure(Host hc, Session session)
			{
				session.setConfig("StrictHostKeyChecking", "no");
			}
		});
	}

	public static synchronized VdbRepositoryRegistry getInstance()
	{
		if (singletonInstance__ == null) {
			singletonInstance__ = new VdbRepositoryRegistry();
		}
		return singletonInstance__;
	}


	Map<String,VdbRepositoryImpl> repositories_
		= new HashMap<String, VdbRepositoryImpl>();

	public synchronized VdbRepository addRepository(Context context,
			String repositoryName, VdbInitializer initializer)
	throws IOException
	{
		Log.d(TAG, "Registering repository: " + repositoryName);
		if (repositories_.containsKey(repositoryName)) {
			throw new IllegalStateException("Repository " + repositoryName
					+ " was already registered");
		}
    	File repoDir = context.getDir("git-" + repositoryName,
    			Context.MODE_WORLD_READABLE | Context.MODE_WORLD_WRITEABLE);
    	// If it does not exist, it will be initialized
		VdbRepositoryImpl repo = new VdbRepositoryImpl(repositoryName,
				repoDir, initializer);
		repositories_.put(repositoryName, repo);
		return repo;
	}

	public synchronized VdbRepository getRepository(String repositoryName)
	{
		return repositories_.get(repositoryName);
	}
}