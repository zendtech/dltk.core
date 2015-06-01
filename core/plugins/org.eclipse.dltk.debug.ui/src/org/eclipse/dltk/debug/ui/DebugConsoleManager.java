/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 
 *******************************************************************************/
package org.eclipse.dltk.debug.ui;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.ILaunchesListener2;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.debug.ui.console.ConsoleColorProvider;
import org.eclipse.debug.ui.console.IConsoleColorProvider;
import org.eclipse.dltk.compiler.util.Util;
import org.eclipse.dltk.debug.core.DLTKDebugLaunchConstants;
import org.eclipse.dltk.debug.core.model.IScriptDebugTarget;
import org.eclipse.dltk.launching.ScriptLaunchConfigurationConstants;
import org.eclipse.dltk.launching.process.IScriptProcess;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.IOConsole;

import com.ibm.icu.text.MessageFormat;

public class DebugConsoleManager implements ILaunchesListener2 {

	private static DebugConsoleManager instance;

	public static synchronized DebugConsoleManager getInstance() {
		if (instance == null) {
			instance = new DebugConsoleManager();
		}
		return instance;
	}

	private final Map<ILaunch, ScriptDebugConsole> launchToConsoleMap = Collections
			.synchronizedMap(new HashMap<ILaunch, ScriptDebugConsole>());

	protected boolean acceptLaunch(ILaunch launch) {
		if (launch == null) {
			return false;
		}
		if (!Boolean.parseBoolean(launch.getAttribute(
				ScriptLaunchConfigurationConstants.ATTR_USE_CONSOLE_IN_RUN_MODE))) {
			if (!ILaunchManager.DEBUG_MODE.equals(launch.getLaunchMode())) {
				return false;
			}
		}
		return launch.getProcesses().length != 0
				&& DLTKDebugLaunchConstants.isDebugConsole(launch)
				|| launch.getDebugTarget() instanceof IScriptDebugTarget
						&& ((IScriptDebugTarget) launch.getDebugTarget())
								.isRemote();
	}

	/**
	 * @since 2.0
	 */
	protected ScriptDebugConsole createConsole(ILaunch launch) {
		final String encoding = selectEncoding(launch);
		final IProcess[] processes = launch.getProcesses();
		final IProcess process = processes.length != 0 ? processes[0] : null;
		final IConsoleColorProvider colorProvider = getColorProvider(
				process != null
						? process.getAttribute(IProcess.ATTR_PROCESS_TYPE)
						: null);
		final ScriptDebugConsole console = new ScriptDebugConsole(launch,
				computeName(launch), null, encoding, colorProvider);
		if (process != null) {
			console.setAttribute(IDebugUIConstants.ATTR_CONSOLE_PROCESS,
					process);
			if (process instanceof IScriptProcess) {
				console.connect((IScriptProcess) process);
			}
		}
		final IConsoleManager manager = getConsoleManager();
		manager.addConsoles(new IConsole[] { console });
		manager.showConsoleView(console);
		return console;
	}

	private String selectEncoding(ILaunch launch) {
		String encoding = launch
				.getAttribute(DebugPlugin.ATTR_CONSOLE_ENCODING);
		if (encoding != null) {
			return encoding;
		}
		final ILaunchConfiguration configuration = launch
				.getLaunchConfiguration();
		if (configuration != null) {
			try {
				return DebugPlugin.getDefault().getLaunchManager()
						.getEncoding(configuration);
			} catch (CoreException e) {
				DLTKDebugUIPlugin.log(e);
			}
		}
		return ResourcesPlugin.getEncoding();
	}

	protected void destroyConsole(IOConsole console) {
		getConsoleManager().removeConsoles(new IConsole[] { console });
	}

	private IConsoleManager getConsoleManager() {
		return ConsolePlugin.getDefault().getConsoleManager();
	}

	protected DebugConsoleManager() {
	}

	/**
	 * @since 2.0
	 */
	protected String computeName(ILaunch launch) {
		final IProcess[] processes = launch.getProcesses();
		String consoleName;
		if (processes.length != 0) {
			final IProcess process = processes[0];
			ILaunchConfiguration config = process.getLaunch()
					.getLaunchConfiguration();
			consoleName = process.getAttribute(IProcess.ATTR_PROCESS_LABEL);
			if (consoleName == null) {
				if (config == null || DebugUITools.isPrivate(config)) {
					// No config or PRIVATE
					consoleName = process.getLabel();
				} else {
					consoleName = computeName(config, process);
				}
			}
		} else {
			final ILaunchConfiguration config = launch.getLaunchConfiguration();
			if (config != null) {
				consoleName = computeName(config, null);
			} else {
				consoleName = Util.EMPTY_STRING;
			}
		}
		consoleName = Messages.DebugConsoleManager_debugConsole + " " //$NON-NLS-1$
				+ consoleName;
		if (launch.isTerminated()) {
			consoleName = NLS.bind(Messages.DebugConsoleManager_terminated,
					consoleName);
		}
		return consoleName;
	}

