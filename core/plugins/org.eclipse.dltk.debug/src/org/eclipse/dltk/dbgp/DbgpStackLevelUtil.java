package org.eclipse.dltk.dbgp;

import org.eclipse.dltk.dbgp.internal.DbgpStackLevel;

public class DbgpStackLevelUtil {
	public static IDbgpStackLevel replaceLineNumberOffset(
			IDbgpStackLevel level, int newLineNumber, int newOffset) {
		return new DbgpStackLevel(level.getFileURI(), level.getWhere(),
				level.getLevel(), newLineNumber, newOffset,
				level.getMethodName(), level.getBeginLine(),
				level.getBeginColumn(), level.getEndLine(),
				level.getEndColumn());
	}
}
