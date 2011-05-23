/*
 * Copyright (C) 2008-2009, Google Inc.
 * and other copyright owners as documented in the project's IP log.
 *
 * This program and the accompanying materials are made available
 * under the terms of the Eclipse Distribution License v1.0 which
 * accompanies this distribution, is reproduced below, and is
 * available at http://www.eclipse.org/org/documents/edl-v10.php
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are permitted provided that the following
 * conditions are met:
 *
 * - Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the following
 *   disclaimer in the documentation and/or other materials provided
 *   with the distribution.
 *
 * - Neither the name of the Eclipse Foundation, Inc. nor the
 *   names of its contributors may be used to endorse or promote
 *   products derived from this software without specific prior
 *   written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND
 * CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package interdroid.vdb.transport;

import ibis.smartsockets.virtual.InitializationException;
import ibis.smartsockets.virtual.VirtualServerSocket;
import ibis.smartsockets.virtual.VirtualSocket;
import ibis.smartsockets.virtual.VirtualSocketAddress;
import ibis.smartsockets.virtual.VirtualSocketFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.SocketAddress;

import org.eclipse.jgit.JGitText;
import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.pack.PackConfig;
import org.eclipse.jgit.transport.ReceivePack;
import org.eclipse.jgit.transport.UploadPack;
import org.eclipse.jgit.transport.resolver.ReceivePackFactory;
import org.eclipse.jgit.transport.resolver.RepositoryResolver;
import org.eclipse.jgit.transport.resolver.ServiceNotAuthorizedException;
import org.eclipse.jgit.transport.resolver.ServiceNotEnabledException;
import org.eclipse.jgit.transport.resolver.UploadPackFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Basic daemon for the anonymous <code>git://</code> transport protocol. */
public class SmartSocketsDaemon {
	private static final Logger logger = LoggerFactory
			.getLogger(SmartSocketsDaemon.class);

	/** 9418: IANA assigned port number for Git. */
	public static final int DEFAULT_PORT = 2525;

	private static final int BACKLOG = 5;

	private VirtualSocketAddress myAddress;

	private final SmartsocketsDaemonService[] services;

	private final ThreadGroup processors;

	private boolean run;

	private Thread acceptThread;

	private int timeout;

	private PackConfig packConfig;

	private volatile RepositoryResolver<SmartSocketsDaemonClient> repositoryResolver;

	private volatile UploadPackFactory<SmartSocketsDaemonClient> uploadPackFactory;

	private volatile ReceivePackFactory<SmartSocketsDaemonClient> receivePackFactory;

	/** Configure a daemon to listen on any available network port. */
	public SmartSocketsDaemon() {
		this(null);
	}

	/**
	 * Configure a new daemon for the specified network address.
	 *
	 * @param addr
	 *            address to listen for connections on. If null, any available
	 *            port will be chosen on all network interfaces.
	 */
	@SuppressWarnings("unchecked")
	public SmartSocketsDaemon(final VirtualSocketAddress addr) {
		myAddress = addr;
		processors = new ThreadGroup("Git-Daemon");

		repositoryResolver = (RepositoryResolver<SmartSocketsDaemonClient>) RepositoryResolver.NONE;

		uploadPackFactory = new UploadPackFactory<SmartSocketsDaemonClient>() {
			public UploadPack create(SmartSocketsDaemonClient req, Repository db)
					throws ServiceNotEnabledException,
					ServiceNotAuthorizedException {
				UploadPack up = new UploadPack(db);
				up.setTimeout(getTimeout());
				up.setPackConfig(getPackConfig());
				return up;
			}
		};

		receivePackFactory = new ReceivePackFactory<SmartSocketsDaemonClient>() {
			public ReceivePack create(SmartSocketsDaemonClient req, Repository db)
					throws ServiceNotEnabledException,
					ServiceNotAuthorizedException {
				ReceivePack rp = new ReceivePack(db);

				SocketAddress peer = req.getRemoteAddress();
				String name = "anonymous";
				String email = "anonymous@" + peer.toString();
				rp.setRefLogIdent(new PersonIdent(name, email));
				rp.setTimeout(getTimeout());

				return rp;
			}
		};

		services = new SmartsocketsDaemonService[] {
				new SmartsocketsDaemonService("upload-pack", "uploadpack") {
					{
						setEnabled(true);
					}

					@Override
					protected void execute(final SmartSocketsDaemonClient dc,
							final Repository db) throws IOException,
							ServiceNotEnabledException,
							ServiceNotAuthorizedException {
						UploadPack up = uploadPackFactory.create(dc, db);
						InputStream in = dc.getInputStream();
						OutputStream out = dc.getOutputStream();
						up.upload(in, out, null);
					}
				}, new SmartsocketsDaemonService("receive-pack", "receivepack") {
					{
						setEnabled(false);
					}

					@Override
					protected void execute(final SmartSocketsDaemonClient dc,
							final Repository db) throws IOException,
							ServiceNotEnabledException,
							ServiceNotAuthorizedException {
						ReceivePack rp = receivePackFactory.create(dc, db);
						InputStream in = dc.getInputStream();
						OutputStream out = dc.getOutputStream();
						rp.receive(in, out, null);
					}
				} };
	}

