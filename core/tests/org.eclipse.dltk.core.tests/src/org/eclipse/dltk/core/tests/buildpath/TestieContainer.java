/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 
 *******************************************************************************/
package org.eclipse.dltk.core.tests.buildpath;

import org.eclipse.core.runtime.IPath;
import org.eclipse.dltk.core.DLTKCore;
import org.eclipse.dltk.core.IBuildpathContainer;
import org.eclipse.dltk.core.IBuildpathEntry;

public class TestieContainer implements IBuildpathContainer {
	private IPath fPath;

	public TestieContainer(IPath srcPath) {
		this.fPath = srcPath;
	}

	@Override
	public IBuildpathEntry[] getBuildpathEntries() {
		return new IBuildpathEntry[] { DLTKCore.newExtLibraryEntry(this.fPath) };
	}

	@Override
	public String getDescription() {
		return "Testie Buildpath Container";
	}

	@Override
	public int getKind() {
		return IBuildpathContainer.K_DEFAULT_SYSTEM;
	}

	@Override
	public IPath getPath() {
		return fPath;
	}

}
