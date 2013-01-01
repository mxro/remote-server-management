package com.appjangle.rsm.server.internal;

import io.nextweb.Link;
import io.nextweb.Node;
import io.nextweb.Query;
import io.nextweb.Session;
import io.nextweb.fn.Closure;
import io.nextweb.fn.ExceptionListener;
import io.nextweb.fn.ExceptionResult;
import io.nextweb.fn.Result;
import io.nextweb.fn.Success;
import io.nextweb.operations.exceptions.ImpossibleListener;
import io.nextweb.operations.exceptions.ImpossibleResult;
import io.nextweb.operations.exceptions.UndefinedListener;
import io.nextweb.operations.exceptions.UndefinedResult;

import com.appjangle.rsm.client.commands.ComponentCommand;
import com.appjangle.rsm.client.commands.OperationCallback;
import com.appjangle.rsm.client.commands.v01.FailureResponse;
import com.appjangle.rsm.client.commands.v01.SuccessResponse;
import com.appjangle.rsm.server.RsmServerConfiguration;

import de.mxro.server.ComponentContext;

public class CommandWorker {

	private static boolean ENABLE_LOG = false;

	private final RsmServerConfiguration conf;
	private final Link commands;
	private final Session session;
	private final ComponentContext context;

	public CommandWorker(final RsmServerConfiguration conf,
			final Link commands, final Session session,
			final ComponentContext context) {
		super();
		this.conf = conf;
		this.commands = commands;
		this.session = session;
		this.context = context;
	}

	public void process(final Node child, final Object value,
			final String childUri, final CommandProcessedCallback callback) {
		prepareCommand(child, value, childUri, callback);
	}

	public static interface CommandProcessedCallback {
		public void onSuccess();

		public void onFailure(Throwable t);
	}

	private void prepareCommand(final Node child, final Object value,
			final String childUri, final CommandProcessedCallback callback) {
		final ComponentCommand command = (ComponentCommand) value;

		final Result<Success> removeRequest = commands.removeSafe(child);

		removeRequest.catchImpossible(new ImpossibleListener() {

			@Override
			public void onImpossible(final ImpossibleResult ir) {
				callback.onSuccess();
				// some other process might have processed this item
			}
		});

		if (ENABLE_LOG) {
			System.out.println(this + ": Attempting to remove request for "
					+ child);
		}

		removeRequest.get(new Closure<Success>() {

			@Override
			public void apply(final Success o) {
				if (ENABLE_LOG) {
					System.out.println(this
							+ ": Remove request successfully completed for "
							+ childUri);
				}
				final Link responseNode = session.node(command
						.getResponsePort().getUri(), command.getResponsePort()
						.getSecret());

				responseNode.catchUndefined(new UndefinedListener() {

					@Override
					public void onUndefined(final UndefinedResult r) {

						System.out
								.println(this
										+ ": Unexpected error. Response node has not been defined correctly.");
						callback.onSuccess();
						// throw new RuntimeException(
						// "Response node has not been defined correctly.");
					}
				});

				responseNode.get(new Closure<Node>() {

					@Override
					public void apply(final Node o) {

						if (ENABLE_LOG) {
							System.out
									.println(this
											+ ": Remove command and loaded response node for: "
											+ childUri);
						}

						processCommand(command, o,
								new CommandProcessedCallback() {

									@Override
									public void onSuccess() {
										if (ENABLE_LOG) {
											System.out
													.println(this
															+ ": Command processed for "
															+ childUri);
										}
										callback.onSuccess();
									}

									@Override
									public void onFailure(final Throwable t) {
										callback.onFailure(t);
									}
								});

					}
				});

			}
		});
	}

	/**
	 * Performing command for server and posting response to response node
	 * specified by client.
	 * 
	 * @param command
	 * @param requestsProcessedCallback
	 */
	private void processCommand(final ComponentCommand command,
			final Node responseNode, final CommandProcessedCallback callback) {

		conf.getExecutor(context).perform(command.getOperation(), context,
				new OperationCallback() {

					@Override
					public void onSuccess() {
						final SuccessResponse successResponse = new SuccessResponse();

						final Query appendRequest = responseNode
								.appendSafe(successResponse);

						appendRequest.catchExceptions(new ExceptionListener() {

							@Override
							public void onFailure(final ExceptionResult r) {
								callback.onFailure(r.exception());
							}
						});

						appendRequest.get(new Closure<Node>() {

							@Override
							public void apply(final Node o) {
								callback.onSuccess();
							}
						});

					}

					@Override
					public void onFailure(final Throwable t) {
						final FailureResponse failureResponse = new FailureResponse();
						failureResponse.setException(t);

						final Query appendRequest = responseNode
								.appendSafe(failureResponse);

						appendRequest.catchExceptions(new ExceptionListener() {

							@Override
							public void onFailure(final ExceptionResult r) {
								callback.onFailure(r.exception());
							}
						});

						appendRequest.get(new Closure<Node>() {

							@Override
							public void apply(final Node o) {
								callback.onSuccess();
							}
						});

					}
				});

	}

}
