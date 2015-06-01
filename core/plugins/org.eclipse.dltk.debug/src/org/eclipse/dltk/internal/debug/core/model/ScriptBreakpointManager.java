/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.dltk.internal.debug.core.model;

import java.net.URI;
import java.util.IdentityHashMap;
import java.util.Map;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IBreakpointListener;
import org.eclipse.debug.core.IBreakpointManager;
import org.eclipse.debug.core.IBreakpointManagerListener;
import org.eclipse.debug.core.IDebugEventSetListener;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.dltk.compiler.util.Util;
import org.eclipse.dltk.core.DLTKCore;
import org.eclipse.dltk.dbgp.IDbgpSession;
import org.eclipse.dltk.dbgp.IDbgpSpawnpoint;
import org.eclipse.dltk.dbgp.breakpoints.DbgpBreakpointConfig;
import org.eclipse.dltk.dbgp.commands.IDbgpBreakpointCommands;
import org.eclipse.dltk.dbgp.commands.IDbgpCoreCommands;
import org.eclipse.dltk.dbgp.commands.IDbgpSpawnpointCommands;
import org.eclipse.dltk.dbgp.exceptions.DbgpException;
import org.eclipse.dltk.debug.core.DLTKDebugPlugin;
import org.eclipse.dltk.debug.core.DebugOption;
import org.eclipse.dltk.debug.core.IDLTKDebugToolkit;
import org.eclipse.dltk.debug.core.IDLTKDebugToolkit2;
import org.eclipse.dltk.debug.core.ScriptDebugManager;
import org.eclipse.dltk.debug.core.model.IScriptBreakpoint;
import org.eclipse.dltk.debug.core.model.IScriptBreakpointLineMapper;
import org.eclipse.dltk.debug.core.model.IScriptBreakpointPathMapper;
import org.eclipse.dltk.debug.core.model.IScriptDebugTarget;
import org.eclipse.dltk.debug.core.model.IScriptExceptionBreakpoint;
import org.eclipse.dltk.debug.core.model.IScriptLineBreakpoint;
import org.eclipse.dltk.debug.core.model.IScriptMethodEntryBreakpoint;
import org.eclipse.dltk.debug.core.model.IScriptSpawnpoint;
import org.eclipse.dltk.debug.core.model.IScriptWatchpoint;
import org.eclipse.osgi.util.NLS;

