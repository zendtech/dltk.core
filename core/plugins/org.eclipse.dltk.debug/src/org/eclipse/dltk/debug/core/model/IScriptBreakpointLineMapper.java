package org.eclipse.dltk.debug.core.model;

import java.net.URI;

import org.eclipse.dltk.dbgp.IDbgpStackLevel;
import org.eclipse.dltk.dbgp.breakpoints.DbgpBreakpointConfig;

/**
 * Interafce is designed to take care of mapping line numbers in breakpoits from
 * one to another.
 * 
 * @author Andrey Sobolev (haiodo@gmail.com)
 * @since 5.2
 */
public interface IScriptBreakpointLineMapper {

	/**
	 * Perform a mapping from source line numbers to debugger line numbers and
	 * update breakpoint config with proper values.
	 * 
	 * @param uri
	 *            - file identifier
	 * @param line
	 *            - line number
	 * @param bpConfig
	 *            - dbgp breakpoint configuration entry.
	 */
	void toDebuggerBreakpoint(URI uri, int line, DbgpBreakpointConfig bpConfig);

	/**
	 * Perform a mapping from debugger line numbers to source line numbers.
	 * Method is used to determine line in source files.
	 * 
	 * @param uri
	 *            - file identifier
	 * @param line
	 *            - line number
	 * @return mapped stack level, return null then do not need to performa any
	 *         changes.
	 */
	IDbgpStackLevel getSourceStackLevel(IDbgpStackLevel iDbgpStackLevel);

}
