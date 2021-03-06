package org.eclipse.dltk.debug.ui.preferences;

import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.dltk.core.DLTKLanguageManager;
import org.eclipse.dltk.core.IDLTKLanguageToolkit;
import org.eclipse.dltk.core.IScriptProject;
import org.eclipse.dltk.debug.ui.IDLTKDebugUIPreferenceConstants;
import org.eclipse.dltk.internal.debug.ui.ScriptDebugOptionsManager;
import org.eclipse.dltk.ui.DLTKUILanguageManager;
import org.eclipse.dltk.ui.IDLTKUILanguageToolkit;
import org.eclipse.jface.preference.IPreferenceStore;

public class StepFilterManager {
	public static String[] getActiveFilters(String nature) {
		IDLTKUILanguageToolkit languageToolkit = DLTKUILanguageManager
				.getLanguageToolkit(nature);
		return toStrings(getActiveFilters(languageToolkit.getPreferenceStore()));
	}

	public static String[] getActiveFilters(IScriptProject project) {
		try {
			IDLTKLanguageToolkit coreToolkit = DLTKLanguageManager
					.getLanguageToolkit(project);
			IDLTKUILanguageToolkit languageToolkit = DLTKUILanguageManager
					.getLanguageToolkit(coreToolkit.getNatureId());
			return toStrings(getActiveFilters(languageToolkit
					.getPreferenceStore()));
		} catch (Exception e) {
			return new String[0];
		}
	}

	private static String[] toStrings(Filter[] activeFilters) {
		String[] filters = new String[activeFilters.length];
		for (int i = 0; i < filters.length; i++) {
			filters[i] = activeFilters[i].getName();
		}
		return filters;
	}

	public static Filter[] getActiveFilters(IPreferenceStore store) {
		Filter[] filters = null;
		String[] activefilters;
		activefilters = ScriptDebugOptionsManager
				.parseList(store
						.getString(IDLTKDebugUIPreferenceConstants.PREF_ACTIVE_FILTERS_LIST));
		filters = new Filter[activefilters.length];
		for (int i = 0; i < activefilters.length; i++) {
			String[] split = activefilters[i].split(":"); //$NON-NLS-1$
			if (split.length == 1) {
				filters[i] = new Filter(split[0], true, 0);
			} else {
				filters[i] = new Filter(split[0], true, (new Integer(split[1]))
						.intValue());
			}
		}

		return filters;
	}

	/**
	 * @param project
	 * @return
	 * @deprecated
	 */
	public static boolean isUseStepFilters(IScriptProject project) {
		return DebugPlugin.isUseStepFilters();
	}

	/**
	 * @param preferenceStore
	 * @return
	 * @deprecated
	 */
	public static boolean isUseStepFilters(IPreferenceStore preferenceStore) {
		return DebugPlugin.isUseStepFilters();
	}

	/**
	 * @param selection
	 * @param preferenceStore
	 * @deprecated
	 */
	public static void setUseStepFilters(boolean selection,
			IPreferenceStore preferenceStore) {
		DebugPlugin.setUseStepFilters(selection);
	}
}
