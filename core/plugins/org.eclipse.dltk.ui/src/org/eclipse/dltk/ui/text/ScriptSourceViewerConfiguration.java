/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 
 *******************************************************************************/
package org.eclipse.dltk.ui.text;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.dltk.annotations.Nullable;
import org.eclipse.dltk.compiler.task.ITodoTaskPreferences;
import org.eclipse.dltk.compiler.util.Util;
import org.eclipse.dltk.internal.ui.editor.ModelElementHyperlinkDetector;
import org.eclipse.dltk.internal.ui.editor.ScriptEditor;
import org.eclipse.dltk.internal.ui.editor.ScriptSourceViewer;
import org.eclipse.dltk.internal.ui.text.HTMLAnnotationHover;
import org.eclipse.dltk.internal.ui.text.HTMLTextPresenter;
import org.eclipse.dltk.internal.ui.text.ScriptCompositeReconcilingStrategy;
import org.eclipse.dltk.internal.ui.text.ScriptElementProvider;
import org.eclipse.dltk.internal.ui.text.ScriptReconciler;
import org.eclipse.dltk.internal.ui.text.hover.EditorTextHoverDescriptor;
import org.eclipse.dltk.internal.ui.text.hover.EditorTextHoverProxy;
import org.eclipse.dltk.internal.ui.text.hover.ScriptInformationProvider;
import org.eclipse.dltk.ui.CodeFormatterConstants;
import org.eclipse.dltk.ui.DLTKUIPlugin;
import org.eclipse.dltk.ui.actions.IScriptEditorActionDefinitionIds;
import org.eclipse.dltk.ui.formatter.ScriptFormatterManager;
import org.eclipse.dltk.ui.formatter.ScriptFormattingStrategy;
import org.eclipse.dltk.ui.text.completion.ContentAssistPreference;
import org.eclipse.dltk.ui.text.spelling.SpellCheckDelegate;
import org.eclipse.dltk.ui.text.util.AutoEditUtils;
import org.eclipse.dltk.ui.text.util.TabStyle;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.AbstractInformationControlManager;
import org.eclipse.jface.text.DefaultInformationControl;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.ITextViewerExtension2;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.formatter.IContentFormatter;
import org.eclipse.jface.text.formatter.MultiPassContentFormatter;
import org.eclipse.jface.text.hyperlink.IHyperlinkDetector;
import org.eclipse.jface.text.information.IInformationPresenter;
import org.eclipse.jface.text.information.IInformationProvider;
import org.eclipse.jface.text.information.InformationPresenter;
import org.eclipse.jface.text.quickassist.IQuickAssistAssistant;
import org.eclipse.jface.text.reconciler.IReconciler;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationHover;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.editors.text.TextSourceViewerConfiguration;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditorPreferenceConstants;
import org.eclipse.ui.texteditor.HyperlinkDetectorRegistry;
import org.eclipse.ui.texteditor.ITextEditor;

