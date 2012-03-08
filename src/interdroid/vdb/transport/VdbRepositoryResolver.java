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
package interdroid.vdb.transport;

import java.io.IOException;
import java.util.List;
import java.util.Map;

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

/**
 * The resolver for repositories within VDB.
 *
 * @author nick &lt;palmer@cs.vu.nl&gt;
 *
 * @param <C> The client type
 */
public class VdbRepositoryResolver<C> implements RepositoryResolver<C> {
    /**
     * Access to logger.
     */
    private static final Logger LOG = LoggerFactory
            .getLogger(VdbRepositoryResolver.class);

    /**
     * The provider registry we lookup repositories with.
     */
    private final VdbProviderRegistry mProviderRegistry;
    /**
     * The context we are running in.
     */
    private final Context mContext;

    /**
     * Construct a resovler for the given context.
     * @param context the context the resolver is running in.
     * @throws IOException if reading or writing fails.
     */
    public VdbRepositoryResolver(final Context context) throws IOException {
        // This ensures that all repositories are registered
        mProviderRegistry = new VdbProviderRegistry(context);
        mContext = context;
    }

    @Override
    public final Repository open(final C req, final String name)
            throws RepositoryNotFoundException, ServiceNotAuthorizedException,
            ServiceNotEnabledException {
        Repository result = null;
        LOG.debug("Getting repository for {}", name);
        // Make sure the provider has been initialized
        // so it is in the RepositoryRegistry properly.
        mProviderRegistry.initByName(name);
        try {
            result =  VdbRepositoryRegistry.getInstance()
                    .getJGitRepository(mContext, name);
        } catch (IOException e) {
            throw new RepositoryNotFoundException(
                    "Error fetching repository", e);
        }
        LOG.debug("Found repo: {}", result);
        return result;
    }

    /**
     * @param email the client email
     * @return the list of repositories visible to this email
     * @throws IOException if reading or writing fails.
     */
    public final List<Map<String, Object>> getRepositoryList(final String email)
            throws IOException {
        return mProviderRegistry.getAllRepositories(email);
    }

}
