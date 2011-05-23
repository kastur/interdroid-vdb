package interdroid.vdb.transport;

import java.io.IOException;

import interdroid.vdb.content.VdbProviderRegistry;
import interdroid.vdb.persistence.api.VdbRepositoryRegistry;

import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.resolver.RepositoryResolver;
import org.eclipse.jgit.transport.resolver.ServiceNotAuthorizedException;
import org.eclipse.jgit.transport.resolver.ServiceNotEnabledException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.Context;

public class VdbRepositoryResolver<C> implements RepositoryResolver<C> {
	private static final Logger logger = LoggerFactory.getLogger(VdbRepositoryResolver.class);

	private final VdbProviderRegistry mProviderRegistry;

	public VdbRepositoryResolver(Context context) throws IOException {
		// This ensures that all repositories are registered
		mProviderRegistry = new VdbProviderRegistry(context);
	}

	@Override
	public Repository open(C req, String name)
			throws RepositoryNotFoundException, ServiceNotAuthorizedException,
			ServiceNotEnabledException {
		Repository result = null;
		logger.debug("Getting repository for {}", name);
		// Make sure the provider has been initialized so it is in the RepositoryRegistry properly.
		mProviderRegistry.initByName(name);
		result =  VdbRepositoryRegistry.getInstance().getJGitRepository(name);
		logger.debug("Found repo: {}", result);
		return result;
	}

}
