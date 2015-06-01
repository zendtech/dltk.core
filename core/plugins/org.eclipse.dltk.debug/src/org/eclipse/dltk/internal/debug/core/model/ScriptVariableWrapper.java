package org.eclipse.dltk.internal.debug.core.model;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.dltk.debug.core.IScriptVariableContainer;
import org.eclipse.dltk.debug.core.model.AtomicScriptType;
import org.eclipse.dltk.debug.core.model.IScriptStackFrame;
import org.eclipse.dltk.debug.core.model.IScriptType;
import org.eclipse.dltk.debug.core.model.IScriptValue;
import org.eclipse.dltk.debug.core.model.IScriptVariable;

public class ScriptVariableWrapper extends ScriptDebugElement implements
		IScriptVariable, IScriptVariableContainer {

	final IDebugTarget target;
	private final String name;
	private IVariable[] children;

	private IScriptValue value = null;
	private ContainerKind kind;

	public ScriptVariableWrapper(IDebugTarget target, String name,
			IVariable[] children, IScriptVariableContainer.ContainerKind kind) {
		this.target = target;
		this.name = name;
		this.children = children;
		this.kind = kind;
	}

	@Override
	public ContainerKind getVariablesContainerKind() {
		return kind;
	}

	public IVariable[] getChildren() throws DebugException {
		if (children == null) {
			return new IScriptVariable[0];
		}
		return (IScriptVariable[]) children.clone();
	}

	public String getEvalName() {
		return name;
	}

	public String getId() {
		return null;
	}

	public String getValueString() {
		return ""; //$NON-NLS-1$
	}

	public boolean hasChildren() {
		if (children == null) {
			return false;
		}
		return children.length > 0;
	}

	public boolean isConstant() {
		return false;
	}

	public String getName() throws DebugException {
		return name;
	}

	public String getReferenceTypeName() throws DebugException {
		return "getReferenceTypeName"; //$NON-NLS-1$
	}

	public boolean hasValueChanged() throws DebugException {
		return false;
	}

	public void setValue(String expression) throws DebugException {

	}

	public void setValue(IValue value) throws DebugException {

	}

	public boolean supportsValueModification() {
		return false;
	}

	public boolean verifyValue(String expression) throws DebugException {
		return false;
	}

	public boolean verifyValue(IValue value) throws DebugException {
		return false;
	}

	public boolean shouldHasChildren() {
		return false;
	}

	public IScriptType getType() {
		return new AtomicScriptType("getType"); //$NON-NLS-1$
	}

	public IScriptStackFrame getStackFrame() {
		return null;
	}

	public IValue getValue() throws DebugException {
		if (value == null) {
			value = new ScriptVariableWrapperValue(this);
		}
		return value;
	}

	public IDebugTarget getDebugTarget() {
		return target;
	}

	/**
	 * @param classes
	 */
	public void refreshValue(IVariable[] newChildren) {
		this.children = newChildren;
	}
}
