package org.eclipse.dltk.core.index.lucene;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.index.Term;
import org.apache.lucene.queries.BooleanFilter;
import org.apache.lucene.queries.TermFilter;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.ConstantScoreQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.LeafCollector;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.BooleanClause.Occur;
import org.eclipse.core.runtime.IPath;
import org.eclipse.dltk.core.DLTKLanguageManager;
import org.eclipse.dltk.core.IDLTKLanguageToolkit;
import org.eclipse.dltk.core.IDLTKLanguageToolkitExtension;
import org.eclipse.dltk.core.IModelElement;
import org.eclipse.dltk.core.ISourceModule;
import org.eclipse.dltk.core.environment.EnvironmentPathUtils;
import org.eclipse.dltk.core.environment.IFileHandle;
import org.eclipse.dltk.core.index.lucene.LuceneIndexManager.IndexType;
import org.eclipse.dltk.core.index2.AbstractIndexer;
import org.eclipse.dltk.core.index2.search.ISearchEngine;
import org.eclipse.dltk.internal.core.ExternalSourceModule;
import org.eclipse.dltk.internal.core.SourceModule;
import org.eclipse.dltk.internal.core.util.Util;

@SuppressWarnings("restriction")
public class LuceneIndexer2 extends AbstractIndexer {

	private String fFilename;
	private String fContainer;

	@Override
	public Map<String, Long> getDocuments(IPath container) {
		IndexSearcher indexSearcher = null;
		try {
			final Map<String, Long> result = new HashMap<String, Long>();
			indexSearcher = LuceneIndexManager.INSTANCE
					.findSearcher(container.toString(), IndexType.SOURCES_INFO).acquire();
			final Set<String> fields = new HashSet<>();
			fields.add(IndexFields.PATH);
			Collector collector = new Collector() {
				@Override
				public LeafCollector getLeafCollector(final LeafReaderContext context) throws IOException {
					final LeafReader reader = context.reader();
					final NumericDocValues timestampField = context.reader().getNumericDocValues(IndexFields.TIMESTAMP);
					return new LeafCollector() {
						@Override
						public void setScorer(Scorer arg0) throws IOException {
						}

						@Override
						public void collect(int docId) throws IOException {
							Document document = reader.document(docId, fields);
							result.put(document.get(IndexFields.PATH), timestampField.get(docId));
						}
					};
				}
			};
			BooleanFilter booleanFilter = new BooleanFilter();
			booleanFilter.add(new TimestampFilter(IndexFields.TIMESTAMP), Occur.MUST);
			indexSearcher.search(new MatchAllDocsQuery(), booleanFilter, collector);
			return result;
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (indexSearcher != null) {
				try {
					LuceneIndexManager.INSTANCE.findSearcher(container.toString(), IndexType.SOURCES_INFO)
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
		return new LuceneSearchEngine2();
	}

	@Override
	public void addDeclaration(DeclarationInfo info) {
		try {
			IndexWriter writer = LuceneIndexManager.INSTANCE.findWriter(fContainer, IndexType.DECLARATIONS);
			writer.addDocument(DocumentFactory.INSTANCE.buildDeclaration(fFilename, info));
		} catch (IOException e) {
			Logger.logException(e);
		}
	}

	@Override
	public void addReference(ReferenceInfo info) {
		try {
			IndexWriter writer = LuceneIndexManager.INSTANCE.findWriter(fContainer, IndexType.REFERENCES);
			writer.addDocument(DocumentFactory.INSTANCE.buildReference(fFilename, info));
		} catch (IOException e) {
			Logger.logException(e);
		}
	}

	@Override
	public void indexDocument(ISourceModule sourceModule) {
		final IFileHandle fileHandle = EnvironmentPathUtils.getFile(sourceModule);
		try {
			IDLTKLanguageToolkit toolkit = DLTKLanguageManager.getLanguageToolkit(sourceModule);
			if (toolkit == null) {
				return;
			}
			IPath containerPath;
			if (sourceModule instanceof SourceModule) {
				containerPath = sourceModule.getScriptProject().getPath();
			} else {
				containerPath = sourceModule.getAncestor(IModelElement.PROJECT_FRAGMENT).getPath();
			}
			String relativePath;
			if (toolkit instanceof IDLTKLanguageToolkitExtension
					&& ((IDLTKLanguageToolkitExtension) toolkit).isArchiveFileName(sourceModule.getPath().toString())) {
				relativePath = ((ExternalSourceModule) sourceModule).getFullPath().toString();
			} else {
				relativePath = Util.relativePath(sourceModule.getPath(), containerPath.segmentCount());
			}
			long lastModified = fileHandle == null ? 0 : fileHandle.lastModified();
			this.fContainer = containerPath.toString();
			this.fFilename = relativePath;
			// Cleanup and write new info...
			cleanupDocument(fContainer, fFilename);
			IndexWriter indexWriter = LuceneIndexManager.INSTANCE.findWriter(fContainer, IndexType.SOURCES_INFO);
			indexWriter.addDocument(DocumentFactory.INSTANCE.buildSourceInfo(fFilename, lastModified));
			super.indexDocument(sourceModule);
		} catch (Exception e) {
			Logger.logException(e);
		}
	}

	@Override
	public void removeContainer(IPath containerPath) {
		cleanupContainer(containerPath.toString());
	}

	@Override
	public void removeDocument(IPath containerPath, String relativePath) {
		cleanupDocument(containerPath.toString(), relativePath);
	}

	private void cleanupContainer(String container) {
		LuceneIndexManager.INSTANCE.cleanup(container);
	}

	private void cleanupDocument(String container, String source) {
		BooleanFilter filter = new BooleanFilter();
		filter.add(new TermFilter(new Term(IndexFields.PATH, source)), Occur.MUST);
		Query query = new ConstantScoreQuery(filter);
		for (IndexType type : IndexType.values()) {
			IndexWriter writer = LuceneIndexManager.INSTANCE.findWriter(container, type);
			try {
				writer.deleteDocuments(query);
			} catch (IOException e) {
				Logger.logException(e);
			}
		}
	}

}
