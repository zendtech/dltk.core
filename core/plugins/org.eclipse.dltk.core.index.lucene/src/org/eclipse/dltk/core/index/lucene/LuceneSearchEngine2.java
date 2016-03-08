package org.eclipse.dltk.core.index.lucene;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.lucene.index.Term;
import org.apache.lucene.queries.BooleanFilter;
import org.apache.lucene.queries.TermFilter;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.DocValuesRangeFilter;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.PrefixFilter;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.QueryWrapperFilter;
import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.search.WildcardQuery;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.dltk.core.ScriptModelUtil;
import org.eclipse.dltk.core.index.lucene.LuceneIndexManager.IndexType;
import org.eclipse.dltk.core.index2.search.ISearchEngine;
import org.eclipse.dltk.core.index2.search.ISearchEngineExtension;
import org.eclipse.dltk.core.index2.search.ISearchRequestor;
import org.eclipse.dltk.core.search.IDLTKSearchScope;
import org.eclipse.dltk.internal.core.search.DLTKSearchScope;

@SuppressWarnings("restriction")
public class LuceneSearchEngine2 implements ISearchEngine, ISearchEngineExtension {

	private static final class SearchScope {

		static List<String> getContainers(IDLTKSearchScope scope) {
			List<String> containers = new ArrayList<String>();
			for (IPath path : scope.enclosingProjectsAndZips()) {
				containers.add(path.toString());
			}
			return containers;
		}

		static List<String> getScripts(IDLTKSearchScope scope) {
			List<String> scripts = new ArrayList<String>();
			if (scope instanceof DLTKSearchScope) {
				String[] relativePaths = ((DLTKSearchScope) scope).getRelativePaths();
				String[] fileExtensions = ScriptModelUtil.getFileExtensions(scope.getLanguageToolkit());
				for (String relativePath : relativePaths) {
					if (relativePath.length() > 0) {
						if (fileExtensions != null) {
							boolean isScriptFile = false;
							for (String ext : fileExtensions) {
								if (relativePath.endsWith("." + ext)) { //$NON-NLS-1$
									isScriptFile = true;
									break;
								}
							}
							if (!isScriptFile) {
								break;
							}
						}
						scripts.add(relativePath);
					}
				}
			}
			return scripts;
		}

	}

	@Override
	public void search(int elementType, String qualifier, String elementName, int trueFlags, int falseFlags, int limit,
			SearchFor searchFor, MatchRule matchRule, IDLTKSearchScope scope, ISearchRequestor requestor,
			IProgressMonitor monitor) {
		search(elementType, qualifier, elementName, null, trueFlags, falseFlags, limit, searchFor, matchRule, scope,
				requestor, monitor);
	}

	@Override
	public void search(int elementType, String qualifier, String elementName, String parent, int trueFlags,
			int falseFlags, int limit, SearchFor searchFor, MatchRule matchRule, IDLTKSearchScope scope,
			ISearchRequestor requestor, IProgressMonitor monitor) {
		boolean searchForDecls = searchFor == SearchFor.DECLARATIONS || searchFor == SearchFor.ALL_OCCURRENCES;
		boolean searchForRefs = searchFor == SearchFor.REFERENCES || searchFor == SearchFor.ALL_OCCURRENCES;

		if (searchForRefs) {
			doSearch(elementType, qualifier, elementName, parent, trueFlags, falseFlags, limit, true, matchRule, scope,
					requestor, monitor);
		}
		if (searchForDecls) {
			doSearch(elementType, qualifier, elementName, parent, trueFlags, falseFlags, limit, false, matchRule, scope,
					requestor, monitor);
		}
	}

