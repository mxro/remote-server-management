package com.appjangle.rsm.server;

import com.appjangle.rsm.server.internal.DefaultOperationExecutor;
import com.appjangle.rsm.server.internal.RsmServerComponent;
import com.appjangle.rsm.server.internal.components.DoNothingComponentFactory;

import de.mxro.factories.Factory;
import de.mxro.server.ServerComponent;
import de.mxro.server.manager.ComponentManager;

public class RsmServer {

	public static OperationExecutor createExecutor(
			final ComponentManager manager) {
		return new DefaultOperationExecutor(manager);
	}

	public static ServerComponent createServer() {

		return new RsmServerComponent();
	}

	public static Factory<?,?,?> createDefaultComponentFactory() {
		return new DoNothingComponentFactory();
	}

}
