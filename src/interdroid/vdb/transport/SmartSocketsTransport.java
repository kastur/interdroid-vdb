package interdroid.vdb.transport;

import ibis.smartsockets.util.MalformedAddressException;
import ibis.smartsockets.virtual.InitializationException;
import ibis.smartsockets.virtual.VirtualSocket;
import ibis.smartsockets.virtual.VirtualSocketAddress;
import ibis.smartsockets.virtual.VirtualSocketFactory;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import org.eclipse.jgit.JGitText;
import org.eclipse.jgit.errors.NotSupportedException;
import org.eclipse.jgit.errors.TransportException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.BasePackFetchConnection;
import org.eclipse.jgit.transport.BasePackPushConnection;
import org.eclipse.jgit.transport.FetchConnection;
import org.eclipse.jgit.transport.PackTransport;
import org.eclipse.jgit.transport.PacketLineOut;
import org.eclipse.jgit.transport.PushConnection;
import org.eclipse.jgit.transport.TcpTransport;
import org.eclipse.jgit.transport.Transport;
import org.eclipse.jgit.transport.TransportProtocol;
import org.eclipse.jgit.transport.URIish;

public class SmartSocketsTransport extends TcpTransport implements PackTransport {

	private static final int IBIS_PORT = 2525;

	static final TransportProtocol PROTO_GIT = new TransportProtocol() {
		public String getName() {
			return "IbisTransport";
		}

		public Set<String> getSchemes() {
			return Collections.singleton("ibis"); //$NON-NLS-1$
		}

		public Set<URIishField> getRequiredFields() {
			return Collections.unmodifiableSet(EnumSet.of(URIishField.HOST,
					URIishField.PATH));
		}

		public Set<URIishField> getOptionalFields() {
			return Collections.unmodifiableSet(EnumSet.of(URIishField.PORT));
		}

		public int getDefaultPort() {
			return IBIS_PORT;
		}

		public Transport open(URIish uri, Repository local, String remoteName)
				throws NotSupportedException {
			return new SmartSocketsTransport(local, uri);
		}
	};

	protected SmartSocketsTransport(Repository local, URIish uri) {
		super(local, uri);
		// TODO Auto-generated constructor stub
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
		// TODO Auto-generated method stub
	}

	VirtualSocket openConnection() throws MalformedAddressException, IOException, InitializationException {
		final int tms = getTimeout() > 0 ? getTimeout() * 1000 : 0;
		final int port = uri.getPort() > 0 ? uri.getPort() : IBIS_PORT;
		VirtualSocketAddress otherSide = new VirtualSocketAddress(uri.getHost() + ":" + port);
		final VirtualSocket s = VirtualSocketFactory.getDefaultSocketFactory().createClientSocket(otherSide, tms, null);
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
		if (uri.getPort() > 0 && uri.getPort() != IBIS_PORT) {
			cmd.append(":");
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
			sock = openConnection();
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
					// Ignore errors during close.
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
			sock = openConnection();
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
					// Ignore errors during close.
				} finally {
					sock = null;
				}
			}
		}
	}

}
