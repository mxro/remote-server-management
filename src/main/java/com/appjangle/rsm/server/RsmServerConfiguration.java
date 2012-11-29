package com.appjangle.rsm.server;

import de.mxro.server.ComponentConfiguration;

public interface RsmServerConfiguration extends ComponentConfiguration {

	/**
	 * The executor doing the work against the specific server implementation.
	 * 
	 * @return
	 */
	public OperationExecutor getExecutor();

	/**
	 * The URI of the node, at which commands will be posted.
	 * 
	 * @return
	 */
	public String getCommandsNode();

	/**
	 * The secret for the node, at which commands will be posted.
	 * 
	 * @return
	 */
	public String getCommandsNodeSecret();

}
