package com.appjangle.rsm.server;

import com.appjangle.rsm.server.internal.components.DoNothingComponent;

import de.mxro.server.ServerComponent;

public class RsmComponents {

	/**
	 * Component that can be used for is-alive tests on a rsm server.
	 * 
	 * @return
	 */
	public static ServerComponent createDoNothingComponent() {
		return new DoNothingComponent();
	}

}
