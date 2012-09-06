/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 
 *******************************************************************************/
package org.eclipse.dltk.internal.debug.core.model.operations;

import org.eclipse.dltk.dbgp.exceptions.DbgpException;
import org.eclipse.dltk.debug.core.model.IScriptThread;

public class DbgpTerminateOperation extends DbgpOperation {
	private static final String JOB_NAME = Messages.DbgpTerminateOperation_terminateOperation;

	public DbgpTerminateOperation(IScriptThread thread, IResultHandler finish) {
		super(thread, JOB_NAME, finish);
	}

	/**
	 * @see org.eclipse.dltk.internal.debug.core.model.operations.DbgpOperation#process()
	 */
	protected void process() throws DbgpException {
		// And terminate operation is also executed when closing eclipse. Then
		// it is possible that the job isn't even finished or executed.. Or the
		// engine is already shutdown in the main thread.
	}

	/**
	 * @see org.eclipse.dltk.internal.debug.core.model.operations.DbgpOperation#schedule()
	 */
	public void schedule() {
		try {
			callFinish(getCore().stop());
		} catch (DbgpException ex) {
			ex.printStackTrace();
		}
	}
}
