package org.eclipse.dltk.launching;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.dltk.compiler.util.Util;
import org.eclipse.dltk.core.PreferencesLookupDelegate;
import org.eclipse.dltk.debug.core.DLTKDebugPlugin;
import org.eclipse.dltk.debug.core.IDbgpService;
import org.eclipse.dltk.debug.core.model.IScriptDebugTarget;
import org.eclipse.dltk.internal.debug.core.model.RemoteDbgpService;
import org.eclipse.dltk.internal.debug.core.model.RemoteScriptDebugTarget;
import org.eclipse.dltk.internal.debug.core.model.ScriptDebugTarget;

public abstract class RemoteDebuggingEngineRunner extends DebuggingEngineRunner {

	public RemoteDebuggingEngineRunner(IInterpreterInstall install) {
		super(install);
	}

	@Override
	protected IScriptDebugTarget createDebugTarget(ILaunch launch,
			IDbgpService dbgpService) throws CoreException {
		return new RemoteScriptDebugTarget(getDebugModelId(), dbgpService,
				getSessionId(launch.getLaunchConfiguration()), launch, null);
	}

	/*
	 * @see DebuggingEngineRunner#getSessionId(ILaunchConfiguration)
	 */
	@Override
	protected String getSessionId(ILaunchConfiguration configuration)
			throws CoreException {
		return "remote:" + getDbgpService().getPort();
	}

	/*
	 * @see DebuggingEngineRunner#addEngineConfig(InterpreterConfig,
	 * IScriptProject,ILaunch)
	 */
	@Override
	protected InterpreterConfig addEngineConfig(InterpreterConfig config,
			PreferencesLookupDelegate delegate, ILaunch launch) {
		return config;
	}

	/*
	 * @see DebuggingEngineRunner#run(InterpreterConfig, ILaunch,
	 * IProgressMonitor)
	 */
	@Override
	public void run(InterpreterConfig config, ILaunch launch,
			IProgressMonitor monitor) throws CoreException {
		try {
			initializeLaunch(launch, config,
					createPreferencesLookupDelegate(launch));
			((RemoteDbgpService) getDbgpService()).connect();
			final ScriptDebugTarget target = (ScriptDebugTarget) launch
					.getDebugTarget();
			waitDebuggerConnected(launch, new DebugSessionAcceptor(target,
					monitor));
		} catch (CoreException e) {
			launch.terminate();
			throw e;
		}
	}

	/**
	 * @see org.eclipse.dltk.launching.DebuggingEngineRunner#getDbgpService()
	 */
	protected IDbgpService getDbgpService() {
		return DLTKDebugPlugin.getDefault().getRemoteDbgpService();
	}

	/*
	 * @see DebuggingEngineRunner#getDebuggingEngineId()
	 */
	@Override
	protected String getDebuggingEngineId() {
		return "remote";
	}

	/*
	 * @see DebuggingEngineRunner#getDebuggingEnginePreferenceQualifier()
	 */
	@Override
	protected String getDebuggingEnginePreferenceQualifier() {
		return getDebugPreferenceQualifier();
	}

	/*
	 * @see DebuggingEngineRunner#getLogFileNamePreferenceKey()
	 */
	@Override
	protected String getLogFileNamePreferenceKey() {
		// not supported on the client side
		return null;
	}

}
