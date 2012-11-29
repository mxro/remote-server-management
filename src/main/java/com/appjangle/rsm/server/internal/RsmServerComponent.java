package com.appjangle.rsm.server.internal;

import io.nextweb.Link;
import io.nextweb.Node;
import io.nextweb.NodeList;
import io.nextweb.Session;
import io.nextweb.common.Interval;
import io.nextweb.common.Monitor;
import io.nextweb.common.MonitorContext;
import io.nextweb.fn.Closure;
import io.nextweb.fn.ExceptionListener;
import io.nextweb.fn.ExceptionResult;
import io.nextweb.fn.Result;
import io.nextweb.fn.Success;
import io.nextweb.jre.Nextweb;

import com.appjangle.rsm.server.RsmServerConfiguration;

import de.mxro.server.ComponentConfiguration;
import de.mxro.server.ComponentContext;
import de.mxro.server.ServerComponent;
import de.mxro.server.StartCallback;

public class RsmServerComponent implements ServerComponent {

	private volatile boolean started = false;
	private volatile boolean starting = false;
	Session session;
	RsmServerConfiguration conf;
	Monitor monitor;

	@Override
	public void start(final StartCallback callback) {
		if (started || starting) {
			throw new IllegalStateException(
					"Cannot start an already started component.");
		}
		starting = true;

		session = Nextweb.createSession();

		final Link commands = session.node(conf.getCommandsNode(),
				conf.getCommandsNodeSecret());

		final Result<Monitor> monitorResult = commands.monitor(Interval.FAST,
				new Closure<MonitorContext>() {

					@Override
					public void apply(final MonitorContext ctx) {
						processRequests(ctx.node());
					}

				});

		monitorResult.get(new Closure<Monitor>() {

			@Override
			public void apply(final Monitor o) {
				monitor = o;
				starting = false;
				started = true;
			}
		});
	}

	private void processRequests(final Node node) {

		node.selectAll().get(new Closure<NodeList>() {

			@Override
			public void apply(final NodeList o) {

				processRequests(o);

			}
		});

	}

	private void processRequests(final NodeList o) {
		
		for (final Object child : o.values()) {
			
			if (child instanceof )
			
		}
		
	}

	@Override
	public void stop(final de.mxro.server.ShutdownCallback callback) {
		if (!(started || starting)) {
			throw new IllegalStateException(
					"Cannot stop an already stopped component.");
		}

		if (starting) {
			while (!started) {

			}
		}

		if (monitor != null) {
			monitor.stop().get();
		}

		final Result<Success> result = session.close();
		result.catchExceptions(new ExceptionListener() {

			@Override
			public void onFailure(final ExceptionResult r) {
				callback.onFailure(r.exception());
			}
		});
		result.get(new Closure<Success>() {

			@Override
			public void apply(final Success o) {
				started = false;
				callback.onShutdownComplete();
			}
		});
	}

	@Override
	public void injectConfiguration(final ComponentConfiguration conf) {
		this.conf = (RsmServerConfiguration) conf;
	}

	@Override
	public void injectContext(final ComponentContext context) {
		// not required
	}

	@Override
	public ComponentConfiguration getConfiguration() {
		return conf;
	}

}
