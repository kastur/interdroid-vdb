package interdroid.vdb.transport;

import ibis.smartsockets.SmartSocketsProperties;
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
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

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

	private VirtualSocketAddress mLocalAddress;

	private final SmartsocketsDaemonService[] mServices;

	private final ThreadGroup mProcessors;

	private boolean mRun;

	private Thread mAcceptThread;

	private int mTimeout;

	private PackConfig mPackConfig;

	private volatile RepositoryResolver<SmartSocketsDaemonClient> mRepositoryResolver;

	private volatile UploadPackFactory<SmartSocketsDaemonClient> mUploadPackFactory;

	private volatile ReceivePackFactory<SmartSocketsDaemonClient> mReceivePackFactory;

	private VirtualServerSocket mListenSock;

	private VirtualSocketFactory mSocketFactory;

	private static Properties sSocketProperties = new Properties();

	static {
		sSocketProperties.put(SmartSocketsProperties.DIRECT_CACHE_IP, "false");
	}

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
		mLocalAddress = addr;
		mProcessors = new ThreadGroup("Git-Daemon");

		mRepositoryResolver = (RepositoryResolver<SmartSocketsDaemonClient>) RepositoryResolver.NONE;

		mUploadPackFactory = new UploadPackFactory<SmartSocketsDaemonClient>() {
			public UploadPack create(SmartSocketsDaemonClient req, Repository db)
					throws ServiceNotEnabledException,
					ServiceNotAuthorizedException {
				UploadPack up = new UploadPack(db);
				up.setTimeout(getTimeout());
				up.setPackConfig(getPackConfig());
				return up;
			}
		};

		mReceivePackFactory = new ReceivePackFactory<SmartSocketsDaemonClient>() {
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

		mServices = new SmartsocketsDaemonService[] {
				new SmartsocketsDaemonService("list-repos", "listrepos") {
					{
						setEnabled(true);
						setOverridable(false);
					}

					@Override
					protected void execute(final SmartSocketsDaemonClient dc,
							final Repository db) throws IOException,
							ServiceNotEnabledException,
							ServiceNotAuthorizedException {
						logger.debug("List Repos Called");
						throw new RuntimeException("Not yet implemented");
					}
				},
				new SmartsocketsDaemonService("upload-pack", "uploadpack") {
					{
						setEnabled(true);
					}

					@Override
					protected void execute(final SmartSocketsDaemonClient dc,
							final Repository db) throws IOException,
							ServiceNotEnabledException,
							ServiceNotAuthorizedException {
						UploadPack up = mUploadPackFactory.create(dc, db);
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
						ReceivePack rp = mReceivePackFactory.create(dc, db);
						InputStream in = dc.getInputStream();
						OutputStream out = dc.getOutputStream();
						rp.receive(in, out, null);
					}
				} };
	}

	/** @return the address connections are received on. */
	public synchronized VirtualSocketAddress getAddress() {
		return mLocalAddress;
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
		for (final SmartsocketsDaemonService s : mServices) {
			if (s.getCommandName().equals(name))
				return s;
		}
		return null;
	}

	/** @return timeout (in seconds) before aborting an IO operation. */
	public int getTimeout() {
		return mTimeout;
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
		mTimeout = seconds;
	}

	/** @return configuration controlling packing, may be null. */
	public PackConfig getPackConfig() {
		return mPackConfig;
	}

	/**
	 * Set the configuration used by the pack generator.
	 *
	 * @param pc
	 *            configuration controlling packing parameters. If null the
	 *            source repository's settings will be used.
	 */
	public void setPackConfig(PackConfig pc) {
		this.mPackConfig = pc;
	}

	/**
	 * Set the resolver used to locate a repository by name.
	 *
	 * @param resolver
	 *            the resolver instance.
	 */
	public void setRepositoryResolver(RepositoryResolver<SmartSocketsDaemonClient> resolver) {
		mRepositoryResolver = resolver;
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
			mUploadPackFactory = factory;
		else
			mUploadPackFactory = (UploadPackFactory<SmartSocketsDaemonClient>) UploadPackFactory.DISABLED;
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
			mReceivePackFactory = factory;
		else
			mReceivePackFactory = (ReceivePackFactory<SmartSocketsDaemonClient>) ReceivePackFactory.DISABLED;
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
		if (mAcceptThread != null)
			throw new IllegalStateException(JGitText.get().daemonAlreadyRunning);

		mSocketFactory = VirtualSocketFactory.createSocketFactory(sSocketProperties, true);
		mListenSock = mSocketFactory.createServerSocket(DEFAULT_PORT, BACKLOG, null);
		if (mListenSock == null) {
			throw new InitializationException("Unable to construct server socket.");
		}
		mLocalAddress = mListenSock.getLocalSocketAddress();

		mRun = true;
		mAcceptThread = new Thread(mProcessors, "Git-Daemon-Accept") {
			public void run() {
				while (isRunning()) {
					try {
						logger.debug("Waiting for new connection...");
						startClient(mListenSock.accept());
						logger.debug("Accepted connection");
					} catch (InterruptedIOException e) {
						// Test again to see if we should keep accepting.
					} catch (IOException e) {
						break;
					}
				}
				logger.debug("No longer running.");
				try {
					logger.debug("Closing listen socket.");
					mListenSock.close();
					logger.debug("Listen socket closed.");
				} catch (IOException err) {
					//
				}
			}
		};
		mAcceptThread.start();
	}

	/** @return true if this daemon is receiving connections. */
	public synchronized boolean isRunning() {
		return mRun;
	}

	/** Stop this daemon. */
	public void stop() {
		if (mAcceptThread != null) {
			synchronized (this) {
				mRun = false;
				try {
					logger.debug("Closing accept socket.");
					mListenSock.close();
					mAcceptThread.interrupt();
					//mSocketFactory.end();
				} catch (Exception e1) {
					logger.error("Exception while closing accept socket.", e1);
				}
			}
			logger.debug("Joining accept thread.");
			try {
				mAcceptThread.join();
			} catch (InterruptedException e) {
				logger.error("Error while joining accept thread.", e);
			}
			logger.debug("Joined accept thread.");
			mAcceptThread = null;
		}
	}

	private void startClient(final VirtualSocket virtualSocket) {
		logger.debug("Starting client for: {}", virtualSocket);
		final SmartSocketsDaemonClient dc = new SmartSocketsDaemonClient(this);

		final SocketAddress peer = virtualSocket.getRemoteSocketAddress();
		dc.setRemoteAddress(peer);

		new Thread(mProcessors, "Git-Daemon-Client " + peer.toString()) {
			public void run() {
				try {
					logger.debug("Executing client request.");
					dc.execute(virtualSocket);
					logger.debug("Client request handled.");
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
					logger.debug("Closing streams");
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
					logger.debug("Closing socket.");
					try {
						virtualSocket.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				logger.debug("Thread is complete.");
			}
		}.start();
		logger.debug("Launched client thread.");
	}

	synchronized SmartsocketsDaemonService matchService(final String cmd) {
		for (final SmartsocketsDaemonService d : mServices) {
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
			return mRepositoryResolver.open(client, name.substring(1));
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
