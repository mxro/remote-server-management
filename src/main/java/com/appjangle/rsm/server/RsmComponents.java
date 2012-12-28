package com.appjangle.rsm.server;

import com.appjangle.rsm.server.internal.components.DoNothingComponent;

import de.mxro.server.ServerComponent;

public class RsmComponents {

	public static ServerComponent createDoNothingComponent() {
		return new DoNothingComponent();
	}

}
