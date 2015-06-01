/*******************************************************************************
 * Copyright (c) 2015 xored software, Inc.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     xored software, Inc. - initial API and Implementation (Andrey Sobolev)
 *******************************************************************************/
package org.eclipse.dltk.debug.core;

public interface IDLTKDebugToolkit2 extends IDLTKDebugToolkit {

	/**
	 * Construct a complex expression in format fieldName|expression.
	 * 
	 * @return
	 */
	boolean isWatchpointComplexSupported();

}