	private BooleanFilter createFilter(final int elementType, String qualifier, String elementName, String parent,
			final int trueFlags, final int falseFlags, final boolean searchForRefs, MatchRule matchRule,
			IDLTKSearchScope scope) {
		BooleanFilter filter = new BooleanFilter();
		if (elementName != null && !elementName.isEmpty()) {
			String elementNameLC = elementName.toLowerCase();
			Filter nameFilter = null;
			Term nameCaseInsensitiveTerm = new Term(IndexFields.ELEMENT_NAME_LC, elementNameLC);
			if (matchRule == MatchRule.PREFIX) {
				nameFilter = new PrefixFilter(nameCaseInsensitiveTerm);
			} else if (matchRule == MatchRule.EXACT) {
				nameFilter = new TermFilter(nameCaseInsensitiveTerm);
			} else if (matchRule == MatchRule.CAMEL_CASE) {
				Term term = new Term(IndexFields.CC_NAME, elementName);
				nameFilter = new PrefixFilter(term);
			} else if (matchRule == MatchRule.PATTERN) {
				nameFilter = new QueryWrapperFilter(new WildcardQuery(nameCaseInsensitiveTerm));
			} else {
				throw new UnsupportedOperationException();
			}
			if (nameFilter != null) {
				filter.add(nameFilter, Occur.MUST);
			}
		}
		if (qualifier != null && !qualifier.isEmpty()) {
			filter.add(new TermFilter(new Term(IndexFields.QUALIFIER, qualifier)), Occur.MUST);
		}
		if (parent != null && !parent.isEmpty()) {
			filter.add(new TermFilter(new Term(IndexFields.PARENT, parent)), Occur.MUST);
		}

		filter.add(DocValuesRangeFilter.newLongRange(IndexFields.ELEMENT_TYPE, Long.valueOf(elementType),
				Long.valueOf(elementType), true, true), Occur.MUST);
		if (trueFlags != 0 || falseFlags != 0) {
			filter.add(new BitFlagsFilter(IndexFields.FLAGS, trueFlags, falseFlags), Occur.MUST);
		}
		List<String> scripts = SearchScope.getScripts(scope);
		if (!scripts.isEmpty()) {
			BooleanFilter scriptFilter = new BooleanFilter();
			for (String script : scripts) {
				scriptFilter.add(new TermFilter(new Term(IndexFields.PATH, script)), Occur.MUST);
			}
			filter.add(scriptFilter, Occur.MUST);
		}
		return filter;
	}

	private void doSearch(final int elementType, String qualifier, String elementName, String parent,
			final int trueFlags, final int falseFlags, int limit, final boolean searchForRefs, MatchRule matchRule,
			IDLTKSearchScope scope, ISearchRequestor requestor, IProgressMonitor monitor) {
		BooleanFilter filter = createFilter(elementType, qualifier, elementName, parent, trueFlags, falseFlags,
				searchForRefs, matchRule, scope);
		IndexSearcher indexSearcher = null;
		Query query = new MatchAllDocsQuery();
		final SearchElementHandler entityHandler = new SearchElementHandler(scope, requestor);
		List<DocumentEntity> results = new ArrayList<>();
		for (String container : SearchScope.getContainers(scope)) {
			// Use index searcher for given container and index type
			SearcherManager searcherManager = LuceneIndexManager.INSTANCE.findSearcher(container,
					searchForRefs ? IndexType.REFERENCES : IndexType.DECLARATIONS);
			try {
				searcherManager.maybeRefresh();
				indexSearcher = searcherManager.acquire();
				indexSearcher.search(query, filter, new DocumentCollector2(container, results));
			} catch (IOException e) {
				Logger.logException(e);
			} finally {
				if (indexSearcher != null) {
					try {
						searcherManager.release(indexSearcher);
					} catch (IOException e) {
						Logger.logException(e);
					}
				}
			}
		}
		// Sort final results by element name
		Collections.sort(results, new Comparator<DocumentEntity>() {
			@Override
			public int compare(DocumentEntity e1, DocumentEntity e2) {
				return e1.getElementName().compareToIgnoreCase(e2.getElementName());
			}
		});

		// Pass results to entity handler
		for (DocumentEntity result : results) {
			entityHandler.handle(result, searchForRefs);
		}
	}

}
