package com.appjangle.rsm.server.internal;

import io.nextweb.Link;
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
import one.utils.server.ComponentConfiguration;
import one.utils.server.ComponentContext;
import one.utils.server.ServerComponent;
import one.utils.server.ShutdownCallback;
import one.utils.server.StartCallback;

import com.appjangle.rsm.server.RsmServerConfiguration;

public class RsmServerComponent implements ServerComponent {

	private volatile boolean started = false;
	Session session;
	RsmServerConfiguration conf;
	Monitor monitor;

	@Override
	public void start(final StartCallback callback) {
		if (started) {
			throw new IllegalStateException(
					"Cannot start an already started component.");
		}
		started = true;

		session = Nextweb.createSession();

		final Link commands = session.node(conf.getCommandsNode(),
				conf.getCommandsNodeSecret());

		final Result<Monitor> monitorResult = commands.monitor(Interval.FAST,
				new Closure<MonitorContext>() {

					@Override
					public void apply(final MonitorContext ctx) {

					}
				});

		monitorResult.get(new Closure<Monitor>() {

			@Override
			public void apply(final Monitor o) {
				monitor = o;
			}
		});
	}

	@Override
	public void stop(final ShutdownCallback callback) {
		if (!started) {
			throw new IllegalStateException(
					"Cannot stop an already stopped component.");
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
