package org.eclipse.dltk.core.index.lucene;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.index.Term;
import org.apache.lucene.queries.BooleanFilter;
import org.apache.lucene.queries.TermFilter;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.FilteredQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.LeafCollector;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.util.QueryBuilder;
import org.eclipse.core.runtime.IPath;
import org.eclipse.dltk.core.DLTKLanguageManager;
import org.eclipse.dltk.core.IDLTKLanguageToolkit;
import org.eclipse.dltk.core.IDLTKLanguageToolkitExtension;
import org.eclipse.dltk.core.IModelElement;
import org.eclipse.dltk.core.ISourceModule;
import org.eclipse.dltk.core.environment.EnvironmentPathUtils;
import org.eclipse.dltk.core.environment.IFileHandle;
import org.eclipse.dltk.core.index2.AbstractIndexer;
import org.eclipse.dltk.core.index2.search.ISearchEngine;
import org.eclipse.dltk.internal.core.ExternalSourceModule;
import org.eclipse.dltk.internal.core.SourceModule;
import org.eclipse.dltk.internal.core.util.Util;

public class LuceneIndexer extends AbstractIndexer {

	private String filename;
	private String container;

	private IndexWriter indexWriter;

	@Override
	public void addDeclaration(DeclarationInfo info) {
		DocumentWrapper document = new DocumentWrapper();
		document.setPath(filename);
		document.setContainer(container);
		document.setType(IndexFields.TYPE_DECLARATION);

		document.setElementName(info.elementName);
		document.setElementType(info.elementType);

		document.setOffset(info.offset);
		document.setLength(info.length);

		document.setMetadata(info.metadata);
		document.setQualifier(info.qualifier);
		document.setDoc(info.doc);
		document.setParent(info.parent);

		document.setFlags(info.flags);
		document.setNameOffset(info.nameOffset);
		document.setNameLength(info.nameLength);
		document.setCCName(info.elementName);

		try {
			indexWriter.addDocument(document.getDocument());
			// System.out.println(document.getDocument());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void addReference(ReferenceInfo info) {
		DocumentWrapper document = new DocumentWrapper();
		document.setPath(filename);
		document.setContainer(container);
		document.setType(IndexFields.TYPE_REFERENCE);

		document.setElementName(info.elementName);
		document.setElementType(info.elementType);

		document.setOffset(info.offset);
		document.setLength(info.length);

		document.setMetadata(info.metadata);
		document.setQualifier(info.qualifier);

		try {
			indexWriter.addDocument(document.getDocument());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void indexDocument(ISourceModule sourceModule) {
		indexWriter = LuceneIndexerPlugin.getIndexWriter();
		if (indexWriter == null) {
			return;
		}
		final IFileHandle fileHandle = EnvironmentPathUtils
				.getFile(sourceModule);
		IndexSearcher indexSearcher = null;
		try {
			IDLTKLanguageToolkit toolkit = DLTKLanguageManager
					.getLanguageToolkit(sourceModule);
			if (toolkit == null) {
				return;
			}

			IPath containerPath;
			if (sourceModule instanceof SourceModule) {
				containerPath = sourceModule.getScriptProject().getPath();
			} else {
				containerPath = sourceModule.getAncestor(
						IModelElement.PROJECT_FRAGMENT).getPath();
			}

			String relativePath;
			if (toolkit instanceof IDLTKLanguageToolkitExtension
					&& ((IDLTKLanguageToolkitExtension) toolkit)
							.isArchiveFileName(sourceModule.getPath()
									.toString())) {
				relativePath = ((ExternalSourceModule) sourceModule)
						.getFullPath().toString();
			} else {
				relativePath = Util.relativePath(sourceModule.getPath(),
						containerPath.segmentCount());
			}

			long lastModified = fileHandle == null ? 0 : fileHandle
					.lastModified();

			this.container = containerPath.toString();
			this.filename = relativePath;

			indexSearcher = LuceneIndexerPlugin.getSearcherManager().acquire();

			BooleanFilter booleanFilter = new BooleanFilter();
			booleanFilter.add(new TermFilter(new Term(IndexFields.PATH,
					filename)), Occur.MUST);
			booleanFilter.add(new TermFilter(new Term(IndexFields.CONTAINER,
					container)), Occur.MUST);
			booleanFilter.add(new TimestampFilter(IndexFields.TIMESTAMP),
					Occur.MUST);

			final AtomicLong timestamp = new AtomicLong(0);
			Collector collector = new Collector() {

				@Override
				public LeafCollector getLeafCollector(
						final LeafReaderContext context) throws IOException {
					final NumericDocValues timestampField = context.reader()
							.getNumericDocValues(IndexFields.TIMESTAMP);
					return new LeafCollector() {
						// boolean found = false;
						@Override
						public void setScorer(Scorer arg0) throws IOException {
						}

						@Override
						public void collect(int docId) throws IOException {
							timestamp.set(timestampField.get(docId));
						}
					};
				}
			};
			indexSearcher.search(new MatchAllDocsQuery(), booleanFilter,
					collector);
			if (timestamp.get() != 0) {
				if (timestamp.get() == lastModified) {
					// File is not updated - nothing to do
					return;
				}
				// Re-index:
				Query query = new FilteredQuery(new MatchAllDocsQuery(),
						booleanFilter);
				indexWriter.deleteDocuments(query);
				// indexWriter.commit();
			}

			DocumentWrapper document = new DocumentWrapper();
			document.setPath(relativePath);
			document.setContainer(container);
			document.setTimestamp(lastModified);

			indexWriter.addDocument(document.getDocument());
			super.indexDocument(sourceModule);
		} catch (Exception e) {
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

	@Override
	public void removeContainer(IPath containerPath) {
		IndexWriter indexWriter = LuceneIndexerPlugin.getIndexWriter();
		if (indexWriter == null) {
			return;
		}
		try {
			System.out.println("All before docs: " + indexWriter.numDocs());

			QueryBuilder qb = new QueryBuilder(new DefaultAnalyzer());
			String container = containerPath.toString().toLowerCase();
			Query query = qb.createBooleanQuery(IndexFields.CONTAINER,
					container);
			indexWriter.deleteDocuments(query);
			indexWriter.commit();

			System.out.println("All after docs: " + indexWriter.numDocs());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void removeDocument(IPath containerPath, String relativePath) {
		IndexWriter indexWriter = LuceneIndexerPlugin.getIndexWriter();
		if (indexWriter == null) {
			return;
		}
		try {
			// System.out.println("All before ONE doc: "
			// + indexWriter.numDocs());

			QueryBuilder qb = new QueryBuilder(new DefaultAnalyzer());
			BooleanQuery query = new BooleanQuery();
			query.add(qb.createBooleanQuery(IndexFields.CONTAINER,
					containerPath.toString().toLowerCase()),
					BooleanClause.Occur.MUST);
			query.add(
					qb.createBooleanQuery(IndexFields.PATH,
							relativePath.toLowerCase()),
					BooleanClause.Occur.MUST);

			indexWriter.deleteDocuments(query);
			indexWriter.commit();
			//
			// System.out.println("All after ONE doc: "
			// + indexWriter.numDocs());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public Map<String, Long> getDocuments(IPath containerPath) {
		IndexSearcher indexSearcher = null;
		try {
			final Map<String, Long> result = new HashMap<String, Long>();

			LuceneIndexerPlugin.getSearcherManager().maybeRefresh();
			indexSearcher = LuceneIndexerPlugin.getSearcherManager().acquire();
			QueryBuilder qb = new QueryBuilder(new DefaultAnalyzer());
			final Set<String> fields = new HashSet<>();
			fields.add(IndexFields.PATH);

			String container = containerPath.toString().toLowerCase();

			Collector collector = new Collector() {

				@Override
				public LeafCollector getLeafCollector(
						final LeafReaderContext context) throws IOException {
					final LeafReader reader = context.reader();
					final NumericDocValues timestampField = context.reader()
							.getNumericDocValues(IndexFields.TIMESTAMP);

					return new LeafCollector() {

						@Override
						public void setScorer(Scorer arg0) throws IOException {
						}

						@Override
						public void collect(int docId) throws IOException {
							Document document = reader.document(docId, fields);
							result.put(document.get(IndexFields.PATH),
									timestampField.get(docId));
						}
					};
				}
			};

			indexSearcher.search(
					qb.createBooleanQuery(IndexFields.CONTAINER, container),
					new TimestampFilter(IndexFields.TIMESTAMP), collector);
			return result;
		} catch (IOException e) {
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

		return Collections.emptyMap();
	}

	@Override
	public ISearchEngine createSearchEngine() {
		return new LuceneSearchEngine();
	}

}
