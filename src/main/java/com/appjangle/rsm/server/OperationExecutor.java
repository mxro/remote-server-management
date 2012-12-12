package com.appjangle.rsm.server;

import com.appjangle.rsm.client.commands.ComponentOperation;
import com.appjangle.rsm.client.commands.OperationCallback;

/**
 * <p>
 * This service does the actual work of performing the operations on a server.
 * </p>
 * <p>
 * The executor depends on the particular server, which is to be manager and
 * must be implemented to support the server-specific context.
 * </p>
 * 
 * @author Max Rohde
 * 
 */
public interface OperationExecutor {

	/**
	 * This method is called to perform the specified operation on the server.
	 * 
	 * @param operation
	 *            Which operation is to be performed
	 * @param callback
	 *            Callback to be called if operation is completed.
	 * 
	 */
	public void perform(ComponentOperation operation, OperationCallback callback);

}
