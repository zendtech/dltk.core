package org.eclipse.dltk.debug.core;

import org.eclipse.dltk.dbgp.commands.IDbgpContextCommands;

/**
 * Provides a kind of standard variables container.
 *
 */
public interface IScriptVariableContainer {
	/**
	 * Container kind.
	 *
	 */
	enum ContainerKind {
		Local(IDbgpContextCommands.LOCAL_CONTEXT_ID), // Local context
		Global(IDbgpContextCommands.GLOBAL_CONTEXT_ID), // Global context
		Class(IDbgpContextCommands.CLASS_CONTEXT_ID); // Class context
		// Values holder
		ContainerKind(int value) {
			this.value = value;
		}

		public int getContainerKind() {
			return value;
		}

		private int value = 0;
	}

	ContainerKind getVariablesContainerKind();
}
