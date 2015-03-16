package org.eclipse.dltk.core.index.lucene;

import java.io.IOException;
import java.util.Iterator;

import org.apache.lucene.index.Term;
import org.apache.lucene.queries.BooleanFilter;
import org.apache.lucene.queries.TermFilter;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.PrefixFilter;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.WildcardQuery;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.dltk.core.index2.search.ISearchEngine;
import org.eclipse.dltk.core.index2.search.ISearchEngineExtension;
import org.eclipse.dltk.core.index2.search.ISearchRequestor;
import org.eclipse.dltk.core.search.IDLTKSearchScope;

public class LuceneSearchEngine implements ISearchEngine,
		ISearchEngineExtension {

	@Override
	public void search(int elementType, String qualifier, String elementName,
			int trueFlags, int falseFlags, int limit, SearchFor searchFor,
			MatchRule matchRule, IDLTKSearchScope scope,
			ISearchRequestor requestor, IProgressMonitor monitor) {
		search(elementType, qualifier, elementName, null, trueFlags,
				falseFlags, limit, searchFor, matchRule, scope, requestor,
				monitor);
	}

	@Override
	public void search(int elementType, String qualifier, String elementName,
			String parent, int trueFlags, int falseFlags, int limit,
			SearchFor searchFor, MatchRule matchRule, IDLTKSearchScope scope,
			ISearchRequestor requestor, IProgressMonitor monitor) {
		boolean searchForDecls = searchFor == SearchFor.DECLARATIONS
				|| searchFor == SearchFor.ALL_OCCURRENCES;
		boolean searchForRefs = searchFor == SearchFor.REFERENCES
				|| searchFor == SearchFor.ALL_OCCURRENCES;

		if (searchForRefs) {
			doSearch(elementType, qualifier, elementName, parent, trueFlags,
					falseFlags, limit, true, searchFor, matchRule, scope,
					requestor, monitor);
		}
		if (searchForDecls) {
			doSearch(elementType, qualifier, elementName, parent, trueFlags,
					falseFlags, limit, false, searchFor, matchRule, scope,
					requestor, monitor);
		}
	}

	private void doSearch(final int elementType, String qualifier,
			String elementName, String parent, final int trueFlags,
			final int falseFlags, int limit, final boolean searchForRefs,
			SearchFor searchFor, MatchRule matchRule, IDLTKSearchScope scope,
			ISearchRequestor requestor, IProgressMonitor monitor) {
		long time = System.currentTimeMillis();
		BooleanQuery booleanQuery = new BooleanQuery();
		BooleanFilter booleanFilter = new BooleanFilter();

		if (elementName != null && !elementName.isEmpty()) {
			elementName = elementName.toLowerCase();

			Filter filter = null;
			Term nameTerm = new Term(IndexFields.ELEMENT_NAME, elementName);
			if (matchRule == MatchRule.PREFIX) {
				filter = new PrefixFilter(nameTerm);
			} else if (matchRule == MatchRule.EXACT) {
				filter = new TermFilter(nameTerm);
			} else if (matchRule == MatchRule.CAMEL_CASE) {
				Term term = new Term(IndexFields.CC_NAME, elementName);
				filter = new PrefixFilter(term);
			} else if (matchRule == MatchRule.PATTERN) {
				booleanQuery.add(new WildcardQuery(nameTerm),
						BooleanClause.Occur.MUST);
			} else {
				throw new UnsupportedOperationException();
			}
			if (filter != null) {
				booleanFilter.add(filter, Occur.MUST);
			}
		}

		booleanFilter.add(new NumberFilter(IndexFields.ELEMENT_TYPE,
				elementType), Occur.MUST);

		int occurenceType = searchForRefs ? IndexFields.TYPE_REFERENCE
				: IndexFields.TYPE_DECLARATION;
		booleanFilter.add(
				new NumberFilter(IndexFields.TYPE, occurenceType),
				Occur.MUST);

		if (trueFlags != 0 || falseFlags != 0) {
			booleanFilter.add(new BitFlagsFilter(IndexFields.FLAGS, trueFlags,
					falseFlags), Occur.MUST);
		}

		for (IPath path : scope.enclosingProjectsAndZips()) {
			String container = path.toString().toLowerCase();
			booleanQuery.add(new TermQuery(new Term(IndexFields.CONTAINER,
					container)), BooleanClause.Occur.SHOULD);
		}

		IndexSearcher indexSearcher = null;

		try {
			LuceneIndexerPlugin.getSearcherManager().maybeRefresh();
			indexSearcher = LuceneIndexerPlugin.getSearcherManager().acquire();

			// if (limit == 0) {
			// limit = Integer.MAX_VALUE;
			// }

			SearchElementHandler elementHandler = new SearchElementHandler(
					scope, requestor);
			DocumentCollector collector = new DocumentCollector();
			indexSearcher.search(booleanQuery, booleanFilter, collector);
			Iterator<DocumentEntity> iterator = collector.iterator();
			while (iterator.hasNext()) {
				DocumentEntity documentEntity = iterator.next();
				elementHandler.handle(documentEntity, searchForRefs);
			}
			System.out.println("SEARCH: elementName:" + elementName
					+ " -> elementType:" + elementType + " -> searchFor:"
					+ searchFor + " -> results:" + collector.resultSize()
					+ " TIME: " + (System.currentTimeMillis() - time));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (indexSearcher != null) {
				try {
					LuceneIndexerPlugin.getSearcherManager()
							.release(indexSearcher);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