	/** @return the address connections are received on. */
	public synchronized VirtualSocketAddress getAddress() {
		return myAddress;
	}

	/**
	 * Lookup a supported service so it can be reconfigured.
	 *
	 * @param name
	 *            name of the service; e.g. "receive-pack"/"git-receive-pack" or
	 *            "upload-pack"/"git-upload-pack".
	 * @return the service; null if this daemon implementation doesn't support
	 *         the requested service type.
	 */
	public synchronized SmartsocketsDaemonService getService(String name) {
		logger.debug("Looking for service: {}", name);
		if (!name.startsWith("git-"))
			name = "git-" + name;
		for (final SmartsocketsDaemonService s : services) {
			if (s.getCommandName().equals(name))
				return s;
		}
		return null;
	}

	/** @return timeout (in seconds) before aborting an IO operation. */
	public int getTimeout() {
		return timeout;
	}

	/**
	 * Set the timeout before willing to abort an IO call.
	 *
	 * @param seconds
	 *            number of seconds to wait (with no data transfer occurring)
	 *            before aborting an IO read or write operation with the
	 *            connected client.
	 */
	public void setTimeout(final int seconds) {
		timeout = seconds;
	}

	/** @return configuration controlling packing, may be null. */
	public PackConfig getPackConfig() {
		return packConfig;
	}

	/**
	 * Set the configuration used by the pack generator.
	 *
	 * @param pc
	 *            configuration controlling packing parameters. If null the
	 *            source repository's settings will be used.
	 */
	public void setPackConfig(PackConfig pc) {
		this.packConfig = pc;
	}

	/**
	 * Set the resolver used to locate a repository by name.
	 *
	 * @param resolver
	 *            the resolver instance.
	 */
	public void setRepositoryResolver(RepositoryResolver<SmartSocketsDaemonClient> resolver) {
		repositoryResolver = resolver;
	}

	/**
	 * Set the factory to construct and configure per-request UploadPack.
	 *
	 * @param factory
	 *            the factory. If null upload-pack is disabled.
	 */
	@SuppressWarnings("unchecked")
	public void setUploadPackFactory(UploadPackFactory<SmartSocketsDaemonClient> factory) {
		if (factory != null)
			uploadPackFactory = factory;
		else
			uploadPackFactory = (UploadPackFactory<SmartSocketsDaemonClient>) UploadPackFactory.DISABLED;
	}

	/**
	 * Set the factory to construct and configure per-request ReceivePack.
	 *
	 * @param factory
	 *            the factory. If null receive-pack is disabled.
	 */
	@SuppressWarnings("unchecked")
	public void setReceivePackFactory(ReceivePackFactory<SmartSocketsDaemonClient> factory) {
		if (factory != null)
			receivePackFactory = factory;
		else
			receivePackFactory = (ReceivePackFactory<SmartSocketsDaemonClient>) ReceivePackFactory.DISABLED;
	}

