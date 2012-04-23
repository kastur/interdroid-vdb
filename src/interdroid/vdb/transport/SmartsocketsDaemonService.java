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

import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.Config.SectionParser;
import org.eclipse.jgit.transport.resolver.ServiceNotAuthorizedException;
import org.eclipse.jgit.transport.resolver.ServiceNotEnabledException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** A service exposed by {@link Daemon} over anonymous <code>git://</code>. */
public abstract class SmartsocketsDaemonService {
	/**
	 * Access to logger.
	 */
	private static final Logger LOG = LoggerFactory
			.getLogger(SmartsocketsDaemonService.class);

	/**
	 * The command this service supports.
	 */
	private final String command;

	/**
	 * The configuration.
	 */
	private final SectionParser<ServiceConfig> configKey;

	/**
	 * Is this service enabled.
	 */
	private boolean enabled;

	/**
	 * Is this service overridable.
	 */
	private boolean overridable;

	/**
	 * Construct a new service handling the given commands.
	 * @param cmdName the command name
	 * @param cfgName the configuration key name
	 */
	SmartsocketsDaemonService(final String cmdName, final String cfgName) {
		command = cmdName.startsWith("git-") ? cmdName : "git-" + cmdName;
		configKey = new SectionParser<ServiceConfig>() {
			public ServiceConfig parse(final Config cfg) {
				return new ServiceConfig(
						SmartsocketsDaemonService.this, cfg, cfgName);
			}
		};
		overridable = true;
	}

	/**
	 * The configuration for the service.
	 *
	 * @author nick &lt;palmer@cs.vu.nl&gt;
	 *
	 */
	private static class ServiceConfig {
		/** Is this service enabled. */
		private final boolean enabled;

		/**
		 * Construct a service configuration.
		 * @param service The service we want config for.
		 * @param cfg the config to load from
		 * @param name the configuration name to load with
		 */
		ServiceConfig(final SmartsocketsDaemonService service, final Config cfg,
				final String name) {
			enabled = cfg.getBoolean("daemon", name, service.isEnabled());
		}
	}

	/** @return is this service enabled for invocation? */
	public final boolean isEnabled() {
		return enabled;
	}

	/**
	 * @param on
	 *            true to allow this service to be used; false to deny it.
	 */
	public final void setEnabled(final boolean on) {
		enabled = on;
	}

	/** @return can this service be configured in the repository config file? */
	public final boolean isOverridable() {
		return overridable;
	}

	/**
	 * @param on
	 *            true to permit repositories to override this service's enabled
	 *            state with the <code>daemon.servicename</code> config setting.
	 */
	public final void setOverridable(final boolean on) {
		overridable = on;
	}

	/** @return name of the command requested by clients. */
	public final String getCommandName() {
		return command;
	}

	/**
	 * Determine if this service can handle the requested command.
	 *
	 * @param commandLine
	 *            input line from the client.
	 * @return true if this command can accept the given command line.
	 */
	public final boolean handles(final String commandLine) {
		LOG.debug("Checking for match: {} {}", command, commandLine);
		return command.length() + 1 < commandLine.length()
				&& commandLine.charAt(command.length()) == ' '
				&& commandLine.startsWith(command);
	}

	/**
	 * Execute this service for the given command from the given client.
	 * @param client the client to execute for
	 * @param commandLine the command the client asked for
	 * @throws IOException if reading or writing failed
	 * @throws ServiceNotEnabledException if the service is not enabled
	 * @throws ServiceNotAuthorizedException if the user is not authorized
	 */
	void execute(final SmartSocketsDaemonClient client,
			final String commandLine)
			throws IOException, ServiceNotEnabledException,
			ServiceNotAuthorizedException {
		final String name = commandLine.substring(command.length() + 1);
		LOG.debug("Got request for repo: {}", name);
		// This should be based on some kind of type flag
		Repository db = null;
		if (name != null && name.length() > 0) {
			db = client.getDaemon().openRepository(client, name);
			if (db == null) {
				return;
			}
		}
		try {
			if (isEnabledFor(db)) {
				execute(client, db);
			}
		} finally {
			if (db != null) {
				db.close();
			}
		}
	}

	/**
	 * @param db the repository being checked
	 * @return true if this service is enabled for this repository.
	 */
	private boolean isEnabledFor(final Repository db) {
		if (db != null) {
			if (isOverridable()) {
				return db.getConfig().get(configKey).enabled;
			}
		}
		return isEnabled();
	}

	/**
	 * Exceute the actual client service. Implemented by subclasses.
	 * @param client the client to execute for
	 * @param db the repository to execute with
	 * @throws IOException if reading or writing fail
	 * @throws ServiceNotEnabledException if the service is not enabled
	 * @throws ServiceNotAuthorizedException if the service is not authorized
	 */
	abstract void execute(SmartSocketsDaemonClient client, Repository db)
			throws IOException, ServiceNotEnabledException,
			ServiceNotAuthorizedException;
}
