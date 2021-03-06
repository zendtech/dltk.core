/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 
 *******************************************************************************/
package org.eclipse.dltk.internal.debug.core.model;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.dltk.compiler.util.Util;
import org.eclipse.dltk.core.DLTKCore;
import org.eclipse.dltk.core.IDLTKLanguageToolkit;
import org.eclipse.dltk.dbgp.IDbgpSession;
import org.eclipse.dltk.dbgp.breakpoints.IDbgpBreakpoint;
import org.eclipse.dltk.dbgp.commands.IDbgpExtendedCommands;
import org.eclipse.dltk.dbgp.exceptions.DbgpException;
import org.eclipse.dltk.dbgp.internal.IDbgpTerminationListener;
import org.eclipse.dltk.debug.core.DLTKDebugLaunchConstants;
import org.eclipse.dltk.debug.core.DLTKDebugPlugin;
import org.eclipse.dltk.debug.core.ExtendedDebugEventDetails;
import org.eclipse.dltk.debug.core.IHotCodeReplaceListener;
import org.eclipse.dltk.debug.core.ISmartStepEvaluator;
import org.eclipse.dltk.debug.core.eval.IScriptEvaluationEngine;
import org.eclipse.dltk.debug.core.model.IScriptDebugTarget;
import org.eclipse.dltk.debug.core.model.IScriptStackFrame;
import org.eclipse.dltk.debug.core.model.IScriptThread;
import org.eclipse.dltk.internal.debug.core.eval.ScriptEvaluationEngine;
import org.eclipse.dltk.internal.debug.core.model.operations.DbgpDebugger;

