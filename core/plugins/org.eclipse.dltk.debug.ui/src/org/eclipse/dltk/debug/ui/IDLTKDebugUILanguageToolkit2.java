package org.eclipse.dltk.debug.ui;

import org.eclipse.dltk.debug.ui.breakpoints.IScriptBreakpointPropertyPageExtension;

/**
 * Extension interface for Debug UI language toolkits.
 * 
 * @author Andrey Sobolev
 * 
 * @since 5.2
 *
 */
public interface IDLTKDebugUILanguageToolkit2 {
	/**
	 * Return a breakpoint property page configuration extension.
	 * 
	 * @return
	 */
	IScriptBreakpointPropertyPageExtension getBreakpointPropertyPageExtension();
}