	/**
	 * @since 2.0
	 */
	protected String computeName(ILaunchConfiguration config,
			IProcess process) {
		String type = null;
		try {
			type = config.getType().getName();
		} catch (CoreException e) {
		}
		StringBuffer buffer = new StringBuffer();
		buffer.append(config.getName());
		if (type != null) {
			buffer.append(" ["); //$NON-NLS-1$
			buffer.append(type);
			buffer.append("]"); //$NON-NLS-1$
		}
		if (process != null) {
			buffer.append(" "); //$NON-NLS-1$
			buffer.append(process.getLabel());
		}
		return buffer.toString();
	}

	/**
	 * @since 2.0
	 */
	public void launchesAdded(ILaunch[] launches) {
		launchesChanged(launches);
	}

	/**
	 * @since 2.0
	 */
	public void launchesChanged(ILaunch[] launches) {
		for (ILaunch launch : launches) {
			if (acceptLaunch(launch)) {
				ScriptDebugConsole console = launchToConsoleMap.get(launch);
				if (console == null) {
					console = createConsole(launch);
					launchToConsoleMap.put(launch, console);
				}
				final IProcess[] processes = launch.getProcesses();
				if (processes.length != 0
						&& processes[0] instanceof IScriptProcess) {
					// console.con
				}
				if (launch.getDebugTarget() instanceof IScriptDebugTarget) {
					IScriptDebugTarget target = (IScriptDebugTarget) launch
							.getDebugTarget();
					if (target != null && target.getStreamProxy() == null) {
						target.setStreamProxy(new ScriptStreamProxy(console));
					}
				}
			}
		}
	}

	/**
	 * @since 2.0
	 */
	public void launchesRemoved(ILaunch[] launches) {
		for (ILaunch launch : launches) {
			final ScriptDebugConsole console = launchToConsoleMap.get(launch);
			if (console != null) {
				destroyConsole(console);
				launchToConsoleMap.remove(launch);
			}
		}
	}

	/**
	 * @since 2.0
	 */
	public void launchesTerminated(ILaunch[] launches) {
		for (ILaunch launch : launches) {
			final ScriptDebugConsole console = launchToConsoleMap.get(launch);
			if (console != null) {
				final String newName = computeName(launch);
				if (!newName.equals(console.getName())) {
					final Runnable r = new Runnable() {
						public void run() {
							console.setName(newName);
						}
					};
					DLTKDebugUIPlugin.getStandardDisplay().asyncExec(r);
				}
			}
		}
	}

	/**
	 * Console document content provider extensions, keyed by extension id
	 */
	private Map<String, IConfigurationElement> fColorProviders = null;

	/**
	 * The default color provider. Used if no color provider is contributed for
	 * the given process type.
	 */
	private IConsoleColorProvider fDefaultColorProvider;

	/**
	 * Returns a new console document color provider extension for the given
	 * process type, or <code>null</code> if none.
	 * 
	 * @param type
	 *            corresponds to <code>IProcess.ATTR_PROCESS_TYPE</code>
	 * @return IConsoleColorProvider
	 */
	private IConsoleColorProvider getColorProvider(String type) {
		if (fColorProviders == null) {
			fColorProviders = new HashMap<String, IConfigurationElement>();
			IExtensionPoint extensionPoint = Platform.getExtensionRegistry()
					.getExtensionPoint(IDebugUIConstants.PLUGIN_ID,
							IDebugUIConstants.EXTENSION_POINT_CONSOLE_COLOR_PROVIDERS);
			IConfigurationElement[] elements = extensionPoint
					.getConfigurationElements();
			for (int i = 0; i < elements.length; i++) {
				IConfigurationElement extension = elements[i];
				fColorProviders.put(extension.getAttribute("processType"), //$NON-NLS-1$
						extension);
			}
		}
		IConfigurationElement extension = fColorProviders.get(type);
		if (extension != null) {
			try {
				Object colorProvider = extension
						.createExecutableExtension("class"); //$NON-NLS-1$
				if (colorProvider instanceof IConsoleColorProvider) {
					return (IConsoleColorProvider) colorProvider;
				}
				DLTKDebugUIPlugin.logErrorMessage(MessageFormat.format(
						"Extension {0} must specify an instanceof IConsoleColorProvider for class attribute.", //$NON-NLS-1$
						new String[] { extension.getDeclaringExtension()
								.getUniqueIdentifier() }));
			} catch (CoreException e) {
				DLTKDebugUIPlugin.log(e);
			}
		}
		// no color provider found of specified type, return default color
		// provider.
		if (fDefaultColorProvider == null) {
			fDefaultColorProvider = new ConsoleColorProvider();
		}
		return fDefaultColorProvider;
	}

}
