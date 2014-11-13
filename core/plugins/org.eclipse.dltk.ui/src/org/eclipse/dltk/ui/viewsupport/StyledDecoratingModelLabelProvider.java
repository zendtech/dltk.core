/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.dltk.ui.viewsupport;

import org.eclipse.dltk.ui.ProblemsLabelDecorator;
import org.eclipse.jface.viewers.DecoratingStyledCellLabelProvider;
import org.eclipse.jface.viewers.DecorationContext;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.ui.PlatformUI;

/**
 * a DecoratingModelLabelProvider which support StyledText
 * 
 * replace org.eclipse.dltk.ui.viewsupport.DecoratingModelLabelProvider
 * 
 * @since 5.2
 */
public class StyledDecoratingModelLabelProvider extends DecoratingStyledCellLabelProvider implements ILabelProvider {

	/**
	 * Decorating label provider for DLTK. Combines a ScriptUILabelProvider with
	 * problem and override indicuator with the workbench decorator (label
	 * decorator extension point).
	 */
	public StyledDecoratingModelLabelProvider(ScriptUILabelProvider labelProvider) {
		this(labelProvider, true);
	}

	/**
	 * Decorating label provider for dltk. Combines a ScriptUILabelProvider (if
	 * enabled with problem indicator) with the workbench decorator (label
	 * decorator extension point).
	 */
	public StyledDecoratingModelLabelProvider(ScriptUILabelProvider labelProvider,
			boolean errorTick) {
		this(labelProvider, errorTick, true);
	}

	/**
	 * Decorating label provider for dltk. Combines a ScriptUILabelProvider (if
	 * enabled with problem indicator) with the workbench decorator (label
	 * decorator extension point).
	 */
	public StyledDecoratingModelLabelProvider(ScriptUILabelProvider labelProvider,
			boolean errorTick, boolean flatPackageMode) {
		super(labelProvider, PlatformUI.getWorkbench().getDecoratorManager()
				.getLabelDecorator(),null);

		if (errorTick) {
			labelProvider.addLabelDecorator(new ProblemsLabelDecorator(null));
		}
		setFlatPackageMode(flatPackageMode);
	}

	/**
	 * Tells the label decorator if the view presents packages flat or
	 * hierarchical.
	 * 
	 * @param enable
	 *            If set, packages are presented in flat mode.
	 */
	public void setFlatPackageMode(boolean enable) {
		if (enable) {
			setDecorationContext(DecorationContext.DEFAULT_CONTEXT);
		} else {
			setDecorationContext(DecorationContext.DEFAULT_CONTEXT);
			// TODO setDecorationContext(HierarchicalDecorationContext.CONTEXT);
		}
	}

	public String getText(Object element) {
		return getStyledText(element).toString();
	}
}