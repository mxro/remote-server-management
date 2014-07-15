package com.appjangle.rsm.server.internal.components;

import com.appjangle.rsm.server.configurations.DoNothingComponentConfiguration;

import de.mxro.server.ComponentConfiguration;
import de.mxro.server.ComponentContext;
import de.mxro.server.ServerComponent;
import de.mxro.service.callbacks.ShutdownCallback;
import de.mxro.service.callbacks.StartCallback;

public class DoNothingComponent implements ServerComponent {

	DoNothingComponentConfiguration conf;

	@Override
	public void stop(final ShutdownCallback callback) {
		callback.onShutdownComplete();
	}

	@Override
	public void start(final StartCallback callback) {
		callback.onStarted();
	}

	@Override
	public void injectConfiguration(final ComponentConfiguration conf) {
		this.conf = (DoNothingComponentConfiguration) conf;
	}

	@Override
	public void injectContext(final ComponentContext context) {

	}

	@Override
	public ComponentConfiguration getConfiguration() {
		return this.conf;
	}

}
