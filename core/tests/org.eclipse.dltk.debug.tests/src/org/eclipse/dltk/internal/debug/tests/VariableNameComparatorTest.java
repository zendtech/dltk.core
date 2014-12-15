package org.eclipse.dltk.internal.debug.tests;

import java.util.ArrayList;
import java.util.Arrays;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.dltk.internal.debug.core.model.VariableNameComparator;

import junit.framework.TestCase;

public class VariableNameComparatorTest extends TestCase {

	protected ArrayList<IVariable> list;
	protected VariableNameComparator comparator;

	protected void setUp() {
		comparator = new VariableNameComparator();
		if (list == null)
			list = new ArrayList<IVariable>();
		else
			list.clear();
	}

	protected void assertSortedListNamesEqual(String... expectedListNames)
			throws DebugException {
		// when comparator doesn't adhere to the "Comparator" contract this
		// could give an exception
		IVariable[] tmp = new IVariable[list.size()];

		Arrays.sort(list.toArray(tmp), comparator);

		assertEquals(expectedListNames.length, tmp.length);

		boolean ok = true;
		for (int i = 0; i < tmp.length && ok; i++) {
			ok = expectedListNames[i].equals(tmp[i].getName());
		}

		if (!ok)
			failNotEquals("Sorted list has unexpected value;",
					Arrays.asList(expectedListNames), Arrays.asList(tmp));
	}

	public void testStringVariables1() throws DebugException {
		list.add(getNewVariable("b"));
		list.add(getNewVariable("c"));
		list.add(getNewVariable("a"));

		assertSortedListNamesEqual("a", "b", "c");
	}

	public void testIntVariables1() throws DebugException {
		list.add(getNewVariable("3"));
		list.add(getNewVariable("2"));
		list.add(getNewVariable("1"));

		assertSortedListNamesEqual("1", "2", "3");
	}

	public void testIntVariables2() throws DebugException {
		list.add(getNewVariable("115"));
		list.add(getNewVariable("2"));
		list.add(getNewVariable("003"));

		assertSortedListNamesEqual("2", "003", "115");
	}

	public void testMixedVariables1() throws DebugException {
		list.add(getNewVariable("b"));
		list.add(getNewVariable("4"));
		list.add(getNewVariable("2a"));
		list.add(getNewVariable("116"));
		list.add(getNewVariable("2a"));
		list.add(getNewVariable("c"));
		list.add(getNewVariable("5"));
		list.add(getNewVariable("2a"));
		list.add(getNewVariable("6"));
		list.add(getNewVariable("114"));
		list.add(getNewVariable("a"));

		// when the comparator doesn't respect it's contract this could get
		// ordered strangely for ArrayList
		assertSortedListNamesEqual("4", "5", "6", "114", "116", "2a", "2a",
				"2a", "a", "b", "c");
	}

