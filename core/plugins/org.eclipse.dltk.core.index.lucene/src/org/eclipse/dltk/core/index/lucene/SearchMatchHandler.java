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

@SuppressWarnings("restriction")
public class SearchMatchHandler {

	private class FilePathHandler {

		private IPath fFolderPath = Path.EMPTY;
		private String fFileName;

		public FilePathHandler(String filePath) {
			this.fFileName = filePath;
			int i = filePath.lastIndexOf('/');
			if (i == -1) {
				i = filePath.lastIndexOf('\\');
			}
			if (i != -1) {
				this.fFolderPath = new Path(filePath.substring(0, i));
				this.fFileName = filePath.substring(i + 1);
			}
		}

		public IPath getFolderPath() {
			return fFolderPath;
		}

		public String getFileName() {
			return fFileName;
		}
	}

	private Map<String, IProjectFragment> fProjectFragmentCache = new HashMap<String, IProjectFragment>();
	private Map<String, ISourceModule> fSourceModuleCache = new HashMap<String, ISourceModule>();
	private ISearchRequestor fSearchRequestor;
	private IDLTKSearchScope fScope;

	public SearchMatchHandler(IDLTKSearchScope scope, ISearchRequestor searchRequestor) {
		this.fScope = scope;
		this.fSearchRequestor = searchRequestor;
	}

	public void handle(SearchMatch match, boolean isReference) {
		String containerPath = match.getContainer();
		IDLTKLanguageToolkit toolkit = ((DLTKSearchScope) fScope).getLanguageToolkit();
		if (toolkit instanceof IDLTKLanguageToolkitExtension
				&& ((IDLTKLanguageToolkitExtension) toolkit).isArchiveFileName(containerPath)) {
			containerPath = containerPath + IDLTKSearchScope.FILE_ENTRY_SEPARATOR;
		}
		if (containerPath.length() != 0 && containerPath.charAt(containerPath.length() - 1) != IPath.SEPARATOR) {
			containerPath = containerPath + IPath.SEPARATOR;
		}
		String filePath = match.getPath();
		final String resourcePath = containerPath + filePath;
		IProjectFragment projectFragment = fProjectFragmentCache.get(containerPath);
		if (projectFragment == null) {
			projectFragment = ((DLTKSearchScope) fScope).projectFragment(resourcePath);
			if (projectFragment == null) {
				projectFragment = ((DLTKSearchScope) fScope).projectFragment(containerPath);
			}
			fProjectFragmentCache.put(containerPath, projectFragment);
		}
		if (projectFragment == null) {
			return;
		}
		if (!fScope.encloses(resourcePath)) {
			return;
		}
		ISourceModule sourceModule = fSourceModuleCache.get(resourcePath);
		if (sourceModule == null) {
			if (projectFragment.isArchive()) {
				FilePathHandler filePathHandler = new FilePathHandler(filePath);
				IScriptFolder scriptFolder = new ArchiveFolder((ProjectFragment) projectFragment,
						filePathHandler.getFolderPath());
				sourceModule = scriptFolder.getSourceModule(filePathHandler.getFileName());
			} else if (projectFragment.isExternal()) {
				FilePathHandler filePathHandler = new FilePathHandler(filePath);
				IScriptFolder scriptFolder = new ExternalScriptFolder((ProjectFragment) projectFragment,
						filePathHandler.getFolderPath());
				sourceModule = scriptFolder.getSourceModule(filePathHandler.getFileName());
			} else if (projectFragment.isBuiltin()) {
				FilePathHandler filePathHandler = new FilePathHandler(filePath);
				IScriptFolder scriptFolder = new BuiltinScriptFolder((ProjectFragment) projectFragment,
						filePathHandler.getFolderPath());
				sourceModule = scriptFolder.getSourceModule(filePathHandler.getFileName());
			} else {
				IProject project = projectFragment.getScriptProject().getProject();
				sourceModule = DLTKCore.createSourceModuleFrom(project.getFile(filePath));
			}
			fSourceModuleCache.put(resourcePath, sourceModule);
		}
		String name = match.getElementName();
		if (name == null) {
			return;
		}
		ModelManager modelManager = ModelManager.getModelManager();
		name = modelManager.intern(name);
		String metadata = match.getMetadata();
		String doc = match.getDoc();
		String qualifier = match.getQualifier();
		String parent = match.getParent();
		// Pass to requestor
		fSearchRequestor.match(match.getElementType(), match.getFlags(), match.getOffset(),
				match.getLength(), match.getNameOffset(), match.getNameLength(), name, metadata, doc,
				qualifier, parent, sourceModule, isReference);

	}

}