public class ScriptBreakpointManager implements IBreakpointListener,
		IBreakpointManagerListener {

	final IScriptBreakpointPathMapper bpPathMapper;
	final IScriptBreakpointLineMapper bpLineMapper;

	private static final IDbgpSession[] NO_SESSIONS = new IDbgpSession[0];

	private IDbgpSession[] sessions;

	// Utility methods
	protected static IBreakpointManager getBreakpointManager() {
		return DebugPlugin.getDefault().getBreakpointManager();
	}

	protected static DbgpBreakpointConfig createBreakpointConfig(
			IScriptBreakpoint breakpoint) throws CoreException {
		// Enabled
		boolean enabled = breakpoint.isEnabled()
				&& getBreakpointManager().isEnabled();

		DbgpBreakpointConfig config = new DbgpBreakpointConfig(enabled);

		// Hit value
		config.setHitValue(breakpoint.getHitValue());

		// Hit condition
		config.setHitCondition(breakpoint.getHitCondition());

		// Expression
		if (breakpoint.getExpressionState()) {
			config.setExpression(breakpoint.getExpression());
		}

		if (breakpoint instanceof IScriptLineBreakpoint
				&& !(breakpoint instanceof IScriptMethodEntryBreakpoint)) {
			IScriptLineBreakpoint lineBreakpoint = (IScriptLineBreakpoint) breakpoint;
			config.setLineNo(lineBreakpoint.getLineNumber());
		}
		return config;
	}

	protected static String makeWatchpointExpression(
			IScriptWatchpoint watchpoint) throws CoreException {
		final IDLTKDebugToolkit debugToolkit = ScriptDebugManager.getInstance()
				.getDebugToolkitByDebugModel(watchpoint.getModelIdentifier());
		if (debugToolkit instanceof IDLTKDebugToolkit2) {
			if (((IDLTKDebugToolkit2) debugToolkit)
					.isWatchpointComplexSupported()) {
				return watchpoint.getFieldName() + "|"
						+ (watchpoint.getExpressionState()
								? watchpoint.getExpression() : "");
			}
		}
		if (debugToolkit.isAccessWatchpointSupported()) {
			return watchpoint.getFieldName()
					+ (watchpoint.isAccess() ? '1' : '0')
					+ (watchpoint.isModification() ? '1' : '0');
		} else {
			return watchpoint.getFieldName();
		}
	}

	// Adding, removing, updating
	protected void addBreakpoint(final IDbgpSession session,
			IScriptBreakpoint breakpoint) throws CoreException, DbgpException {
		final IDbgpCoreCommands commands = session.getCoreCommands();
		DbgpBreakpointConfig config = createBreakpointConfig(breakpoint);

		String id = null;
		URI bpUri = null;

		// map the outgoing uri if we're a line breakpoint
		if (breakpoint instanceof IScriptLineBreakpoint) {
			IScriptLineBreakpoint bp = (IScriptLineBreakpoint) breakpoint;
			bpUri = bpPathMapper.map(bp.getResourceURI());
		}

		// Type specific
		if (breakpoint instanceof IScriptWatchpoint) {
			IScriptWatchpoint watchpoint = (IScriptWatchpoint) breakpoint;
			config.setExpression(makeWatchpointExpression(watchpoint));
			config.setLineNo(watchpoint.getLineNumber());
			if (bpLineMapper != null) {
				bpLineMapper.toDebuggerBreakpoint(bpUri,
						config.getLineNo(), config);
			}
			id = commands.setWatchBreakpoint(bpUri, config.getLineNo(),
					config);
		} else if (breakpoint instanceof IScriptMethodEntryBreakpoint) {
			IScriptMethodEntryBreakpoint entryBreakpoint = (IScriptMethodEntryBreakpoint) breakpoint;

			if (entryBreakpoint.breakOnExit()) {
				final String exitId = commands.setReturnBreakpoint(bpUri,
						entryBreakpoint.getMethodName(), config);

				entryBreakpoint.setExitBreakpointId(exitId);
			}

			if (entryBreakpoint.breakOnEntry()) {
				final String entryId = commands.setCallBreakpoint(bpUri,
						entryBreakpoint.getMethodName(), config);

				entryBreakpoint.setEntryBreakpointId(entryId);
			}
		} else if (breakpoint instanceof IScriptLineBreakpoint) {
			IScriptLineBreakpoint lineBreakpoint = (IScriptLineBreakpoint) breakpoint;

			config.setLineNo(lineBreakpoint.getLineNumber());

			if (bpLineMapper != null) {
				bpLineMapper.toDebuggerBreakpoint(bpUri,
						config.getLineNo(), config);
			}

			if (ScriptBreakpointUtils.isConditional(lineBreakpoint)) {
				id = commands.setConditionalBreakpoint(bpUri,
						config.getLineNo(), config);
			} else {
				id = commands.setLineBreakpoint(bpUri, config.getLineNo(),
						config);
			}
		} else if (breakpoint instanceof IScriptExceptionBreakpoint) {
			IScriptExceptionBreakpoint lineBreakpoint = (IScriptExceptionBreakpoint) breakpoint;
			id = commands.setExceptionBreakpoint(lineBreakpoint.getTypeName(),
					config);
		}

		// Identifier
		breakpoint.setId(session, id);
	}

	private void addSpawnpoint(final IDbgpSession session,
			IScriptSpawnpoint spawnpoint) throws DbgpException, CoreException {
		final IDbgpSpawnpointCommands commands = (IDbgpSpawnpointCommands) session
				.get(IDbgpSpawnpointCommands.class);
		final IDbgpSpawnpoint p = commands.setSpawnpoint(
				bpPathMapper.map(spawnpoint.getResourceURI()),
				spawnpoint.getLineNumber(), spawnpoint.isEnabled());
		if (p != null) {
			spawnpoint.setId(session, p.getId());
		}
	}

	protected void changeBreakpoint(final IDbgpSession session,
			IScriptBreakpoint breakpoint) throws DbgpException, CoreException {
		final IDbgpBreakpointCommands commands = session.getCoreCommands();
		URI bpUri = null;

		// map the outgoing uri if we're a line breakpoint
		if (breakpoint instanceof IScriptLineBreakpoint) {
			IScriptLineBreakpoint bp = (IScriptLineBreakpoint) breakpoint;
			bpUri = bpPathMapper.map(bp.getResourceURI());
		}

		if (breakpoint instanceof IScriptMethodEntryBreakpoint) {
			DbgpBreakpointConfig config = createBreakpointConfig(breakpoint);
			IScriptMethodEntryBreakpoint entryBreakpoint = (IScriptMethodEntryBreakpoint) breakpoint;

			String entryId = entryBreakpoint.getEntryBreakpointId();
			if (entryBreakpoint.breakOnEntry()) {
				if (entryId == null) {
					// Create entry breakpoint
					entryId = commands.setCallBreakpoint(bpUri,
							entryBreakpoint.getMethodName(), config);
					entryBreakpoint.setEntryBreakpointId(entryId);
				} else {
					// Update entry breakpoint
					commands.updateBreakpoint(entryId, config);
				}
			} else {
				if (entryId != null) {
					// Remove existing entry breakpoint
					commands.removeBreakpoint(entryId);
					entryBreakpoint.setEntryBreakpointId(null);
				}
			}

			String exitId = entryBreakpoint.getExitBreakpointId();
			if (entryBreakpoint.breakOnExit()) {
				if (exitId == null) {
					// Create exit breakpoint
					exitId = commands.setReturnBreakpoint(bpUri,
							entryBreakpoint.getMethodName(), config);
					entryBreakpoint.setExitBreakpointId(exitId);
				} else {
					// Update exit breakpoint
					commands.updateBreakpoint(exitId, config);
				}
			} else {
				if (exitId != null) {
					// Remove exit breakpoint
					commands.removeBreakpoint(exitId);
					entryBreakpoint.setExitBreakpointId(null);
				}
			}
		} else {
			// All other breakpoints
			final String id = breakpoint.getId(session);
			if (id != null) {
				final DbgpBreakpointConfig config = createBreakpointConfig(
						breakpoint);
				if (breakpoint instanceof IScriptWatchpoint) {
					config.setExpression(makeWatchpointExpression(
							(IScriptWatchpoint) breakpoint));
				}
				commands.updateBreakpoint(id, config);
			}
		}
	}

	protected static void removeBreakpoint(IDbgpSession session,
			IScriptBreakpoint breakpoint) throws DbgpException, CoreException {
		final IDbgpBreakpointCommands commands = session.getCoreCommands();
		final String id = breakpoint.removeId(session);
		if (id != null) {
			commands.removeBreakpoint(id);
		}

		if (breakpoint instanceof IScriptMethodEntryBreakpoint) {
			IScriptMethodEntryBreakpoint entryBreakpoint = (IScriptMethodEntryBreakpoint) breakpoint;

			final String entryId = entryBreakpoint.getEntryBreakpointId();
			if (entryId != null) {
				commands.removeBreakpoint(entryId);
			}

			final String exitId = entryBreakpoint.getExitBreakpointId();
			if (exitId != null) {
				commands.removeBreakpoint(exitId);
			}
		}
	}

	private static final int NO_CHANGES = 0;
	private static final int MINOR_CHANGE = 1;
	private static final int MAJOR_CHANGE = 2;

	private int hasBreakpointChanges(IMarkerDelta delta,
			IScriptBreakpoint breakpoint) {
		final String[] attrs = breakpoint.getUpdatableAttributes();
		try {
			final IMarker marker = delta.getMarker();
			for (int i = 0; i < attrs.length; ++i) {
				final String attr = attrs[i];

				final Object oldValue = delta.getAttribute(attr);
				final Object newValue = marker.getAttribute(attr);

				if (oldValue == null) {
					if (newValue != null) {
						return classifyBreakpointChange(delta, breakpoint,
								attr);
					}
					continue;
				}
				if (newValue == null) {
					return classifyBreakpointChange(delta, breakpoint, attr);
				}
				if (!oldValue.equals(newValue)) {
					return classifyBreakpointChange(delta, breakpoint, attr);
				}
			}
		} catch (CoreException e) {
			DLTKDebugPlugin.log(e);
		}
		return NO_CHANGES;
	}

	private static int hasSpawnpointChanges(IMarkerDelta delta,
			IScriptBreakpoint breakpoint) {
		final String[] attrs = breakpoint.getUpdatableAttributes();
		try {
			final IMarker marker = delta.getMarker();
			for (int i = 0; i < attrs.length; ++i) {
				final String attr = attrs[i];
				if (IBreakpoint.ENABLED.equals(attr)
						|| IMarker.LINE_NUMBER.equals(attr)) {
					final Object oldValue = delta.getAttribute(attr);
					final Object newValue = marker.getAttribute(attr);
					if (oldValue == null) {
						if (newValue != null) {
							return IMarker.LINE_NUMBER.equals(attr)
									? MAJOR_CHANGE
									: MINOR_CHANGE;
						}
						continue;
					}
					if (newValue == null) {
						return IMarker.LINE_NUMBER.equals(attr) ? MAJOR_CHANGE
								: MINOR_CHANGE;
					}
					if (!oldValue.equals(newValue)) {
						return IMarker.LINE_NUMBER.equals(attr) ? MAJOR_CHANGE
								: MINOR_CHANGE;
					}
				}
			}
		} catch (CoreException e) {
			DLTKDebugPlugin.log(e);
		}
		return NO_CHANGES;
	}

	private int classifyBreakpointChange(IMarkerDelta delta,
			IScriptBreakpoint breakpoint, String attr) throws CoreException {
		final boolean conditional = ScriptBreakpointUtils
				.isConditional(breakpoint);
		if (conditional && AbstractScriptBreakpoint.EXPRESSION.equals(attr)) {
			return MAJOR_CHANGE;
		}
		final boolean oldExprState = delta.getAttribute(
				AbstractScriptBreakpoint.EXPRESSION_STATE, false);
		final String oldExpr = delta.getAttribute(
				AbstractScriptBreakpoint.EXPRESSION, null);
		if (ScriptBreakpointUtils.isConditional(oldExprState,
				oldExpr) != conditional) {
			return MAJOR_CHANGE;
		}
		if (IMarker.LINE_NUMBER.equals(attr)
				&& !target.getOptions().get(
						DebugOption.DBGP_BREAKPOINT_UPDATE_LINE_NUMBER)) {
			return MAJOR_CHANGE;
		}
		return MINOR_CHANGE;
	}

	// DebugTarget
	private final IScriptDebugTarget target;

	private void changeSpawnpoint(final IDbgpSession session,
			IScriptSpawnpoint spawnpoint) throws DbgpException, CoreException {
		final IDbgpSpawnpointCommands commands = (IDbgpSpawnpointCommands) session
				.get(IDbgpSpawnpointCommands.class);
		if (commands != null) {
			final String id = spawnpoint.getId(session);
			if (id != null) {
				commands.updateSpawnpoint(id, spawnpoint.isEnabled());
			}
		}
	}

	protected void removeSpawnpoint(final IDbgpSession session,
			IScriptSpawnpoint spawnpoint) throws DbgpException, CoreException {
		final IDbgpSpawnpointCommands commands = (IDbgpSpawnpointCommands) session
				.get(IDbgpSpawnpointCommands.class);
		if (commands != null) {
			final String id = spawnpoint.getId(session);
			if (id != null) {
				commands.removeSpawnpoint(id);
				spawnpoint.setId(session, null);
			}
		}
	}

	public ScriptBreakpointManager(IScriptDebugTarget target,
			IScriptBreakpointPathMapper pathMapper,
			IScriptBreakpointLineMapper lineMapper) {
		this.target = target;
		this.bpPathMapper = pathMapper;
		this.bpLineMapper = lineMapper;
		this.sessions = NO_SESSIONS;
	}

	public boolean supportsBreakpoint(IBreakpoint breakpoint) {
		if (breakpoint instanceof IScriptBreakpoint) {
			return StrUtils.equals(breakpoint.getModelIdentifier(),
					target.getModelIdentifier());
		}

		return false;
	}

	private void threadAccepted() {
		IBreakpointManager manager = getBreakpointManager();

		manager.addBreakpointListener(target);
		manager.addBreakpointManagerListener(this);
	}

	public void threadTerminated() {
		IBreakpointManager manager = getBreakpointManager();

		manager.removeBreakpointListener(target);
		manager.removeBreakpointManagerListener(this);

		if (bpPathMapper instanceof IScriptBreakpointPathMapperExtension) {
			((IScriptBreakpointPathMapperExtension) bpPathMapper).clearCache();
		}
	}

	synchronized IDbgpSession[] getSessions() {
		return sessions;
	}

	private synchronized boolean addSession(IDbgpSession session) {
		for (int i = 0; i < sessions.length; ++i) {
			if (session.equals(sessions[i])) {
				return false;
			}
		}
		final IDbgpSession[] temp = new IDbgpSession[sessions.length + 1];
		System.arraycopy(sessions, 0, temp, 0, sessions.length);
		temp[sessions.length] = session;
		sessions = temp;
		return true;
	}

	synchronized boolean removeSession(IDbgpSession session) {
		for (int i = 0; i < sessions.length; ++i) {
			if (session.equals(sessions[i])) {
				if (sessions.length == 1) {
					sessions = NO_SESSIONS;
				} else {
					final IDbgpSession[] temp = new IDbgpSession[sessions.length
							- 1];
					if (i > 0) {
						System.arraycopy(sessions, 0, temp, 0, i);
					}
					++i;
					if (i < sessions.length) {
						System.arraycopy(sessions, i, temp, i - 1,
								sessions.length - i);
					}
					sessions = temp;
				}
				return true;
			}
		}
		return false;
	}

	public void initializeSession(IDbgpSession session,
			IProgressMonitor monitor) {
		if (!addSession(session)) {
			return;
		}
		IBreakpoint[] breakpoints = getBreakpointManager().getBreakpoints(
				target.getModelIdentifier());
		monitor.beginTask(Util.EMPTY_STRING, breakpoints.length);

		for (int i = 0; i < breakpoints.length; i++) {
			try {
				final IBreakpoint breakpoint = breakpoints[i];
				if (breakpoint instanceof IScriptSpawnpoint) {
					addSpawnpoint(session, (IScriptSpawnpoint) breakpoint);
				} else {
					addBreakpoint(session, (IScriptBreakpoint) breakpoint);
				}
			} catch (Exception e) {
				DLTKDebugPlugin.logWarning(
						NLS.bind(Messages.ErrorSetupDeferredBreakpoints,
								e.getMessage()),
						e);
				if (DLTKCore.DEBUG) {
					e.printStackTrace();
				}
			}
			monitor.worked(1);
		}
		threadAccepted();
		monitor.done();
	}

	private static class TemporaryBreakpoint implements IDebugEventSetListener {
		final ScriptBreakpointManager manager;
		final Map<IDbgpSession, String> ids = new IdentityHashMap<IDbgpSession, String>(
				1);

		/**
		 * @param manager
		 * @param uri
		 * @param line
		 */
		public TemporaryBreakpoint(ScriptBreakpointManager manager, URI uri,
				int line) {
			this.manager = manager;
			final IDbgpSession[] sessions = manager.getSessions();
			for (int i = 0; i < sessions.length; ++i) {
				DbgpBreakpointConfig config = new DbgpBreakpointConfig(true);
				try {
					final String id = sessions[i].getCoreCommands()
							.setLineBreakpoint(uri, line, config);
					if (id != null) {
						ids.put(sessions[i], id);
					}
				} catch (DbgpException e) {
					DLTKDebugPlugin.log(e);
				}
			}
		}

		public void handleDebugEvents(DebugEvent[] events) {
			for (int i = 0; i < events.length; ++i) {
				DebugEvent event = events[i];
				if (event.getKind() == DebugEvent.SUSPEND) {
					removeBreakpoint();
					DebugPlugin.getDefault().removeDebugEventListener(this);
					break;
				}
			}
		}

		private void removeBreakpoint() {
			try {
				final IDbgpSession[] sessions = manager.getSessions();
				for (int i = 0; i < sessions.length; ++i) {
					final IDbgpSession session = sessions[i];
					final String id = (String) ids.remove(session);
					if (id != null) {
						session.getCoreCommands().removeBreakpoint(id);
					}
				}
			} catch (DbgpException e) {
				DLTKDebugPlugin.log(e);
			}
		}

	}

	public void setBreakpointUntilFirstSuspend(URI uri, int line) {
		uri = bpPathMapper.map(uri);
		final TemporaryBreakpoint temp = new TemporaryBreakpoint(this, uri,
				line);
		if (!temp.ids.isEmpty()) {
			DebugPlugin.getDefault().addDebugEventListener(temp);
		}
	}

	// IBreakpointListener
	public void breakpointAdded(final IBreakpoint breakpoint) {
		if (!supportsBreakpoint(breakpoint)) {
			return;
		}
		final IDbgpSession[] sessions = getSessions();
		for (int i = 0; i < sessions.length; ++i) {
			final IDbgpSession session = sessions[i];
			scheduleBackgroundOperation(target, new Runnable() {
				@Override
				public void run() {
					try {
						if (breakpoint instanceof IScriptSpawnpoint) {
							addSpawnpoint(session,
									(IScriptSpawnpoint) breakpoint);
						} else {
							addBreakpoint(session,
									(IScriptBreakpoint) breakpoint);
						}
					} catch (Exception e) {
						DLTKDebugPlugin.log(e);
					}
				}
			});
		}

	}

	/**
	 * @see IBreakpointListener#breakpointChanged(IBreakpoint, IMarkerDelta)
	 * @param breakpoint
	 * @param delta
	 *            if delta is <code>null</code> then there was a call to
	 *            BreakPointManager.fireBreakpointChanged(IBreakpoint
	 *            breakpoint), so see it as a major change.
	 */
	public void breakpointChanged(final IBreakpoint breakpoint,
			final IMarkerDelta delta) {
		if (!supportsBreakpoint(breakpoint)) {
			return;
		}
		if (breakpoint instanceof IScriptSpawnpoint) {
			final int changes = delta != null ? hasSpawnpointChanges(delta,
					(IScriptSpawnpoint) breakpoint) : MAJOR_CHANGE;
			if (changes != NO_CHANGES) {
				final IDbgpSession[] sessions = getSessions();
				for (int i = 0; i < sessions.length; ++i) {
					final IDbgpSession session = sessions[i];
					scheduleBackgroundOperation(target, new Runnable() {
						@Override
						public void run() {
							try {
								if (changes == MAJOR_CHANGE) {
									removeSpawnpoint(session,
											(IScriptSpawnpoint) breakpoint);
									addSpawnpoint(session,
											(IScriptSpawnpoint) breakpoint);
								} else {
									changeSpawnpoint(session,
											(IScriptSpawnpoint) breakpoint);
								}
							} catch (Exception e) {
								DLTKDebugPlugin.logError(e.getMessage(), e);
							}
						}
					});
				}
			}
		} else {
			final IScriptBreakpoint sbp = (IScriptBreakpoint) breakpoint;
			final int changes = delta != null ? hasBreakpointChanges(delta, sbp)
					: MAJOR_CHANGE;
			if (changes != NO_CHANGES) {
				final IDbgpSession[] sessions = getSessions();
				for (int i = 0; i < sessions.length; ++i) {
					final IDbgpSession session = sessions[i];
					scheduleBackgroundOperation(target, new Runnable() {
						@Override
						public void run() {
							try {
								if (changes == MAJOR_CHANGE) {
									removeBreakpoint(session, sbp);
									addBreakpoint(session, sbp);
								} else {
									changeBreakpoint(session, sbp);
								}
							} catch (Exception e) {
								DLTKDebugPlugin.logError(e.getMessage(), e);
							}
						}
					});
				}
			}
		}
	}

	public void breakpointRemoved(final IBreakpoint breakpoint,
			final IMarkerDelta delta) {
		if (!supportsBreakpoint(breakpoint)) {
			return;
		}
		final IDbgpSession[] sessions = getSessions();
		for (int i = 0; i < sessions.length; ++i) {
			final IDbgpSession session = sessions[i];
			scheduleBackgroundOperation(target, new Runnable() {
				@Override
				public void run() {
					try {
						if (breakpoint instanceof IScriptSpawnpoint) {
							removeSpawnpoint(session,
									(IScriptSpawnpoint) breakpoint);
						} else {
							removeBreakpoint(session,
									(IScriptBreakpoint) breakpoint);
						}
					} catch (Exception e) {
						DLTKDebugPlugin.log(e);
					}
				}
			});
		}
	}

	// IBreakpointManagerListener
	public void breakpointManagerEnablementChanged(boolean enabled) {
		IBreakpoint[] breakpoints = getBreakpointManager().getBreakpoints(
				target.getModelIdentifier());
		final IDbgpSession[] sessions = getSessions();
		for (int i = 0; i < breakpoints.length; ++i) {
			final IBreakpoint breakpoint = breakpoints[i];
			for (int j = 0; j < sessions.length; ++j) {
				final IDbgpSession session = sessions[j];
				scheduleBackgroundOperation(target, new Runnable() {
					@Override
					public void run() {
						try {
							if (breakpoint instanceof IScriptSpawnpoint) {
								changeSpawnpoint(session,
										(IScriptSpawnpoint) breakpoint);
							} else {
								changeBreakpoint(session,
										(IScriptBreakpoint) breakpoint);
							}
						} catch (Exception e) {
							DLTKDebugPlugin.log(e);
						}
					}
				});
			}
		}
	}

	private void scheduleBackgroundOperation(final IScriptDebugTarget target,
			final Runnable runnable) {
		String name = target.getLaunch().getLaunchConfiguration().getName();

		Job breakpointBackgroundJob = new Job("Update target breakpoints: " //$NON-NLS-1$
				+ name) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					runnable.run();
				} catch (Exception r) {
					DLTKDebugPlugin.logError(r.getMessage(), r);
				}
				return Status.OK_STATUS;
			}
		};
		breakpointBackgroundJob.setSystem(true);
		breakpointBackgroundJob.schedule();
	}
}
