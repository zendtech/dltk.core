/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.dltk.ui.text.completion;

import org.eclipse.dltk.core.CompletionProposal;
import org.eclipse.jface.viewers.StyledString;

/**
 * @since 5.2
 */
public interface ICompletionProposalLabelProviderExtension {

	StyledString createStyledFieldProposalLabel(CompletionProposal proposal);

	StyledString createStyledLabel(CompletionProposal fProposal);

	StyledString createStyledKeywordLabel(CompletionProposal proposal);

	StyledString createStyledSimpleLabel(CompletionProposal proposal);

	StyledString createStyledTypeProposalLabel(CompletionProposal proposal);

	StyledString createStyledSimpleLabelWithType(CompletionProposal proposal);
}
