/*******************************************************************************
 * Copyright (c) 2015 Dawid Pakula and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Dawid Pakula - initial API and implementation
 *******************************************************************************/
package org.eclipse.dltk.core.index2.search;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.dltk.core.IModelElement;
import org.eclipse.dltk.core.search.IDLTKSearchScope;

public interface ISearchEngineExtension extends ISearchEngine {

	/**
	 * Search for model elements in index.
	 * 
	 * @param elementType
	 *            Element type ({@link IModelElement#TYPE},
	 *            {@link IModelElement#METHOD},{@link IModelElement#FIELD},etc.)
	 * @param qualifier
	 *            Element qualifier (package name)
	 * @param elementName
	 *            Element name pattern
	 * @param parent
	 *            Element parent name
	 * @param trueFlags
	 *            Logical OR of flags that must exist in element flags bitset.
	 *            Set to <code>0</code> to disable filtering by trueFlags.
	 * @param falseFlags
	 *            Logical OR of flags that must not exist in the element flags
	 *            bitset. Set to <code>0</code> to disable filtering by
	 *            falseFlags.
	 * @param limit
	 *            Limit number of results (<code>0</code> - unlimited)
	 * @param searchFor
	 *            A combination of {@link #SF_REFS}, {@link #SF_DECLS}
	 * @param matchRule
	 *            A combination of {@link #MR_EXACT}, {@link #MR_PREFIX}
	 * @param scope
	 *            Search scope
	 * @param requestor
	 *            Search requestor
	 * @param monitor
	 *            Progress monitor
	 * @see https://bugs.eclipse.org/bugs/show_bug.cgi?id=457784
	 * @since 5.1.1
	 */
	public void search(int elementType, String qualifier, String elementName,
			String parent,
			int trueFlags, int falseFlags, int limit, SearchFor searchFor,
			MatchRule matchRule, IDLTKSearchScope scope,
			ISearchRequestor requestor, IProgressMonitor monitor);
}
