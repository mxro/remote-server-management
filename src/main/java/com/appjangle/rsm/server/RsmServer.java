package com.appjangle.rsm.server;

import one.utils.server.ServerComponent;

import com.appjangle.rsm.server.internal.RsmServerComponent;

public class RsmServer {

	public static ServerComponent createServer() {

		return new RsmServerComponent();
	}

}
