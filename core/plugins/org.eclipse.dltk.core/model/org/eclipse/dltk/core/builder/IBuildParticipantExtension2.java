/*******************************************************************************
 * Copyright (c) 2008 xored software, Inc.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     xored software, Inc. - initial API and Implementation (Alex Panchenko)
 *******************************************************************************/
package org.eclipse.dltk.core.builder;

import org.eclipse.core.runtime.CoreException;

public interface IBuildParticipantExtension2 extends IBuildParticipantExtension {

	/**
	 * @param buildChange
	 * @param buildState
	 * @return
	 */
	void prepare(IBuildChange buildChange, IBuildState buildState)
			throws CoreException;

	void buildExternalModule(IBuildContext context) throws CoreException;

}
