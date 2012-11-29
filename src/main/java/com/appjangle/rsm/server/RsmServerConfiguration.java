package com.appjangle.rsm.server;

import one.utils.server.ComponentConfiguration;

public interface RsmServerConfiguration extends ComponentConfiguration {

	/**
	 * The URI of the node, at which commands will be posted.
	 * 
	 * @return
	 */
	public String getCommandsNode();

	public String getCommandsNodeSecret();

}
