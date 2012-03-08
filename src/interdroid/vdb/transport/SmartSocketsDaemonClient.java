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

import ibis.smartsockets.virtual.VirtualSocket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketAddress;

import org.eclipse.jgit.transport.PacketLineIn;
import org.eclipse.jgit.transport.resolver.ServiceNotAuthorizedException;
import org.eclipse.jgit.transport.resolver.ServiceNotEnabledException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Active network client of {@link Daemon}. */
public final class SmartSocketsDaemonClient {
    /**
     * Access to logger.
     */
    private static final Logger LOG = LoggerFactory
            .getLogger(SmartSocketsDaemonClient.class);

    /** Number of millis in a second.*/
    private static final int MILLIS_PER_SECOND = 1000;

    /**
     * The daemon we are working for.
     */
    private final SmartSocketsDaemon daemon;

    /**
     * The address of our peer.
     */
    private SocketAddress peer;

    /**
     * The raw input stream.
     */
    private InputStream rawIn;

    /**
     * The raw output stream.
     */
    private OutputStream rawOut;

    /**
     * Construct a new client.
     * @param d the daemon the client works for.
     */
    SmartSocketsDaemonClient(final SmartSocketsDaemon d) {
        daemon = d;
    }

    /**
     * Set the remote address.
     * @param peer2 the address of the peer.
     */
    void setRemoteAddress(final SocketAddress peer2) {
        peer = peer2;
    }

    /** @return the daemon which spawned this client. */
    public SmartSocketsDaemon getDaemon() {
        return daemon;
    }

    /** @return Internet address of the remote client. */
    public SocketAddress getRemoteAddress() {
        return peer;
    }

    /** @return input stream to read from the connected client. */
    public InputStream getInputStream() {
        return rawIn;
    }

    /** @return output stream to send data to the connected client. */
    public OutputStream getOutputStream() {
        return rawOut;
    }

    /**
     * Execute the client service.
     * @param virtualSocket the virtual socket to talk with
     * @throws IOException if reading or writing to the socket fails
     * @throws ServiceNotEnabledException if the service is not enabled
     * @throws ServiceNotAuthorizedException if the user is not authorized
     */
    void execute(final VirtualSocket virtualSocket) throws IOException,
            ServiceNotEnabledException, ServiceNotAuthorizedException {
        LOG.debug("Building streams.");
        rawIn = virtualSocket.getInputStream();
        rawOut = virtualSocket.getOutputStream();

        LOG.debug("Setting socket timeout.");
        if (0 < daemon.getTimeout()) {
            virtualSocket.setSoTimeout(daemon.getTimeout() * MILLIS_PER_SECOND);
        }
        LOG.debug("Reading string.");
        String cmd = new PacketLineIn(rawIn).readStringRaw();
        LOG.debug("Command is: {}", cmd);

        final int nul = cmd.indexOf('\0');
        if (nul >= 0) {
            // Newer clients hide a "host" header behind this byte.
            // Currently we don't use it for anything, so we ignore
            // this portion of the command.
            //
            cmd = cmd.substring(0, nul);
        }

        final SmartsocketsDaemonService srv = getDaemon().matchService(cmd);
        LOG.debug("Servicing with: {}", srv);
        if (srv == null) {
            return;
        }
        virtualSocket.setSoTimeout(0);
        srv.execute(this, cmd);
        LOG.debug("Executed service.");
    }
}