public class ScriptThread extends ScriptDebugElement implements IScriptThread,
		IThreadManagement, IDbgpTerminationListener,
		ScriptThreadStateManager.IStateChangeHandler, IHotCodeReplaceListener {

	private ScriptThreadStateManager stateManager;

	private final IScriptThreadManager manager;

	private final ScriptStack stack;

	// Session
	private final IDbgpSession session;

	// State variables
	private final IScriptDebugTarget target;

	private IScriptEvaluationEngine evalEngine;

	private int currentStackLevel;

	private boolean terminated = false;

	private int propertyPageSize = 32;

	// ScriptThreadStateManager.IStateChangeHandler
	public void handleSuspend(int detail) {
		DebugEventHelper.fireExtendedEvent(this,
				ExtendedDebugEventDetails.BEFORE_SUSPEND);

		stack.update(true);

		if (handleSmartStepInto()) {
			return;
		}

		DebugEventHelper.fireChangeEvent(this);
		DebugEventHelper.fireSuspendEvent(this, detail);
	}

	private boolean handleSmartStepInto() {
		if (stateManager.isStepInto()
				&& getScriptDebugTarget().isUseStepFilters()
				&& stack.getFrames().length > currentStackLevel) {
			stateManager.setStepInto(false);
			IScriptDebugTarget target = this.getScriptDebugTarget();
			String[] filters = target.getFilters();
			IDLTKLanguageToolkit toolkit = this.getScriptDebugTarget()
					.getLanguageToolkit();
			if (toolkit != null) {
				ISmartStepEvaluator evaluator = SmartStepEvaluatorManager
						.getEvaluator(toolkit.getNatureId());
				if (evaluator != null) {
					if (evaluator.isFiltered(filters, this)) {
						try {
							this.stepReturn();
							return true;
						} catch (DebugException e) {
							if (DLTKCore.DEBUG) {
								e.printStackTrace();
							}
						}
					}
				}
			}
		}
		return false;
	}

	public void handleResume(int detail) {
		DebugEventHelper.fireExtendedEvent(this,
				ExtendedDebugEventDetails.BEFORE_RESUME);

		DebugEventHelper.fireResumeEvent(this, detail);
		DebugEventHelper.fireChangeEvent(this);
	}

	public void handleTermination(DbgpException e) {
		if (e != null) {
			DLTKDebugPlugin.log(e);
			IScriptStreamProxy proxy = getScriptDebugTarget().getStreamProxy();
			if (proxy != null) {
				proxy.writeStderr("\n" + e.getMessage() + "\n"); //$NON-NLS-1$ //$NON-NLS-2$
				stack.update(false);
				IStackFrame[] frames = stack.getFrames();
				proxy.writeStderr("\nStack trace:\n"); //$NON-NLS-1$
				try {
					for (int i = 0; i < frames.length; i++) {
						IScriptStackFrame frame = (IScriptStackFrame) frames[i];
						String line = "\t#" + frame.getLevel() + " file:" //$NON-NLS-1$ //$NON-NLS-2$
								+ frame.getSourceURI().getPath() + " [" //$NON-NLS-1$
								+ frame.getLineNumber() + "]\n"; //$NON-NLS-1$
						proxy.writeStderr(line);
					}
				} catch (DebugException e2) {
					if (DLTKCore.DEBUG) {
						e.printStackTrace();
					}
				}
			}
		}

		session.requestTermination();
		try {
			session.waitTerminated();
		} catch (InterruptedException ee) {
			ee.printStackTrace();
		}
		manager.terminateThread(this);
	}

	public ScriptThread(IScriptDebugTarget target, IDbgpSession session,
			IScriptThreadManager manager) throws DbgpException, CoreException {

		this.target = target;

		this.manager = manager;

		this.session = session;
		this.session.addTerminationListener(this);

		this.stateManager = new ScriptThreadStateManager(this);

		this.stack = new ScriptStack(this);
	}

	public void initialize(IProgressMonitor monitor) throws DbgpException {
		monitor.beginTask(Util.EMPTY_STRING, 10);
		try {

			final DbgpDebugger engine = this.stateManager.getEngine();

			if (DLTKCore.DEBUG) {
				DbgpDebugger.printEngineInfo(engine);
			}

			engine.setMaxChildren(propertyPageSize);
			engine.setMaxDepth(2);
			engine.setMaxData(8192);
			monitor.worked(2);

			manager.configureThread(engine, this);
			monitor.worked(6);

			final boolean isDebugConsole = DLTKDebugLaunchConstants
					.isDebugConsole(target.getLaunch());

			if (isDebugConsole
					&& engine
							.isFeatureSupported(IDbgpExtendedCommands.STDIN_COMMAND)) {
				engine.redirectStdin();
			}
			engine.setNotifyOk(true);
			if (isDebugConsole) {
				engine.redirectStdout();
				engine.redirectStderr();
			}
			monitor.worked(2);

			HotCodeReplaceManager.getDefault().addHotCodeReplaceListener(this);
		} finally {
			monitor.done();
		}
		// final IDbgpExtendedCommands extended = session.getExtendedCommands();
		// session.getNotificationManager().addNotificationListener(
		// new IDbgpNotificationListener() {
		// private final BufferedReader reader = new BufferedReader(
		// new InputStreamReader(getStreamProxy().getStdin()));
		//
		// public void dbgpNotify(IDbgpNotification notification) {
		// try {
		// extended.sendStdin(reader.readLine() + "\n");
		// } catch (IOException e) {
		// // TODO: log exception
		// e.printStackTrace();
		// } catch (DbgpException e) {
		// // TODO: log exception
		// e.printStackTrace();
		// }
		// }
		// });
	}

	public boolean hasStackFrames() {
		return isSuspended() && !isTerminated() && stack.hasFrames();
	}

	boolean isStackInitialized() {
		return stack.isInitialized();
	}

	// IThread
	public IStackFrame[] getStackFrames() throws DebugException {
		if (!isSuspended()) {
			try {
				Thread.sleep(100);
			} catch (Exception e) {
			}
			if (!isSuspended()) {
				return ScriptStack.NO_STACK_FRAMES;
			}
		}

		return session.getDebugOptions().filterStackLevels(stack.getFrames());
	}

	public int getPriority() throws DebugException {
		return 0;
	}

	public IStackFrame getTopStackFrame() {
		return stack.getTopFrame();
	}

	public String getName() {
		return session.getInfo().getThreadId();
	}

	public IBreakpoint[] getBreakpoints() {
		return DebugPlugin.getDefault().getBreakpointManager().getBreakpoints(
				getModelIdentifier());
	}

	// ISuspendResume

	// Suspend
	public int getModificationsCount() {
		return stateManager.getModificationsCount();
	}

	public boolean isSuspended() {
		return stateManager.isSuspended();
	}

	public boolean canSuspend() {
		return stateManager.canSuspend();
	}

	public void suspend() throws DebugException {
		stateManager.suspend();
	}

	// Resume
	public boolean canResume() {
		return stateManager.canResume();
	}

	public void resume() throws DebugException {
		stateManager.resume();
	}

	public void initialStepInto() {
		stateManager.setSuspended(false, DebugEvent.CLIENT_REQUEST);
		stateManager.getEngine().initialStepInto();
	}

	// IStep
	public boolean isStepping() {
		return stateManager.isStepping();
	}

	boolean isStepInto() {
		return stateManager.isStepInto();
	}

	// Step into
	public boolean canStepInto() {
		return stateManager.canStepInto();
	}

	public void stepInto() throws DebugException {
		currentStackLevel = this.stack.getFrames().length;
		stateManager.stepInto();
	}

	// Step over
	public boolean canStepOver() {
		return stateManager.canStepOver();
	}

	public void stepOver() throws DebugException {
		stateManager.stepOver();
	}

	// Step return
	public boolean canStepReturn() {
		return stateManager.canStepReturn();
	}

	public void stepReturn() throws DebugException {
		stateManager.stepReturn();
	}

	// ITerminate
	public boolean isTerminated() {
		return terminated || stateManager.isTerminated();
	}

	public boolean canTerminate() {
		return !isTerminated();
	}

	public void terminate() throws DebugException {
		target.terminate();
	}

	public void sendTerminationRequest() throws DebugException {
		stateManager.terminate();
	}

	public IDbgpSession getDbgpSession() {
		return session;
	}

	public IDbgpBreakpoint getDbgpBreakpoint(String id) {
		try {
			return session.getCoreCommands().getBreakpoint(id);
		} catch (DbgpException e) {
			if (DLTKCore.DEBUG) {
				e.printStackTrace();
			}
		}

		return null;
	}

	public IScriptStreamProxy getStreamProxy() {
		return target.getStreamProxy();
	}

	public IDebugTarget getDebugTarget() {
		return target.getDebugTarget();
	}

	public IScriptEvaluationEngine getEvaluationEngine() {
		if (evalEngine == null) {
			evalEngine = new ScriptEvaluationEngine(this);
		}

		return evalEngine;
	}

	// IDbgpTerminationListener
	public void objectTerminated(Object object, Exception e) {
		terminated = true;
		Assert.isTrue(object == session);
		HotCodeReplaceManager.getDefault().removeHotCodeReplaceListener(this);
		manager.terminateThread(this);
	}

	// Object
	public String toString() {
		return "Thread (" + session.getInfo().getThreadId() + ")"; //$NON-NLS-1$ //$NON-NLS-2$
	}

	public void notifyModified() {
		stateManager.notifyModified();
	}

	public void hotCodeReplaceFailed(IScriptDebugTarget target,
			DebugException exception) {
		if (isSuspended()) {
			stack.updateFrames();
			DebugEventHelper.fireChangeEvent(this);
		}
	}

	public void hotCodeReplaceSucceeded(IScriptDebugTarget target) {
		if (isSuspended()) {
			stack.updateFrames();
			DebugEventHelper.fireChangeEvent(this);
		}
	}

	public int getPropertyPageSize() {
		return propertyPageSize;
	}

	public boolean retrieveGlobalVariables() {
		return target.retrieveGlobalVariables();
	}

	public boolean retrieveClassVariables() {
		return target.retrieveClassVariables();
	}

	public boolean retrieveLocalVariables() {
		return target.retrieveLocalVariables();
	}

	public void updateStackFrames() {
		stack.updateFrames();
		DebugEventHelper.fireChangeEvent(ScriptThread.this.getDebugTarget());
	}

	void updateStack() {
		stack.update(true);
	}

	boolean isValidStack() {
		return session.getDebugOptions().isValidStack(stack.getFrames());
	}

}
