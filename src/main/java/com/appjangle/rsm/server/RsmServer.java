package com.appjangle.rsm.server;

import com.appjangle.rsm.server.configurations.DoNothingComponentConfiguration;
import com.appjangle.rsm.server.internal.DefaultOperationExecutor;
import com.appjangle.rsm.server.internal.RsmServerComponent;
import com.appjangle.rsm.server.internal.components.DoNothingComponent;

import de.mxro.server.ComponentConfiguration;
import de.mxro.server.ComponentFactory;
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

	public static ComponentFactory createDefaultComponentFactory() {
		return new ComponentFactory() {

			@Override
			public ServerComponent createComponent(
					final ComponentConfiguration conf) {

				if (conf instanceof DoNothingComponentConfiguration) {
					return new DoNothingComponent();
				}

				throw new IllegalArgumentException(
						"Cannot create component of type: " + conf.getClass());
			}
		};
	}

}
