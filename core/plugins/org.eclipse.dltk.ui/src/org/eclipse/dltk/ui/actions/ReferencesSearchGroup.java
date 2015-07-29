/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 
 *******************************************************************************/
package org.eclipse.dltk.ui.actions;

import java.util.Iterator;

import org.eclipse.core.runtime.Assert;
import org.eclipse.dltk.core.IDLTKLanguageToolkit;
import org.eclipse.dltk.internal.ui.callhierarchy.SearchUtil;
import org.eclipse.dltk.internal.ui.editor.ScriptEditor;
import org.eclipse.dltk.internal.ui.search.SearchMessages;
import org.eclipse.dltk.ui.IContextMenuConstants;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.actions.ActionGroup;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditor;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;


/**
 * Action group that adds the search for references actions to a context menu
 * and the global menu bar.
 * 
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 */
public class ReferencesSearchGroup extends ActionGroup {

	private static final String MENU_TEXT = SearchMessages.group_references;

	private IWorkbenchSite fSite;

	private AbstractDecoratedTextEditor fEditor;

	private IActionBars fActionBars;

	private String fGroupId;

	private FindReferencesAction fFindReferencesAction;

	private FindReferencesInProjectAction fFindReferencesInProjectAction;

	private FindReferencesInHierarchyAction fFindReferencesInHierarchyAction;

	private FindReferencesInWorkingSetAction fFindReferencesInWorkingSetAction;

	private final IDLTKLanguageToolkit toolkit;

	private ISelectionProvider fSelectionProvider;
	
	/**
	 * Creates a new <code>ReferencesSearchGroup</code>. The group requires
	 * that the selection provided by the site's selection provider is of type
	 * <code>
	 * org.eclipse.jface.viewers.IStructuredSelection</code>.
	 * 
	 * @param site
	 *            the view part that owns this action group
	 */
	public ReferencesSearchGroup(IWorkbenchSite site, IDLTKLanguageToolkit tk) {
		fSite = site;
		fGroupId = IContextMenuConstants.GROUP_SEARCH;
		this.toolkit = tk;

		fFindReferencesAction = new FindReferencesAction(toolkit, site);
		fFindReferencesAction.setActionDefinitionId(IScriptEditorActionDefinitionIds.SEARCH_REFERENCES_IN_WORKSPACE);

		fFindReferencesInProjectAction = new FindReferencesInProjectAction(
				toolkit, site);
		fFindReferencesInProjectAction.setActionDefinitionId(IScriptEditorActionDefinitionIds.SEARCH_REFERENCES_IN_PROJECT);

		fFindReferencesInHierarchyAction = new FindReferencesInHierarchyAction(
				toolkit, site);
		fFindReferencesInHierarchyAction.setActionDefinitionId(IScriptEditorActionDefinitionIds.SEARCH_REFERENCES_IN_HIERARCHY);

		fFindReferencesInWorkingSetAction = new FindReferencesInWorkingSetAction(
				toolkit, site);
		fFindReferencesInWorkingSetAction.setActionDefinitionId(IScriptEditorActionDefinitionIds.SEARCH_REFERENCES_IN_WORKING_SET);

		// register the actions as selection listeners
		fSelectionProvider = fSite.getSelectionProvider();
		assert fSelectionProvider != null;

		registerActions();
	}

	/**
	 * Note: This constructor is for internal use only. Clients should not call
	 * this constructor.
	 * 
	 * @param editor the Script editor
	 */
	public ReferencesSearchGroup(ScriptEditor editor,
			IDLTKLanguageToolkit tk) {
		this((AbstractDecoratedTextEditor) editor, tk);
	}
	
	/**
	 * Note: This constructor is for internal use only. Clients should not call
	 * this constructor.
	 * 
	 * @param editor
	 *            the Script editor
	 * @since 5.3
	 */
	public ReferencesSearchGroup(AbstractDecoratedTextEditor editor,
			IDLTKLanguageToolkit tk) {
		Assert.isNotNull(editor);
		this.toolkit = tk;
		fEditor = editor;
		fSite = fEditor.getSite();
		fGroupId = ITextEditorActionConstants.GROUP_FIND;

		fFindReferencesAction = new FindReferencesAction(toolkit, editor);
		fFindReferencesAction.setActionDefinitionId(IScriptEditorActionDefinitionIds.SEARCH_REFERENCES_IN_WORKSPACE);
		fEditor.setAction("SearchReferencesInWorkspace", fFindReferencesAction); //$NON-NLS-1$

		fFindReferencesInProjectAction = new FindReferencesInProjectAction(
				toolkit, fEditor);
		fFindReferencesInProjectAction.setActionDefinitionId(IScriptEditorActionDefinitionIds.SEARCH_REFERENCES_IN_PROJECT);
		fEditor.setAction("SearchReferencesInProject", fFindReferencesInProjectAction); //$NON-NLS-1$

		fFindReferencesInHierarchyAction = new FindReferencesInHierarchyAction(
				toolkit, fEditor);
		fFindReferencesInHierarchyAction.setActionDefinitionId(IScriptEditorActionDefinitionIds.SEARCH_REFERENCES_IN_HIERARCHY);
		fEditor.setAction("SearchReferencesInHierarchy", fFindReferencesInHierarchyAction); //$NON-NLS-1$

		fFindReferencesInWorkingSetAction = new FindReferencesInWorkingSetAction(
				toolkit, fEditor);
		fFindReferencesInWorkingSetAction.setActionDefinitionId(IScriptEditorActionDefinitionIds.SEARCH_REFERENCES_IN_WORKING_SET);
		fEditor.setAction("SearchReferencesInWorkingSet", fFindReferencesInWorkingSetAction); //$NON-NLS-1$
	}

