package com.appjangle.rsm.server;


import com.appjangle.rsm.server.internal.RsmServerComponent;

import de.mxro.server.ServerComponent;

public class RsmServer {

	public static ServerComponent createServer() {

		return new RsmServerComponent();
	}

}
