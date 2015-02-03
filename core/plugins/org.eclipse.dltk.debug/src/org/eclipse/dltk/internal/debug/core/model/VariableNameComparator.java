package org.eclipse.dltk.internal.debug.core.model;

import java.util.Comparator;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IVariable;

public class VariableNameComparator implements Comparator<IVariable> {

	public int compare(IVariable v1, IVariable v2) {
		int result = 0;
		try {
			String v1Str = (v1 != null) ? v1.getName() : ""; //$NON-NLS-1$
			v1Str = v1Str.replaceAll("\\[", "").replaceAll("\\]", ""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			int v1Int = 0;
			boolean v1IsInt;
			String v2Str = (v2 != null) ? v2.getName() : ""; //$NON-NLS-1$
			v2Str = v2Str.replaceAll("\\[", "").replaceAll("\\]", ""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			int v2Int = 0;
			boolean v2IsInt;

			try {
				v1Int = Integer.parseInt(v1Str);
				v1IsInt = true;
			} catch (NumberFormatException nxcn) {
				v1IsInt = false;
			}

			try {
				v2Int = Integer.parseInt(v2Str);
				v2IsInt = true;
			} catch (NumberFormatException nxcn) {
				v2IsInt = false;
			}

			if (v1IsInt != v2IsInt) {
				return v1IsInt ? -1 : +1;
			} else if (v1IsInt) {
				if (v1Int > v2Int) {
					result = 1;
				} else if (v1Int < v2Int) {
					result = -1;
				} else {
					result = 0;
				}
			} else {
				result = v1Str.compareTo(v2Str);
			}
		} catch (DebugException e) {
		}

		return result;
	}

}
