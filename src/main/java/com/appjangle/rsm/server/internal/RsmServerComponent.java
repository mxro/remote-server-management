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
import io.nextweb.fn.IntegerResult;
import io.nextweb.fn.Result;
import io.nextweb.fn.Success;
import io.nextweb.jre.Nextweb;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import one.async.joiner.CallbackLatch;

import com.appjangle.rsm.client.commands.ComponentCommand;
import com.appjangle.rsm.server.RsmServerConfiguration;
import com.appjangle.rsm.server.internal.CommandWorker.CommandProcessedCallback;

import de.mxro.server.ComponentConfiguration;
import de.mxro.server.ComponentContext;
import de.mxro.server.ServerComponent;
import de.mxro.server.StartCallback;

public class RsmServerComponent implements ServerComponent {

	private static boolean ENABLE_LOG = true;

	private volatile boolean started = false;
	private volatile boolean starting = false;

	private final List<NodeList> scheduled = Collections
			.synchronizedList(new LinkedList<NodeList>());
	private final AtomicBoolean processing = new AtomicBoolean(false);

	Session session;
	RsmServerConfiguration conf;
	Monitor monitor;
	Link commands;
	ComponentContext context;

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

				scheduled.add(o);
				if (ENABLE_LOG) {
					System.out.println(this + ": Add to scheduled: "
							+ o.values());
				}

				processScheduled();

			}
		});

	}

	protected void processScheduledGuarded() {
		synchronized (processing) {

			if (processing.get()) {
				if (ENABLE_LOG) {
					System.out
							.println(this
									+ ": Skip processing because process already running.");
				}
				return;
			}

			processing.set(true);

		}

		processScheduled();
	}

	protected void processScheduled() {
		if (scheduled.size() == 0) {
			processing.set(false);
			if (ENABLE_LOG) {
				System.out.println(this + ": All pending operation cleared");
			}
			return;
		}

		final NodeList o = scheduled.get(0);
		scheduled.remove(0);

		final List<Object> values = o.values();
		if (ENABLE_LOG) {

			System.out.println(this + ": Processing: " + values);
		}

		processRequests(o, new RequestsProcessedCallback() {

			@Override
			public void onDone() {

				if (ENABLE_LOG) {
					System.out.println(this + ": Completed processing: "
							+ values);
				}

				processScheduled();
			}
		});

	}

	private void processRequests(final NodeList o,
			final RequestsProcessedCallback requestsProcessedCallback) {

		final CallbackLatch latch = new CallbackLatch(o.nodes().size()) {

			@Override
			public void onFailed(final Throwable t) {
				requestsProcessedCallback.onDone();
			}

			@Override
			public void onCompleted() {
				final IntegerResult clearVersionsRequest = commands
						.clearVersions(10);

				clearVersionsRequest.catchExceptions(new ExceptionListener() {

					@Override
					public void onFailure(final ExceptionResult r) {
						requestsProcessedCallback.onDone();
					}
				});

				clearVersionsRequest.get(new Closure<Integer>() {

					@Override
					public void apply(final Integer o) {
						requestsProcessedCallback.onDone();
					}
				});

			}
		};

		for (final Node child : o.nodes()) {

			final Object value = child.value();

			final String childUri = child.uri();

			if (!(value instanceof ComponentCommand)) {
				latch.registerSuccess();
				continue;
			}

			if (value instanceof ComponentCommand) {
				new CommandWorker(conf, commands, session, context).process(
						child, value, childUri, new CommandProcessedCallback() {

							@Override
							public void onSuccess() {
								latch.registerSuccess();
							}

							@Override
							public void onFailure(final Throwable t) {
								latch.registerFail(t);
							}
						});

				continue;
			}

			throw new IllegalStateException("Child of wrong type");

		}

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
		while (processing.get()) {
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
