package org.eclipse.dltk.internal.debug.core.model;

import java.net.URI;

import org.eclipse.dltk.dbgp.IDbgpStackLevel;
import org.eclipse.dltk.dbgp.breakpoints.DbgpBreakpointConfig;
import org.eclipse.dltk.debug.core.model.IScriptBreakpointLineMapper;

/**
 * Just a dummy implementation of line number mapper for a breakpoints.
 * 
 * @author Andrey Sobolev (haiodo@gmail.com)
 *
 */
public class NopScriptBreakpointLineMapper implements
		IScriptBreakpointLineMapper {

	@Override
	public void toDebuggerBreakpoint(URI uri, int line,
			DbgpBreakpointConfig bpConfig) {
		// Just do nothing
	}

	@Override
	public IDbgpStackLevel getSourceStackLevel(IDbgpStackLevel level) {
		return null;
	}
}
