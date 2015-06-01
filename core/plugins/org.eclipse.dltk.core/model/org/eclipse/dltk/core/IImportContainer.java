/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.dltk.core;

/**
 * Represents an import container is a child of a Java compilation unit that
 * contains all (and only) the import declarations. If a compilation unit has no
 * import declarations, no import container will be present.
 * 
 * @noimplement This interface is not intended to be implemented by clients.
 * @since 2.0
 */
public interface IImportContainer extends IModelElement, IParent,
		ISourceReference {
	/**
	 * Returns the first import declaration in this import container with the
	 * given name. This is a handle-only method. The import declaration may or
	 * may not exist.
	 * 
	 * @param name
	 *            the given name
	 * 
	 * @return the first import declaration in this import container with the
	 *         given name
	 */
	IImportDeclaration getImport(String name, String version);

	/**
	 * @see {@link #getImport(String, String)}
	 * @param alias
	 *            the given alias
	 * @param {@link IModelElement}
	 * @param user
	 *            mmodifiers
	 * @since 5.2
	 */
	IImportDeclaration getImport(String name, String version, String alias,
			int type, int flags);

	/**
	 * @since 3.0
	 */
	IImportDeclaration[] getImports() throws ModelException;

	String getContainerName();
}
