package org.eclipse.dltk.debug.ui.breakpoints;

import org.eclipse.dltk.debug.core.model.IScriptBreakpoint;
import org.eclipse.dltk.debug.ui.IDLTKDebugUILanguageToolkit;
import org.eclipse.swt.widgets.Composite;

/**
 * Interface to customize breakpoint property dialogs.
 * 
 * Should be mixed to {@link IDLTKDebugUILanguageToolkit} interface at same
 * time. Same extension point should be used.
 * 
 * @author Andrey Sobolev
 *
 */
public interface IScriptBreakpointPropertyPageExtension {

	/**
	 * Return true in case only greater or equals hit count value it supported.
	 * Used to show simplefied interface in breakpoint property pages.
	 * 
	 * @param breakpoint
	 */
	boolean hasOnlyGreaterOrEqualsHitCount(IScriptBreakpoint breakpoint);

	/**
	 * Return false in case expression editor should not be available in
	 * breakpoint property pages.
	 * 
	 * @param breakpoint
	 */
	boolean hasExpressionEditor(IScriptBreakpoint breakpoint);

	/**
	 * Return true in case hitcount editor should be available.
	 * 
	 * @param breakpoint
	 */
	boolean hasHitCountEditor(IScriptBreakpoint breakpoint);

	/**
	 * Creates a extra editor for particular type of breakpoint code.
	 */
	IScriptBreakpointPropertyPageExtensionEditor createExtraPropertyEditor(
			IScriptBreakpoint breakpoint);

	public interface IScriptBreakpointPropertyPageExtensionEditor {
		/**
		 * Create some additional contents in breakpoint property page.
		 */
		void createContents(Composite parent);

		/**
		 * Load value of breakpoint contents to widget data.
		 */
		void loadContents(IScriptBreakpoint breakpoint);

		/**
		 * Save values of widget data to breakpoint.
		 * 
		 * @param breakpoint
		 */
		void saveContents(IScriptBreakpoint breakpoint);
	}

}