	/**
	 * Start this daemon on a background thread.
	 *
	 * @throws IOException
	 *             the server socket could not be opened.
	 * @throws IllegalStateException
	 *             the daemon is already running.
	 */
	public synchronized void start() throws IOException, InitializationException {
		if (acceptThread != null)
			throw new IllegalStateException(JGitText.get().daemonAlreadyRunning);

		final VirtualServerSocket listenSock = VirtualSocketFactory.getDefaultSocketFactory().createServerSocket(DEFAULT_PORT, BACKLOG, null);
		if (listenSock == null) {
			throw new InitializationException("Unable to construct server socket.");
		}
		myAddress = listenSock.getLocalSocketAddress();

		run = true;
		acceptThread = new Thread(processors, "Git-Daemon-Accept") {
			public void run() {
				while (isRunning()) {
					try {
						logger.debug("Waiting for new connection...");
						startClient(listenSock.accept());
						logger.debug("Accepted connection");
					} catch (InterruptedIOException e) {
						// Test again to see if we should keep accepting.
					} catch (IOException e) {
						break;
					}
				}

				try {
					listenSock.close();
				} catch (IOException err) {
					//
				} finally {
					synchronized (SmartSocketsDaemon.this) {
						acceptThread = null;
					}
				}
			}
		};
		acceptThread.start();
	}

	/** @return true if this daemon is receiving connections. */
	public synchronized boolean isRunning() {
		return run;
	}

	/** Stop this daemon. */
	public synchronized void stop() {
		if (acceptThread != null) {
			run = false;
			acceptThread.interrupt();
		}
	}

	private void startClient(final VirtualSocket virtualSocket) {
		logger.debug("Starting client for: {}", virtualSocket);
		final SmartSocketsDaemonClient dc = new SmartSocketsDaemonClient(this);

		final SocketAddress peer = virtualSocket.getRemoteSocketAddress();
		dc.setRemoteAddress(peer);

		new Thread(processors, "Git-Daemon-Client " + peer.toString()) {
			public void run() {
				try {
					logger.debug("Executing client request.");
					dc.execute(virtualSocket);
				} catch (RepositoryNotFoundException e) {
					// Ignored. Client cannot use this repository.
				} catch (ServiceNotEnabledException e) {
					// Ignored. Client cannot use this repository.
				} catch (ServiceNotAuthorizedException e) {
					// Ignored. Client cannot use this repository.
				} catch (IOException e) {
					// Ignore unexpected IO exceptions from clients
					e.printStackTrace();
				} finally {
					try {
						virtualSocket.getInputStream().close();
					} catch (IOException e) {
						// Ignore close exceptions
					}
					try {
						virtualSocket.getOutputStream().close();
					} catch (IOException e) {
						// Ignore close exceptions
					}
				}
			}
		}.start();
		logger.debug("Launched client thread.");
	}

	synchronized SmartsocketsDaemonService matchService(final String cmd) {
		for (final SmartsocketsDaemonService d : services) {
			if (d.handles(cmd))
				return d;
		}
		return null;
	}

	Repository openRepository(SmartSocketsDaemonClient client, String name) {
		// Assume any attempt to use \ was by a Windows client
		// and correct to the more typical / used in Git URIs.
		//
		name = name.replace('\\', '/');

		// git://thishost/path should always be name="/path" here
		//
		if (!name.startsWith("/"))
			return null;

		try {
			return repositoryResolver.open(client, name.substring(1));
		} catch (RepositoryNotFoundException e) {
			// null signals it "wasn't found", which is all that is suitable
			// for the remote client to know.
			return null;
		} catch (ServiceNotAuthorizedException e) {
			// null signals it "wasn't found", which is all that is suitable
			// for the remote client to know.
			return null;
		} catch (ServiceNotEnabledException e) {
			// null signals it "wasn't found", which is all that is suitable
			// for the remote client to know.
			return null;
		}
	}
}
