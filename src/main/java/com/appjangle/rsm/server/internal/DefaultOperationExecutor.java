package com.appjangle.rsm.server.internal;

import com.appjangle.rsm.client.commands.ComponentOperation;
import com.appjangle.rsm.client.commands.OperationCallback;
import com.appjangle.rsm.server.OperationExecutor;

import de.mxro.server.ComponentContext;
import de.mxro.server.manager.ComponentManager;

public class DefaultOperationExecutor implements OperationExecutor {

	public final ComponentManager manager;

	@Override
	public void perform(final ComponentOperation operation,
			final ComponentContext context, final OperationCallback callback) {

		operation.perform(manager, context, callback);
	}

	public DefaultOperationExecutor(final ComponentManager manager) {
		super();
		this.manager = manager;
	}

}