	private void registerAction(SelectionDispatchAction action, ISelectionProvider provider, ISelection selection) {
		action.update(selection);
		provider.addSelectionChangedListener(action);
	}

	/**
	 * Note: this method is for internal use only. Clients should not call this
	 * method.
	 * 
	 * @return the menu label
	 */
	protected String getName() {
		return MENU_TEXT;
	}

	/*
	 * (non-Javadoc) Method declared in ActionGroup
	 */
	public void fillActionBars(IActionBars actionBars) {
		Assert.isNotNull(actionBars);
		super.fillActionBars(actionBars);
		fActionBars = actionBars;
		updateGlobalActionHandlers();
	}

	private void addAction(IAction action, IMenuManager manager) {
		if (action.isEnabled()) {
			manager.add(action);
		}
	}

	private void addWorkingSetAction(IWorkingSet[] workingSets, IMenuManager manager) {
		FindAction action;
		if (fEditor != null)
			action = new WorkingSetFindAction(fEditor,
					new FindReferencesInWorkingSetAction(toolkit, fEditor,
							workingSets), SearchUtil.toString(workingSets));
		else
			action = new WorkingSetFindAction(fSite,
					new FindReferencesInWorkingSetAction(toolkit, fSite,
							workingSets), SearchUtil.toString(workingSets));
		action.update(getContext().getSelection());
		addAction(action, manager);
	}

	/*
	 * (non-Javadoc) Method declared on ActionGroup.
	 */
	public void fillContextMenu(IMenuManager manager) {
		MenuManager javaSearchMM = new MenuManager(getName(), IContextMenuConstants.GROUP_SEARCH);
		addAction(fFindReferencesAction, javaSearchMM);
		addAction(fFindReferencesInProjectAction, javaSearchMM);
		addAction(fFindReferencesInHierarchyAction, javaSearchMM);

		javaSearchMM.add(new Separator());

		Iterator iter = SearchUtil.getLRUWorkingSets().sortedIterator();
		while (iter.hasNext()) {
			addWorkingSetAction((IWorkingSet[]) iter.next(), javaSearchMM);
		}
		addAction(fFindReferencesInWorkingSetAction, javaSearchMM);

		if (!javaSearchMM.isEmpty())
			manager.appendToGroup(fGroupId, javaSearchMM);
	}

	/*
	 * Overrides method declared in ActionGroup
	 */
	public void dispose() {
		unregisterActions();
		fSelectionProvider = null;
		fFindReferencesAction = null;
		fFindReferencesInProjectAction = null;
		fFindReferencesInHierarchyAction = null;
		fFindReferencesInWorkingSetAction = null;
		updateGlobalActionHandlers();
		super.dispose();
	}

	private void updateGlobalActionHandlers() {
		if (fActionBars != null) {
			fActionBars.setGlobalActionHandler(DLTKActionConstants.FIND_REFERENCES_IN_WORKSPACE, fFindReferencesAction);
			fActionBars.setGlobalActionHandler(DLTKActionConstants.FIND_REFERENCES_IN_PROJECT, fFindReferencesInProjectAction);
			fActionBars.setGlobalActionHandler(DLTKActionConstants.FIND_REFERENCES_IN_HIERARCHY, fFindReferencesInHierarchyAction);
			fActionBars.setGlobalActionHandler(DLTKActionConstants.FIND_REFERENCES_IN_WORKING_SET, fFindReferencesInWorkingSetAction);
		}
	}

	private void disposeAction(ISelectionChangedListener action, ISelectionProvider provider) {
		if (action != null)
			provider.removeSelectionChangedListener(action);
	}

	private void registerActions() {
		if (fEditor != null) {
			return;
		}
		ISelection selection = fSelectionProvider.getSelection();
		registerAction(fFindReferencesAction, fSelectionProvider, selection);
		registerAction(fFindReferencesInProjectAction, fSelectionProvider, selection);
		registerAction(fFindReferencesInHierarchyAction, fSelectionProvider, selection);
		registerAction(fFindReferencesInWorkingSetAction, fSelectionProvider, selection);
	}

	private void unregisterActions() {
		if (fEditor != null) {
			return;
		}
		disposeAction(fFindReferencesAction, fSelectionProvider);
		disposeAction(fFindReferencesInProjectAction, fSelectionProvider);
		disposeAction(fFindReferencesInHierarchyAction, fSelectionProvider);
		disposeAction(fFindReferencesInWorkingSetAction, fSelectionProvider);
	}

	/**
	 * Proxy to {@link SelectionDispatchAction#setSpecialSelectionProvider(ISelectionProvider)}
	 * 
	 * @since 5.3
	 * @param provider
	 */
	public void setSpecialSelectionProvider(ISelectionProvider provider) {
		assert fEditor != null || provider != null;
		unregisterActions();
		fSelectionProvider = provider;
		registerActions();

		fFindReferencesAction.setSpecialSelectionProvider(provider);
		fFindReferencesInProjectAction.setSpecialSelectionProvider(provider);
		fFindReferencesInHierarchyAction.setSpecialSelectionProvider(provider);
		fFindReferencesInWorkingSetAction.setSpecialSelectionProvider(provider);
	}
}