public abstract class ScriptSourceViewerConfiguration extends
		TextSourceViewerConfiguration {

	private IColorManager fColorManager;
	private ITextEditor fTextEditor;
	private String fDocumentPartitioning;

	public ScriptSourceViewerConfiguration(IColorManager colorManager,
			IPreferenceStore preferenceStore, ITextEditor editor,
			String partitioning) {
		super(preferenceStore);

		fColorManager = colorManager;
		fTextEditor = editor;
		fDocumentPartitioning = partitioning;

		initializeScanners();
	}

	protected void initializeScanners() {

	}

	/**
	 * Returns a scanner that is capable of detecting single line comments that
	 * contain todo tasks.
	 * 
	 * <p>
	 * Clients should make a call to this method to create the scanner in their
	 * overriden <code>initalizeScanners()</code> implementation.
	 * </p>
	 * 
	 * @param commentColor
	 *            comment color key
	 * @param tagColor
	 *            tag color key
	 * 
	 * @see #createCommentScanner(String, String, ITodoTaskPreferences)
	 */
	protected final AbstractScriptScanner createCommentScanner(
			String commentColor, String tagColor) {
		return createCommentScanner(commentColor, tagColor,
				new TodoTaskPreferencesOnPreferenceStore(fPreferenceStore));
	}

	/**
	 * Returns a scanner that is capable of detecting single line comments that
	 * contain todo tasks.
	 * 
	 * <p>
	 * Default implementation returns an instance of
	 * {@link ScriptCommentScanner}. Clients that need to define an alternate
	 * comment scanner implementation should override this method.
	 * </p>
	 * 
	 * @see #createCommentScanner(String, String)
	 */
	protected AbstractScriptScanner createCommentScanner(String commentColor,
			String tagColor, ITodoTaskPreferences taskPrefs) {
		return new ScriptCommentScanner(getColorManager(), fPreferenceStore,
				commentColor, tagColor, taskPrefs);
	}

	@Override
	public String getConfiguredDocumentPartitioning(ISourceViewer sourceViewer) {
		if (fDocumentPartitioning != null)
			return fDocumentPartitioning;
		return super.getConfiguredDocumentPartitioning(sourceViewer);
	}

	protected IColorManager getColorManager() {
		return this.fColorManager;
	}

	/**
	 * @since 3.0
	 */
	public IPreferenceStore getPreferenceStore() {
		return fPreferenceStore;
	}

	public ITextEditor getEditor() {
		return this.fTextEditor;
	}

	@Override
	public IReconciler getReconciler(ISourceViewer sourceViewer) {
		final ITextEditor editor = getEditor();
		if (editor != null && editor.isEditable()) {
			ScriptCompositeReconcilingStrategy strategy = new ScriptCompositeReconcilingStrategy(
					editor, getConfiguredDocumentPartitioning(sourceViewer),
					createSpellCheckDelegate());
			ScriptReconciler reconciler = new ScriptReconciler(editor,
					strategy, false);
			reconciler.setIsAllowedToModifyDocument(false);
			reconciler.setIsIncrementalReconciler(false);
			reconciler.setProgressMonitor(new NullProgressMonitor());
			reconciler.setDelay(500);

			return reconciler;
		}
		return null;
	}

	public boolean affectsTextPresentation(PropertyChangeEvent event) {
		return false;
	}

	public void handlePropertyChangeEvent(PropertyChangeEvent event) {

	}

	/*
	 * @see SourceViewerConfiguration#getDefaultPrefixes(ISourceViewer,String)
	 */
	@Override
	public String[] getDefaultPrefixes(ISourceViewer sourceViewer,
			String contentType) {
		return new String[] { getCommentPrefix(), "" }; //$NON-NLS-1$
	}

	/**
	 * Returns the comment prefix.
	 * 
	 * <p>
	 * Default implementation returns a <code>#</code>, sub-classes may override
	 * if their language uses a different prefix.
	 * </p>
	 */
	protected String getCommentPrefix() {
		return "#"; //$NON-NLS-1$
	}

	/**
	 * Returns the outline presenter control creator. The creator is a factory
	 * creating outline presenter controls for the given source viewer. This
	 * implementation always returns a creator for
	 * <code>ScriptOutlineInformationControl</code> instances.
	 * 
	 * @param sourceViewer
	 *            the source viewer to be configured by this configuration
	 * @param commandId
	 *            the ID of the command that opens this control
	 * @return an information control creator
	 * 
	 */
	protected IInformationControlCreator getOutlinePresenterControlCreator(
			ISourceViewer sourceViewer, final String commandId) {
		return new IInformationControlCreator() {
			public IInformationControl createInformationControl(Shell parent) {
				int shellStyle = SWT.RESIZE;
				int treeStyle = SWT.V_SCROLL | SWT.H_SCROLL;
				return new ScriptOutlineInformationControl(parent, shellStyle,
						treeStyle, commandId, fPreferenceStore);
			}
		};
	}

	public IInformationPresenter getOutlinePresenter(
			ISourceViewer sourceViewer, boolean doCodeResolve) {
		InformationPresenter presenter;
		if (doCodeResolve)
			presenter = new InformationPresenter(
					getOutlinePresenterControlCreator(sourceViewer,
							IScriptEditorActionDefinitionIds.OPEN_STRUCTURE));
		else
			presenter = new InformationPresenter(
					getOutlinePresenterControlCreator(sourceViewer,
							IScriptEditorActionDefinitionIds.SHOW_OUTLINE));
		presenter
				.setDocumentPartitioning(getConfiguredDocumentPartitioning(sourceViewer));
		presenter.setAnchor(AbstractInformationControlManager.ANCHOR_GLOBAL);
		IInformationProvider provider = new ScriptElementProvider(getEditor(),
				doCodeResolve);
		presenter.setInformationProvider(provider,
				IDocument.DEFAULT_CONTENT_TYPE);
		initializeQuickOutlineContexts(presenter, provider);
		for (String contentType : getOutlinePresenterContentTypes(sourceViewer,
				doCodeResolve)) {
			if (IDocument.DEFAULT_CONTENT_TYPE.equals(contentType))
				continue;
			if (presenter.getInformationProvider(contentType) != null)
				continue;
			presenter.setInformationProvider(provider, contentType);
		}

		presenter.setSizeConstraints(50, 20, true, false);
		return presenter;
	}

	protected String[] getOutlinePresenterContentTypes(
			ISourceViewer sourceViewer, boolean doCodeResolve) {
		return getConfiguredContentTypes(sourceViewer);
	}

	@Deprecated
	protected void initializeQuickOutlineContexts(
			InformationPresenter presenter, IInformationProvider provider) {
	}

	public IInformationPresenter getHierarchyPresenter(
			ScriptSourceViewer viewer, boolean b) {
		return null;
	}

	protected IDialogSettings getSettings(String sectionName) {
		IDialogSettings settings = DLTKUIPlugin.getDefault()
				.getDialogSettings().getSection(sectionName);
		if (settings == null)
			settings = DLTKUIPlugin.getDefault().getDialogSettings()
					.addNewSection(sectionName);

		return settings;
	}

	@Override
	public IHyperlinkDetector[] getHyperlinkDetectors(ISourceViewer sourceViewer) {
		if (!fPreferenceStore
				.getBoolean(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_HYPERLINKS_ENABLED))
			return null;

		final IHyperlinkDetector[] inheritedDetectors = super
				.getHyperlinkDetectors(sourceViewer);

		if (fTextEditor == null) {
			return inheritedDetectors;
		}

		int resultLength = 1;
		if (inheritedDetectors != null) {
			resultLength += inheritedDetectors.length;
		}
		final IHyperlinkDetector[] additionalDetectors = getAdditionalRegisteredHyperlinkDetectors(sourceViewer);
		if (additionalDetectors != null) {
			resultLength += additionalDetectors.length;
		}
		final IHyperlinkDetector[] detectors = new IHyperlinkDetector[resultLength];
		int resultIndex = 0;
		if (inheritedDetectors != null) {
			System.arraycopy(inheritedDetectors, 0, detectors, resultIndex,
					inheritedDetectors.length);
			resultIndex += inheritedDetectors.length;
		}
		detectors[resultIndex++] = new ModelElementHyperlinkDetector(
				fTextEditor);
		if (additionalDetectors != null) {
			System.arraycopy(additionalDetectors, 0, detectors, resultIndex,
					additionalDetectors.length);
			resultIndex += additionalDetectors.length;
		}
		return detectors;
	}

	/**
	 * Returns the additional registered hyperlink detectors which are used to
	 * detect hyperlinks in the given source viewer.
	 * 
	 * @param sourceViewer
	 *            the source viewer to be configured by this configuration
	 * @return an array with hyperlink detectors or <code>null</code> if no
	 *         hyperlink detectors are registered
	 * @since 5.0
	 */
	@Nullable
	private final IHyperlinkDetector[] getAdditionalRegisteredHyperlinkDetectors(
			ISourceViewer sourceViewer) {
		final Map<String, IAdaptable> targets = getAdditionalHyperlinkDetectorTargets(sourceViewer);
		Assert.isNotNull(targets);
		if (targets.isEmpty()) {
			return null;
		}
		final HyperlinkDetectorRegistry registry = EditorsUI
				.getHyperlinkDetectorRegistry();
		List<IHyperlinkDetector> result = null;
		for (Map.Entry<String, IAdaptable> target : targets.entrySet()) {
			final IHyperlinkDetector[] detectors = registry
					.createHyperlinkDetectors(target.getKey(),
							target.getValue());
			if (detectors != null && detectors.length != 0) {
				if (result == null) {
					result = new ArrayList<IHyperlinkDetector>();
				}
				Collections.addAll(result, detectors);
			}
		}
		return result != null ? result.toArray(new IHyperlinkDetector[result
				.size()]) : null;
	}

	/**
	 * Similar to {@link #getHyperlinkDetectorTargets(ISourceViewer)}, but these
	 * detectors are always added in the end.
	 * 
	 * @since 5.0
	 */
	protected Map<String, IAdaptable> getAdditionalHyperlinkDetectorTargets(
			ISourceViewer sourceViewer) {
		return new HashMap<String, IAdaptable>();
	}

	/*
	 * @see
	 * SourceViewerConfiguration#getConfiguredTextHoverStateMasks(ISourceViewer,
	 * String)
	 */
	@Override
	public int[] getConfiguredTextHoverStateMasks(ISourceViewer sourceViewer,
			String contentType) {
		final String natureId = getNatureId();
		if (natureId == null) {
			return null;
		}
		EditorTextHoverDescriptor[] hoverDescs = DLTKUIPlugin.getDefault()
				.getEditorTextHoverDescriptors(fPreferenceStore, natureId);
		int stateMasks[] = new int[hoverDescs.length];
		int stateMasksLength = 0;
		for (int i = 0; i < hoverDescs.length; i++) {
			if (hoverDescs[i].isEnabled()) {
				int j = 0;
				int stateMask = hoverDescs[i].getStateMask();
				while (j < stateMasksLength) {
					if (stateMasks[j] == stateMask)
						break;
					j++;
				}
				if (j == stateMasksLength)
					stateMasks[stateMasksLength++] = stateMask;
			}
		}
		if (stateMasksLength == hoverDescs.length)
			return stateMasks;

		int[] shortenedStateMasks = new int[stateMasksLength];
		System.arraycopy(stateMasks, 0, shortenedStateMasks, 0,
				stateMasksLength);
		return shortenedStateMasks;
	}

	/*
	 * @see SourceViewerConfiguration#getTextHover(ISourceViewer, String, int)
	 */
	@Override
	public ITextHover getTextHover(ISourceViewer sourceViewer,
			String contentType, int stateMask) {
		final String natureId = getNatureId();
		if (natureId == null) {
			return null;
		}
		EditorTextHoverDescriptor[] hoverDescs = DLTKUIPlugin.getDefault()
				.getEditorTextHoverDescriptors(fPreferenceStore, natureId);
		int i = 0;
		while (i < hoverDescs.length) {
			if (hoverDescs[i].isEnabled()
					&& hoverDescs[i].getStateMask() == stateMask)
				return new EditorTextHoverProxy(hoverDescs[i], getEditor(),
						fPreferenceStore);
			i++;
		}

		return null;
	}

	private String getNatureId() {
		final ITextEditor editor = getEditor();
		if (editor == null || !(editor instanceof ScriptEditor)) {
			return null;
		}
		return ((ScriptEditor) editor).getLanguageToolkit().getNatureId();
	}

	@Override
	public ITextHover getTextHover(ISourceViewer sourceViewer,
			String contentType) {
		return getTextHover(sourceViewer, contentType,
				ITextViewerExtension2.DEFAULT_HOVER_STATE_MASK);
	}

	/**
	 * Returns the information presenter control creator. The creator is a
	 * factory creating the presenter controls for the given source viewer. This
	 * implementation always returns a creator for
	 * <code>DefaultInformationControl</code> instances.
	 * 
	 * @param sourceViewer
	 *            the source viewer to be configured by this configuration
	 * @return an information control creator
	 * 
	 */
	private IInformationControlCreator getInformationPresenterControlCreator(
			ISourceViewer sourceViewer) {
		return new IInformationControlCreator() {
			public IInformationControl createInformationControl(Shell parent) {
				int shellStyle = SWT.RESIZE | SWT.TOOL;
				int style = SWT.V_SCROLL | SWT.H_SCROLL;
				return new DefaultInformationControl(parent, shellStyle, style,
						new HTMLTextPresenter(false));
			}
		};
	}

	@Override
	public IInformationPresenter getInformationPresenter(
			ISourceViewer sourceViewer) {
		InformationPresenter presenter = new InformationPresenter(
				getInformationPresenterControlCreator(sourceViewer));
		presenter
				.setDocumentPartitioning(getConfiguredDocumentPartitioning(sourceViewer));
		IInformationProvider provider = new ScriptInformationProvider(
				getEditor());
		presenter.setInformationProvider(provider,
				IDocument.DEFAULT_CONTENT_TYPE);

		presenter.setSizeConstraints(60, 10, true, true);
		return presenter;
	}

	@Override
	public IContentAssistant getContentAssistant(ISourceViewer sourceViewer) {
		if (getEditor() != null) {
			ContentAssistant assistant = new ContentAssistant();

			assistant
					.setDocumentPartitioning(getConfiguredDocumentPartitioning(sourceViewer));
			assistant
					.setRestoreCompletionProposalSize(getSettings("completion_proposal_size")); //$NON-NLS-1$
			assistant
					.setContextInformationPopupOrientation(IContentAssistant.CONTEXT_INFO_ABOVE);
			assistant
					.setInformationControlCreator(getInformationControlCreator(sourceViewer));

			alterContentAssistant(assistant);

			getContentAssistPreference().configure(assistant, fPreferenceStore);

			assistant.enableColoredLabels(true);

			return assistant;
		}

		return null;
	}

	protected abstract ContentAssistPreference getContentAssistPreference();

	protected void alterContentAssistant(ContentAssistant assistant) {
		// empty implementation
	}

	public String getFontPropertyPreferenceKey() {
		return JFaceResources.TEXT_FONT;
	}

	public void changeContentAssistantConfiguration(ContentAssistant c,
			PropertyChangeEvent event) {
		getContentAssistPreference().changeConfiguration(c, fPreferenceStore,
				event);
	}

	@Override
	public IContentFormatter getContentFormatter(ISourceViewer sourceViewer) {
		final String natureId = getNatureId();
		if (ScriptFormatterManager.hasFormatterFor(natureId)) {
			final MultiPassContentFormatter formatter = new MultiPassContentFormatter(
					getConfiguredDocumentPartitioning(sourceViewer),
					IDocument.DEFAULT_CONTENT_TYPE);
			formatter.setMasterStrategy(new ScriptFormattingStrategy(natureId));
			return formatter;
		}
		return super.getContentFormatter(sourceViewer);
	}

	/*
	 * @see SourceViewerConfiguration#getIndentPrefixes(ISourceViewer, String)
	 */
	@Override
	public String[] getIndentPrefixes(ISourceViewer sourceViewer,
			String contentType) {
		if (fPreferenceStore == null) {
			return super.getIndentPrefixes(sourceViewer, contentType);
		}
		final TabStyle tabStyle = getTabStyle();
		final int tabWidth = getTabWidth(sourceViewer);
		final int indentWidth = getIndentationSize(sourceViewer);
		if (tabStyle != TabStyle.TAB && indentWidth < tabWidth) {
			return new String[] { AutoEditUtils.getNSpaces(indentWidth), "\t", //$NON-NLS-1$
					Util.EMPTY_STRING };
		} else if (tabStyle == TabStyle.TAB) {
			return getIndentPrefixesForTab(tabWidth);
		} else {
			return getIndentPrefixesForSpaces(tabWidth);
		}
	}

	protected TabStyle getTabStyle() {
		if (fPreferenceStore != null) {
			TabStyle tabStyle = TabStyle.forName(fPreferenceStore
					.getString(CodeFormatterConstants.FORMATTER_TAB_CHAR));
			if (tabStyle != null) {
				return tabStyle;
			}
		}
		return TabStyle.TAB;
	}

	@Override
	public int getTabWidth(ISourceViewer sourceViewer) {
		if (fPreferenceStore == null)
			return super.getTabWidth(sourceViewer);
		return fPreferenceStore
				.getInt(CodeFormatterConstants.FORMATTER_TAB_SIZE);
	}

	protected int getIndentationSize(ISourceViewer sourceViewer) {
		if (fPreferenceStore == null)
			return super.getTabWidth(sourceViewer);
		return fPreferenceStore
				.getInt(CodeFormatterConstants.FORMATTER_INDENTATION_SIZE);
	}

	/**
	 * Computes and returns the indent prefixes for space indentation and the
	 * given <code>tabWidth</code>.
	 * 
	 * @param tabWidth
	 *            the display tab width
	 * @return the indent prefixes
	 * @see #getIndentPrefixes(ISourceViewer, String)
	 */
	protected String[] getIndentPrefixesForSpaces(int tabWidth) {
		final String[] indentPrefixes = new String[tabWidth + 2];
		indentPrefixes[0] = AutoEditUtils.getNSpaces(tabWidth);
		for (int i = 0; i < tabWidth; i++) {
			indentPrefixes[i + 1] = AutoEditUtils.getNSpaces(i) + '\t';
		}
		indentPrefixes[tabWidth + 1] = ""; //$NON-NLS-1$
		return indentPrefixes;
	}

	/*
	 * @see
	 * SourceViewerConfiguration#getInformationControlCreator(ISourceViewer)
	 * 
	 * @since 3.0
	 */
	@Override
	public IInformationControlCreator getInformationControlCreator(
			ISourceViewer sourceViewer) {
		return new IInformationControlCreator() {
			public IInformationControl createInformationControl(Shell parent) {
				return new DefaultInformationControl(parent, false);
			}
		};
	}

	/*
	 * @see SourceViewerConfiguration#getAnnotationHover(ISourceViewer)
	 * 
	 * @since 3.0
	 */
	@Override
	public IAnnotationHover getAnnotationHover(ISourceViewer sourceViewer) {
		return new HTMLAnnotationHover(false) {
			@Override
			protected boolean isIncluded(Annotation annotation) {
				return isShowInVerticalRuler(annotation);
			}
		};
	}

	/*
	 * @see
	 * SourceViewerConfiguration#getOverviewRulerAnnotationHover(ISourceViewer)
	 * 
	 * @since 3.0
	 */
	@Override
	public IAnnotationHover getOverviewRulerAnnotationHover(
			ISourceViewer sourceViewer) {
		return new HTMLAnnotationHover(true) {
			@Override
			protected boolean isIncluded(Annotation annotation) {
				return isShowInOverviewRuler(annotation);
			}
		};
	}

	protected SpellCheckDelegate createSpellCheckDelegate() {
		return new SpellCheckDelegate();
	}

	@Override
	public IQuickAssistAssistant getQuickAssistAssistant(
			ISourceViewer sourceViewer) {
		if (getEditor() != null)
			return new ScriptCorrectionAssistant(getEditor(), fPreferenceStore,
					getColorManager());
		return super.getQuickAssistAssistant(sourceViewer);
	}
}
