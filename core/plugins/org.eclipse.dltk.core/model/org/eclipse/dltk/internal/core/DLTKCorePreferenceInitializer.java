/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 
 *******************************************************************************/
package org.eclipse.dltk.internal.core;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.dltk.core.DLTKCore;

/**
 * DLTKCore eclipse preferences initializer. Initially done in
 * DLTKCore.initializeDefaultPreferences which was deprecated with new eclipse
 * preferences mechanism.
 */
public class DLTKCorePreferenceInitializer extends
		AbstractPreferenceInitializer {

	/**
	 * If modified, also modify the method
	 * {@link ModelManager#getDefaultOptionsNoInitialization()}
	 */
	@Override
	public void initializeDefaultPreferences() {
		// Get options names set
		HashSet<String> optionNames = ModelManager.getModelManager().optionNames;

		Map<String, String> defaultOptionsMap = new HashMap<String, String>();

		// DLTKCore settings
		defaultOptionsMap.put(DLTKCore.CORE_INCOMPLETE_BUILDPATH,
				DLTKCore.ERROR);
		defaultOptionsMap.put(DLTKCore.CORE_CIRCULAR_BUILDPATH, DLTKCore.ERROR);
		defaultOptionsMap.put(
				DLTKCore.CORE_ENABLE_BUILDPATH_EXCLUSION_PATTERNS,
				DLTKCore.ENABLED);
		defaultOptionsMap.put(DLTKCore.INDEXER_ENABLED, DLTKCore.ENABLED);
		defaultOptionsMap.put(DLTKCore.BUILDER_ENABLED, DLTKCore.ENABLED);
		defaultOptionsMap.put(DLTKCore.CODEASSIST_CAMEL_CASE_MATCH,
				DLTKCore.ENABLED);

		// encoding setting comes from resource plug-in
		optionNames.add(DLTKCore.CORE_ENCODING);

		// project-specific options
		optionNames.add(DLTKCore.PROJECT_SOURCE_PARSER_ID);

		// Store default values to default preferences
		IEclipsePreferences defaultPreferences = DefaultScope.INSTANCE
				.getNode(DLTKCore.PLUGIN_ID);
		for (Map.Entry<String, String> entry : defaultOptionsMap.entrySet()) {
			String optionName = entry.getKey();
			defaultPreferences.put(optionName, entry.getValue());
			optionNames.add(optionName);
		}
	}
}
