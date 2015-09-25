/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 
 *******************************************************************************/
package org.eclipse.dltk.internal.ui.actions;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.dltk.core.DLTKCore;
import org.eclipse.dltk.core.IModelElement;
import org.eclipse.dltk.core.IProjectFragment;
import org.eclipse.dltk.internal.core.ModelManager;
import org.eclipse.dltk.internal.ui.navigator.ProjectFragmentContainer;
import org.eclipse.dltk.ui.DLTKPluginImages;
import org.eclipse.dltk.ui.DLTKUIPlugin;
import org.eclipse.dltk.ui.actions.SelectionDispatchAction;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.IShellProvider;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.IWorkingSet;

/**
 * Action for refreshing the workspace from the local file system for the
 * selected resources and all of their descendants. This action also considers
 * external archives managed by the Script Model.
 * <p>
 * Action is applicable to selections containing resources and Script elements
 * down to compilation units.
 * 
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 */
public class RefreshAction extends SelectionDispatchAction {

	/**
	 * As the DLTK RefreshAction is already API, we have to wrap the workbench
	 * action.
	 */
	private static class WrappedWorkbenchRefreshAction
			extends org.eclipse.ui.actions.RefreshAction {

		public WrappedWorkbenchRefreshAction(IShellProvider provider) {
			super(provider);
		}

		@Override
		protected List<? extends IResource> getSelectedResources() {
			List<? extends IResource> selectedResources = super.getSelectedResources();
			if (!getStructuredSelection().isEmpty()
					&& selectedResources.size() == 1
					&& selectedResources.get(0) instanceof IWorkspaceRoot) {
				selectedResources = Collections.emptyList(); // Refresh action
																// refreshes
																// root when it
																// can't find
																// any resources
																// in selection
			}

			ArrayList<IResource> allResources = new ArrayList<IResource>(
					selectedResources);
			addWorkingSetResources(allResources);
			return allResources;
		}

		private void addWorkingSetResources(List<IResource> selectedResources) {
			Object[] elements = getStructuredSelection().toArray();
			for (int i = 0; i < elements.length; i++) {
				Object curr = elements[i];
				if (curr instanceof IWorkingSet) {
					IAdaptable[] members = ((IWorkingSet) curr).getElements();
					for (int k = 0; k < members.length; k++) {
						IResource adapted = (IResource) members[k]
								.getAdapter(IResource.class);
						if (adapted != null) {
							selectedResources.add(adapted);
						}
					}
				}
			}
		}

		public void run(IProgressMonitor monitor)
				throws CoreException, OperationCanceledException {
			try {
				final IStatus[] errorStatus = new IStatus[] {
						Status.OK_STATUS };
				createOperation(errorStatus).run(monitor);
				if (errorStatus[0].matches(IStatus.ERROR)) {
					throw new CoreException(errorStatus[0]);
				}
			} catch (InvocationTargetException e) {
				Throwable targetException = e.getTargetException();
				if (targetException instanceof CoreException)
					throw (CoreException) targetException;
				throw new CoreException(
						new Status(IStatus.ERROR, DLTKUIPlugin.PLUGIN_ID,
								ActionMessages.RefreshAction_error_workbenchaction_message,
								targetException));
			} catch (InterruptedException e) {
				throw new OperationCanceledException();
			}
		}
	}

	/**
	 * Creates a new <code>RefreshAction</code>. The action requires that the
	 * selection provided by the site's selection provider is of type
	 * {@link org.eclipse.jface.viewers.IStructuredSelection} .
	 * 
	 * @param site
	 *            the site providing context information for this action
	 */
	public RefreshAction(IWorkbenchSite site) {
		super(site);
		setText(ActionMessages.RefreshAction_label);
		setToolTipText(ActionMessages.RefreshAction_toolTip);
		DLTKPluginImages.setLocalImageDescriptors(this, "refresh_nav.png");//$NON-NLS-1$
		if (DLTKCore.DEBUG) {
			System.err.println("Add help support here..."); //$NON-NLS-1$
		}

		// PlatformUI.getWorkbench().getHelpSystem().setHelp(this,
		// IScriptHelpContextIds.REFRESH_ACTION);
	}

	/**
	 * Method declared in SelectionDispatchAction
	 */
	@Override
	public void selectionChanged(IStructuredSelection selection) {
		setEnabled(checkEnabled(selection));
	}

	private boolean checkEnabled(IStructuredSelection selection) {
		if (selection.isEmpty())
			return true;
		for (Iterator<?> iter = selection.iterator(); iter.hasNext();) {
			Object element = iter.next();
			if (element instanceof IWorkingSet) {
				// don't inspect working sets any deeper.
			} else if (element instanceof IAdaptable) {
				IResource resource = (IResource) ((IAdaptable) element)
						.getAdapter(IResource.class);
				if (resource == null)
					return false;
				if (resource.getType() == IResource.PROJECT
						&& !((IProject) resource).isOpen())
					return false;
			} else {
				return false;
			}
		}
		return true;
	}

	private void performRefresh(IStructuredSelection selection,
			IProgressMonitor monitor)
					throws CoreException, OperationCanceledException {
		monitor.beginTask(ActionMessages.RefreshAction_progressMessage, 2);

		WrappedWorkbenchRefreshAction workbenchAction = new WrappedWorkbenchRefreshAction(
				getSite());
		workbenchAction.selectionChanged(selection);
		workbenchAction.run(new SubProgressMonitor(monitor, 1));
		refreshScriptElements(selection, new SubProgressMonitor(monitor, 1));
	}

	private void refreshScriptElements(IStructuredSelection selection,
			SubProgressMonitor monitor) throws CoreException {
		Object[] selectedElements = selection.toArray();
		ArrayList<IModelElement> modelElements = new ArrayList<IModelElement>();
		for (int i = 0; i < selectedElements.length; i++) {
			Object curr = selectedElements[i];
			if (curr instanceof IProjectFragment) {
				modelElements.add((IProjectFragment) curr);
			} else if (curr instanceof ProjectFragmentContainer) {
				modelElements
						.addAll(Arrays.asList(((ProjectFragmentContainer) curr)
								.getProjectFragments()));
			} else if (curr instanceof IWorkingSet) {
				IAdaptable[] members = ((IWorkingSet) curr).getElements();
				for (int k = 0; k < members.length; k++) {
					IModelElement adapted = (IModelElement) members[k]
							.getAdapter(IModelElement.class);
					if (adapted instanceof ProjectFragmentContainer) {
						modelElements.add(adapted);
					}
				}
			}
		}
		if (!modelElements.isEmpty()) {
			ModelManager.getModelManager().getDeltaProcessor()
					.checkExternalChanges(
							modelElements.toArray(
									new IModelElement[modelElements.size()]),
							monitor);
		}
	}

	/**
	 * Method declared in SelectionDispatchAction
	 */
	@Override
	public void run(final IStructuredSelection selection) {
		IWorkspaceRunnable operation = new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException {
				performRefresh(selection, monitor);
			}
		};
		new WorkbenchRunnableAdapter(operation).runAsUserJob(
				ActionMessages.RefreshAction_refresh_operation_label, null);
	}

}
