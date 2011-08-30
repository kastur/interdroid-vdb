package interdroid.vdb.transport;

import ibis.smartsockets.SmartSocketsProperties;
import ibis.smartsockets.naming.NameResolver;
import ibis.smartsockets.util.MalformedAddressException;
import ibis.smartsockets.virtual.InitializationException;
import ibis.smartsockets.virtual.VirtualSocket;
import ibis.smartsockets.virtual.VirtualSocketAddress;
import ibis.smartsockets.virtual.VirtualSocketFactory;
import interdroid.vdb.content.VdbProviderRegistry;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.eclipse.jgit.JGitText;
import org.eclipse.jgit.errors.NotSupportedException;
import org.eclipse.jgit.errors.TransportException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.BasePackFetchConnection;
import org.eclipse.jgit.transport.BasePackPushConnection;
import org.eclipse.jgit.transport.FetchConnection;
import org.eclipse.jgit.transport.PackTransport;
import org.eclipse.jgit.transport.PacketLineIn;
import org.eclipse.jgit.transport.PacketLineOut;
import org.eclipse.jgit.transport.PushConnection;
import org.eclipse.jgit.transport.TcpTransport;
import org.eclipse.jgit.transport.Transport;
import org.eclipse.jgit.transport.TransportProtocol;
import org.eclipse.jgit.transport.URIish;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SmartSocketsTransport extends TcpTransport implements PackTransport {
	private static final Logger logger = LoggerFactory
	.getLogger(SmartSocketsTransport.class);

	public static final String SMARTSOCKETS_TRANSPORT_SCHEME = "ss";

	private static final int SMARTSOCKETS_PORT = 9090;

	public static Properties sSocketProperties = new Properties();

	static {
		sSocketProperties.put(SmartSocketsProperties.DIRECT_CACHE_IP, "false");
		//sSocketProperties.put(SmartSocketsProperties.HUB_GOSSIP_INTERVAL, "60000");
		//sSocketProperties.put(SmartSocketsProperties.HUB_ADDRESSES, "10.0.1.57-17878#3a.32.89.ff.07.7b.00.00.85.cf.70.8c.99.6a.9f.a8~nick");
		//sSocketProperties.put(SmartSocketsProperties.HUB_ADDRESSES, "130.37.152.198/130.37.29.189-17878~nick");
		sSocketProperties.put(SmartSocketsProperties.HUB_ADDRESSES, "130.37.29.189-17878~nick");
		sSocketProperties.put(SmartSocketsProperties.START_HUB, "true");
		//sSocketProperties.put(SmartSocketsProperties.HUB_ADDRESSES, "192.168.43.92-17878#e6.14.91.68.08.7b.00.00.ac.ca.9e.c7.2e.3d.4f.8e~nick");
	}

	public static final TransportProtocol PROTO = new TransportProtocol() {
		public String getName() {
			return "Smart Sockets";
		}

		public Set<String> getSchemes() {
			return Collections.singleton(SMARTSOCKETS_TRANSPORT_SCHEME); //$NON-NLS-1$
		}

		public Set<URIishField> getRequiredFields() {
			return Collections.unmodifiableSet(EnumSet.of(URIishField.HOST, URIishField.USER,
					URIishField.PATH));
		}

		public Set<URIishField> getOptionalFields() {
			return Collections.unmodifiableSet(EnumSet.of(URIishField.PORT));
		}

		public int getDefaultPort() {
			return SMARTSOCKETS_PORT;
		}

		public Transport open(URIish uri, Repository local, String remoteName)
		throws NotSupportedException {
			return new SmartSocketsTransport(local, uri);
		}
	};

	public static final String REAL_NAME = "name";
	public static final String EMAIL = "email";

	// TODO: This should come from a property or something
	private static final int TIMEOUT = 1000 * 30;

	protected SmartSocketsTransport(Repository local, URIish uri) {
		super(local, uri);
	}

	public SmartSocketsTransport(URIish uri) {
		super(null, uri);
	}

	@Override
	public FetchConnection openFetch() throws TransportException {
		try {
			return new SmartSocketsFetchConnection();
		} catch (Exception e) {
			throw new TransportException(uri, "Unable to open socket.", e);
		}
	}

	@Override
	public PushConnection openPush() throws TransportException {
		try {
			return new SmartSocketsPushConnection();
		} catch (Exception e) {
			throw new TransportException(uri, "Unable to open socket.", e);
		}
	}

	@Override
	public void close() {
		// TODO: Unregister our endpoint with smartsockets?
	}

	static VirtualSocket openConnection(URIish uri, int timeout) throws MalformedAddressException, IOException, InitializationException {
		logger.debug("Opening connection to: {} {}", uri, timeout);
		logger.debug("Resolving: {}@{}", uri.getUser(), uri.getHost());
		NameResolver resolver = getResolver();
		VirtualSocketAddress otherSide = resolver.resolve(uri.getUser() + "@" + uri.getHost(), timeout);
		logger.debug("Other side is: {}", otherSide);
		if (otherSide == null) {
			throw new IOException("Unable to resolve host: " + uri.getUser() + "@" + uri.getHost());
		}
		VirtualSocket s = resolver.getSocketFactory().createClientSocket(otherSide, timeout, null);
		return s;
	}

	void service(final String name, final PacketLineOut pckOut)
	throws IOException {
		final StringBuilder cmd = new StringBuilder();
		cmd.append(name);
		cmd.append(' ');
		cmd.append(uri.getPath());
		cmd.append('\0');
		cmd.append("host=");
		cmd.append(uri.getHost());
		cmd.append("user=");
		cmd.append(uri.getHost());
		if (uri.getPort() > 0 && uri.getPort() != SMARTSOCKETS_PORT) {
			cmd.append(':');
			cmd.append(uri.getPort());
		}
		cmd.append('\0');
		pckOut.writeString(cmd.toString());
		pckOut.flush();
	}

	class SmartSocketsFetchConnection extends BasePackFetchConnection {
		private VirtualSocket sock;

		SmartSocketsFetchConnection() throws MalformedAddressException, IOException, InitializationException {
			super(SmartSocketsTransport.this);
			sock = openConnection(uri, getTimeout());
			try {
				InputStream sIn = sock.getInputStream();
				OutputStream sOut = sock.getOutputStream();

				sIn = new BufferedInputStream(sIn);
				sOut = new BufferedOutputStream(sOut);

				init(sIn, sOut);
				service("git-upload-pack", pckOut);
			} catch (IOException err) {
				close();
				throw new TransportException(uri,
						JGitText.get().remoteHungUpUnexpectedly, err);
			}
			readAdvertisedRefs();
		}

		@Override
		public void close() {
			super.close();

			if (sock != null) {
				try {
					sock.close();
				} catch (IOException err) {
					logger.warn("Exception while closing.", err);
				} finally {
					sock = null;
				}
			}
		}
	}

	class SmartSocketsPushConnection extends BasePackPushConnection {
		private VirtualSocket sock;

		SmartSocketsPushConnection() throws MalformedAddressException, IOException, InitializationException {
			super(SmartSocketsTransport.this);
			sock = openConnection(uri, getTimeout());
			try {
				InputStream sIn = sock.getInputStream();
				OutputStream sOut = sock.getOutputStream();

				sIn = new BufferedInputStream(sIn);
				sOut = new BufferedOutputStream(sOut);

				init(sIn, sOut);
				service("git-receive-pack", pckOut);
			} catch (IOException err) {
				close();
				throw new TransportException(uri,
						JGitText.get().remoteHungUpUnexpectedly, err);
			}
			readAdvertisedRefs();
		}

		@Override
		public void close() {
			super.close();

			if (sock != null) {
				try {
					sock.close();
				} catch (IOException err) {
					logger.warn("Exception while closing.", err);
				} finally {
					sock = null;
				}
			}
		}
	}

	public static List<Map<String, Object>> getRepositories(String localEmail, String remoteEmail) throws MalformedAddressException, IOException, InitializationException {
		List<Map<String, Object>> repositories = null;
		URIish uri;
		logger.debug("Getting repositories from: " + remoteEmail);
		try {
			uri = new URIish(SMARTSOCKETS_TRANSPORT_SCHEME + "://" + remoteEmail + "/");
		} catch (URISyntaxException e) {
			throw new MalformedAddressException(e);
		}
		VirtualSocket socket = openConnection(uri, TIMEOUT);
		socket.setSoTimeout(TIMEOUT);

		PacketLineOut out = null;
		PacketLineIn in = null;
		try {
			logger.debug("Sending Command.");
			out = new PacketLineOut(socket.getOutputStream());
			out.writeString("git-list-repos " + localEmail);

			repositories = new ArrayList<Map<String, Object>>();
			logger.debug("Reading result.");
			in = new PacketLineIn(socket.getInputStream());
			for (String repository = in.readString(); !repository.equals(PacketLineIn.END); repository = in.readString()) {
				logger.debug("Read repository: {}", repository);
				Map<String, Object> repoInfo = new HashMap<String, Object>();
				repoInfo.put(VdbProviderRegistry.REPOSITORY_NAME, repository);
				repoInfo.put(VdbProviderRegistry.REPOSITORY_IS_PEER, Boolean.valueOf(in.readString()));
				repoInfo.put(VdbProviderRegistry.REPOSITORY_IS_PUBLIC, Boolean.valueOf(in.readString()));
				repositories.add(repoInfo);
			}
		} finally {
			if(in != null) {
				socket.getInputStream().close();
			}
			if (out != null) {
				socket.getOutputStream().close();
			}
			socket.close();
		}
		logger.debug("Done fetching repositories.");
		return repositories;
	}

	public static NameResolver getResolver() throws InitializationException {
		return NameResolver.getOrCreateResolver(SMARTSOCKETS_TRANSPORT_SCHEME, sSocketProperties, true);
	}

}
