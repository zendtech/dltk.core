package org.eclipse.dltk.core.index.lucene;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.dltk.core.DLTKCore;
import org.eclipse.dltk.core.IDLTKLanguageToolkit;
import org.eclipse.dltk.core.IDLTKLanguageToolkitExtension;
import org.eclipse.dltk.core.IProjectFragment;
import org.eclipse.dltk.core.IScriptFolder;
import org.eclipse.dltk.core.ISourceModule;
import org.eclipse.dltk.core.index2.search.ISearchRequestor;
import org.eclipse.dltk.core.search.IDLTKSearchScope;
import org.eclipse.dltk.internal.core.ArchiveFolder;
import org.eclipse.dltk.internal.core.BuiltinScriptFolder;
import org.eclipse.dltk.internal.core.ExternalScriptFolder;
import org.eclipse.dltk.internal.core.ModelManager;
import org.eclipse.dltk.internal.core.ProjectFragment;
import org.eclipse.dltk.internal.core.search.DLTKSearchScope;

public class SearchElementHandler {

	private static final String EMPTY = ""; //$NON-NLS-1$
	private Map<String, IProjectFragment> projectFragmentCache = new HashMap<String, IProjectFragment>();
	private Map<String, ISourceModule> sourceModuleCache = new HashMap<String, ISourceModule>();
	private ISearchRequestor searchRequestor;
	private IDLTKSearchScope scope;

	public SearchElementHandler(IDLTKSearchScope scope,
			ISearchRequestor searchRequestor) {
		this.scope = scope;
		this.searchRequestor = searchRequestor;
	}

	public void handle(DocumentEntity document, boolean isReference) {
		String containerPath = document.getContainer();
		IDLTKLanguageToolkit toolkit = ((DLTKSearchScope) scope)
				.getLanguageToolkit();
		if (toolkit instanceof IDLTKLanguageToolkitExtension
				&& ((IDLTKLanguageToolkitExtension) toolkit)
						.isArchiveFileName(containerPath)) {
			containerPath = containerPath
					+ IDLTKSearchScope.FILE_ENTRY_SEPARATOR;
		}
		if (containerPath.length() != 0
				&& containerPath.charAt(containerPath.length() - 1) != IPath.SEPARATOR) {
			containerPath = containerPath + IPath.SEPARATOR;
		}

		String filePath = document.getPath();
		final String resourcePath = containerPath + filePath;

		IProjectFragment projectFragment = projectFragmentCache
				.get(containerPath);

		if (projectFragment == null) {
			projectFragment = ((DLTKSearchScope) scope)
					.projectFragment(resourcePath);
			if (projectFragment == null) {
				projectFragment = ((DLTKSearchScope) scope)
						.projectFragment(containerPath);
			}
			projectFragmentCache.put(containerPath, projectFragment);
		}
		if (projectFragment == null) {
			return;
		}

		if (!scope.encloses(resourcePath)) {
			return;
		}
		ISourceModule sourceModule = sourceModuleCache.get(resourcePath);
		if (sourceModule == null) {
			String folderPath = EMPTY;
			String fileName = filePath;
			int i = filePath.lastIndexOf('/');
			if (i == -1) {
				i = filePath.lastIndexOf('\\');
			}
			if (i != -1) {
				folderPath = filePath.substring(0, i);
				fileName = filePath.substring(i + 1);
			}
			if (projectFragment.isExternal()) {
				IScriptFolder scriptFolder = new ExternalScriptFolder(
						(ProjectFragment) projectFragment, new Path(folderPath));
				sourceModule = scriptFolder.getSourceModule(fileName);
			} else if (projectFragment.isArchive()) {
				IScriptFolder scriptFolder = new ArchiveFolder(
						(ProjectFragment) projectFragment, new Path(folderPath));
				sourceModule = scriptFolder.getSourceModule(fileName);
			} else if (projectFragment.isBuiltin()) {
				IScriptFolder scriptFolder = new BuiltinScriptFolder(
						(ProjectFragment) projectFragment, new Path(folderPath));
				sourceModule = scriptFolder.getSourceModule(fileName);
			} else {
				IProject project = projectFragment.getScriptProject()
						.getProject();
				sourceModule = DLTKCore.createSourceModuleFrom(project
						.getFile(filePath));
			}
			sourceModuleCache.put(resourcePath, sourceModule);
		}

		String name = document.getElementName();
		if (name == null) {
			return;
		}
		ModelManager modelManager = ModelManager.getModelManager();
		name = modelManager.intern(name);
		String metadata = document.getMetadata();
		String docz = document.getDoc();
		String qualifierz = document.getQualifier();
		String parentz = document.getParent();

		searchRequestor.match(document.getElementType(), document.getFlags(),
				document.getOffset(), document.getLength(),
				document.getNameOffset(), document.getNameLength(), name,
				metadata, docz, qualifierz, parentz, sourceModule, isReference);

	}

}