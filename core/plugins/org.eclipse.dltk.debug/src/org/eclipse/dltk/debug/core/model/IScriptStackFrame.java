/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 
 *******************************************************************************/
package org.eclipse.dltk.debug.core.model;

import java.net.URI;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IStackFrame;

public interface IScriptStackFrame extends IStackFrame {
	IScriptStack getStack();

	/**
	 * Return associated thread.
	 * 
	 * @return
	 */
	IScriptThread getScriptThread();

	/**
	 * Return current stack level.
	 * 
	 * @return
	 */
	int getLevel();

	String getSourceLine();

	/**
	 * Return line number of the command start or -1 if not available
	 * 
	 * @return
	 */
	int getBeginLine();

	/**
	 * Return column number of the command start or -1 if not available
	 * 
	 * @return
	 */
	int getBeginColumn();

	/**
	 * Return line number of the command end or -1 if not available
	 * 
	 * @return
	 */
	int getEndLine();

	/**
	 * Return column number of the command end or -1 if not available
	 * 
	 * @return
	 */
	int getEndColumn();

	URI getSourceURI();

	IScriptVariable findVariable(String varName) throws DebugException;

	String getWhere();

	/**
	 * Return method name returned from debugger or null if not available.
	 * 
	 * @return name of method or null.
	 */
	String getMethodName();

	/**
	 * Return method offset returned from debugger.
	 * 
	 * @return integer method offset or -1 if not available.
	 */
	int getMethodOffset();
}