	public void testMixedVariables2() throws DebugException {
		// 33 items, enough to trigger merges in Java's Tim-sort alg. (< 32
		// doesn't do merges)
		// + a > 17 consecutive numbers to trigger merges in such a way as to
		// detect Comparator inconsistency

		list.add(getNewVariable("1"));
		list.add(getNewVariable("2"));
		list.add(getNewVariable("3"));
		list.add(getNewVariable("4"));
		list.add(getNewVariable("5"));
		list.add(getNewVariable("6"));
		list.add(getNewVariable("7"));
		list.add(getNewVariable("8"));
		list.add(getNewVariable("9"));
		list.add(getNewVariable("10"));
		list.add(getNewVariable("11"));

		list.add(getNewVariable("12"));
		list.add(getNewVariable("13"));
		list.add(getNewVariable("14"));
		list.add(getNewVariable("15"));
		list.add(getNewVariable("16"));
		list.add(getNewVariable("17"));
		list.add(getNewVariable("18"));

		list.add(getNewVariable("2a"));
		list.add(getNewVariable("6"));
		list.add(getNewVariable("114"));
		list.add(getNewVariable("a"));

		list.add(getNewVariable("b"));
		list.add(getNewVariable("4"));
		list.add(getNewVariable("2a"));
		list.add(getNewVariable("116"));
		list.add(getNewVariable("2a"));
		list.add(getNewVariable("c"));
		list.add(getNewVariable("5"));
		list.add(getNewVariable("2a"));
		list.add(getNewVariable("6"));
		list.add(getNewVariable("114"));
		list.add(getNewVariable("a"));

		list.add(getNewVariable("b"));
		list.add(getNewVariable("4"));
		list.add(getNewVariable("2a"));
		list.add(getNewVariable("116"));
		list.add(getNewVariable("2a"));
		list.add(getNewVariable("c"));
		list.add(getNewVariable("5"));
		list.add(getNewVariable("2a"));
		list.add(getNewVariable("6"));
		list.add(getNewVariable("114"));
		list.add(getNewVariable("a"));

		list.add(getNewVariable("b"));
		list.add(getNewVariable("4"));
		list.add(getNewVariable("2a"));
		list.add(getNewVariable("116"));
		list.add(getNewVariable("2a"));
		list.add(getNewVariable("c"));
		list.add(getNewVariable("5"));
		list.add(getNewVariable("2a"));
		list.add(getNewVariable("6"));
		list.add(getNewVariable("114"));
		list.add(getNewVariable("a"));

		// this array configuration used to throw an exception directly when
		// sorting
		// so it didn't even reach the compare expected sorted result part...

		// java.lang.IllegalArgumentException: Comparison method violates its
		// general contract!
		// at java.util.TimSort.mergeHi(TimSort.java:868)
		// at java.util.TimSort.mergeAt(TimSort.java:485)
		// at java.util.TimSort.mergeForceCollapse(TimSort.java:426)
		// at java.util.TimSort.sort(TimSort.java:223)
		// at java.util.TimSort.sort(TimSort.java:173)
		// at java.util.Arrays.sort(Arrays.java:659)
		// at
		// org.eclipse.dltk.internal.debug.tests.VariableNameComparatorTest.assertSortedListNamesEqual(VariableNameComparatorTest.java:31)
		// at
		// org.eclipse.dltk.internal.debug.tests.VariableNameComparatorTest.testMixedVariables2(VariableNameComparatorTest.java:158)
		// (...)

		assertSortedListNamesEqual("1", "2", "3", "4", "4", "4", "4", "5", "5",
				"5", "5", "6", "6", "6", "6", "6", "7", "8", "9", "10", "11",
				"12", "13", "14", "15", "16", "17", "18", "114", "114", "114",
				"114", "116", "116", "116", "2a", "2a", "2a", "2a", "2a", "2a",
				"2a", "2a", "2a", "2a", "a", "a", "a", "a", "b", "b", "b", "c",
				"c", "c");
	}

	protected IVariable getNewVariable(final String name) {
		return new IVariable() {

			public boolean verifyValue(IValue value) throws DebugException {
				// dummy
				return false;
			}

			public boolean verifyValue(String expression) throws DebugException {
				// dummy
				return false;
			}

			public boolean supportsValueModification() {
				// dummy
				return false;
			}

			public void setValue(IValue value) throws DebugException {
				// dummy
			}

			public void setValue(String expression) throws DebugException {
				// dummy
			}

			public Object getAdapter(Class adapter) {
				// dummy
				return null;
			}

			public String getModelIdentifier() {
				// dummy
				return null;
			}

			public ILaunch getLaunch() {
				// dummy
				return null;
			}

			public IDebugTarget getDebugTarget() {
				// dummy
				return null;
			}

			public boolean hasValueChanged() throws DebugException {
				// dummy
				return false;
			}

			public IValue getValue() throws DebugException {
				// dummy
				return null;
			}

			public String getReferenceTypeName() throws DebugException {
				// dummy
				return null;
			}

			public String getName() throws DebugException {
				return name;
			}

			@Override
			public String toString() {
				return name;
			}

		};
	}

}
