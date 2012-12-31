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

	private static boolean ENABLE_LOG = true;

	private volatile boolean started = false;
	private volatile boolean starting = false;

	Session session;
	RsmServerConfiguration conf;
	Monitor monitor;
	Link commands;
	ComponentContext context;
	CommandListWorker worker;

	@Override
	public void start(final StartCallback callback) {
		if (started || starting) {
			throw new IllegalStateException(
					"Cannot start an already started component.");
		}
		starting = true;

		session = Nextweb.createSession();

		commands = session.node(conf.getCommandsNode(),
				conf.getCommandsNodeSecret());

		if (ENABLE_LOG) {
			System.out.println(this + ": Start monitoring: "
					+ conf.getCommandsNode());
		}

		final Result<Monitor> monitorResult = commands.monitor(Interval.FAST,
				new Closure<MonitorContext>() {

					@Override
					public void apply(final MonitorContext ctx) {

						if (ENABLE_LOG) {
							System.out
									.println(this
											+ ": Change detected, processing requests.");
						}

						processRequests(ctx.node());
					}

				});

		monitorResult.catchExceptions(new ExceptionListener() {

			@Override
			public void onFailure(final ExceptionResult r) {
				callback.onFailure(r.exception());
			}
		});

		if (ENABLE_LOG) {
			System.out.println(this + ": Starting monitor.");
		}

		monitorResult.get(new Closure<Monitor>() {

			@Override
			public void apply(final Monitor o) {

				if (ENABLE_LOG) {
					System.out.println(this + ": Monitor started.");
				}

				monitor = o;
				starting = false;
				started = true;

				commands.get(new Closure<Node>() {

					@Override
					public void apply(final Node o) {
						if (ENABLE_LOG) {
							System.out.println(this
									+ ": Processing initial requests.");
						}
						// processing requests available on startup
						processRequests(o);
					}
				});

				callback.onStarted();

			}
		});

	}

	public static interface RequestsProcessedCallback {

		public void onDone();

	}

	private void processRequests(final Node node) {

		node.selectAll().get(new Closure<NodeList>() {

			@Override
			public void apply(final NodeList o) {

				worker.addListToBeProcessed(o);
				if (ENABLE_LOG) {
					System.out.println(this + ": Add to scheduled: "
							+ o.values());
				}

				worker.startProcessingIfRequired();

			}
		});

	}

	@Override
	public void stop(final de.mxro.server.ShutdownCallback callback) {
		assertServerNotStopped();

		waitForStartupToBeCompleted();

		waitForProcessingToStop();

		stopSessionAndMonitor(callback);

	}

	private void stopSessionAndMonitor(
			final de.mxro.server.ShutdownCallback callback) {
		final Result<Success> stopRequest = monitor.stop();

		stopRequest.catchExceptions(new ExceptionListener() {

			@Override
			public void onFailure(final ExceptionResult r) {
				callback.onFailure(r.exception());
			}
		});

		stopRequest.get(new Closure<Success>() {

			@Override
			public void apply(final Success o) {
				stopSession(callback);
			}

		});
	}

	private void stopSession(final de.mxro.server.ShutdownCallback callback) {
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

	private void assertServerNotStopped() {
		if (!(started || starting)) {
			throw new IllegalStateException(
					"Cannot stop an already stopped component.");
		}
	}

	private void waitForStartupToBeCompleted() {
		if (starting) {
			while (!started) {
				if (ENABLE_LOG) {
					System.out
							.println(this
									+ ": Cannot stop because component is still starting");
				}
				try {
					Thread.sleep(100);
				} catch (final InterruptedException e) {
					throw new RuntimeException(e);
				}
				Thread.yield();
			}
		}
	}

	private void waitForProcessingToStop() {
		while (worker.isWorking()) {
			if (ENABLE_LOG) {
				System.out.println(this
						+ ": Cannot stop because server is processing/");
			}
			try {
				Thread.sleep(100);
			} catch (final InterruptedException e) {
				throw new RuntimeException(e);
			}
			Thread.yield();
		}
	}

	@Override
	public void injectConfiguration(final ComponentConfiguration conf) {
		this.conf = (RsmServerConfiguration) conf;
	}

	@Override
	public void injectContext(final ComponentContext context) {
		this.context = context;
	}

	@Override
	public ComponentConfiguration getConfiguration() {
		return conf;
	}

}
