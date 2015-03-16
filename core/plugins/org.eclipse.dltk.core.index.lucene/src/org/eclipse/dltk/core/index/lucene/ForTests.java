package org.eclipse.dltk.core.index.lucene;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Iterator;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.queries.BooleanFilter;
import org.apache.lucene.queries.TermFilter;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.FilterCachingPolicy;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.LRUFilterCache;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.SimpleCollector;
import org.apache.lucene.search.UsageTrackingFilterCachingPolicy;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

public class ForTests {

	public static void main(String[] args) throws IOException {

		Directory indexDir = FSDirectory
				.open(Paths
						.get("/home/wywrzal/development/ZEND/eclipse-pdt/runtime-PDT/.metadata/.plugins/org.eclipse.php.indexer/index"));
		// CharArraySet.EMPTY_SET
		IndexWriterConfig config = new IndexWriterConfig(new DefaultAnalyzer());

		IndexWriter indexWriter = new IndexWriter(indexDir, config);
		final IndexReader indexReader = DirectoryReader.open(indexWriter, true);
		System.out.println("All: " + indexWriter.numDocs());

		IndexSearcher indexSearcher = new IndexSearcher(indexReader);

		LRUFilterCache filterCache = new LRUFilterCache(256, 50 * 1024L * 1024L);
		FilterCachingPolicy defaultCachingPolicy = new UsageTrackingFilterCachingPolicy();

		// long time = System.currentTimeMillis();
		CollectionCollector collector = null;
		Iterator<DocumentEntity> iterator = null;

		// ExecutorService executorService = Executors
		// .newFixedThreadPool(8 - 1);
		//
		// collector = new ParallelDocumentCollector(
		// executorService);
		// indexSearcher.search(new MatchAllDocsQuery(), booleanFilter,
		// collector);
		//
		// Iterator<DocumentEntity> iterator = collector.iterator();
		// while (iterator.hasNext()) {
		// iterator.next();
		// }
		// System.out.println(collector.resultSize());
		//
		// System.out.println("TIME: "
		// + (System.currentTimeMillis() - time));
		// executorService.shutdown();

		// indexSearcher.search(new MatchAllDocsQuery(), f2,
		// new DocumentCollector());

		// time = System.currentTimeMillis();
		// BooleanQuery booleanQuery = new BooleanQuery();
		// QueryBuilder qb = new QueryBuilder(new DefaultAnalyzer());
		// for (IPath path : scope.enclosingProjectsAndZips()) {
		BooleanFilter pathFilter = new BooleanFilter();
		for (String path : new String[] { "/magento2", "/zf2" }) {
			String container = path.toLowerCase();
			pathFilter.add(new TermFilter(new Term(IndexFields.CONTAINER,
					container)), Occur.SHOULD);
		}

		for (int i = 0; i < 10; i++) {
			long time = System.currentTimeMillis();
			BooleanFilter booleanFilter = new BooleanFilter();
			booleanFilter.add(pathFilter, Occur.MUST);
			// booleanFilter.add(new TermFilter(new Term(IndexFields.CONTAINER,
			// "/magento2")), Occur.MUST);

			Filter f1 = new NumberFilter(IndexFields.ELEMENT_TYPE, 7);
			booleanFilter.add(f1, Occur.MUST);

			Filter f2 = new NumberFilter(IndexFields.TYPE, 2);
			booleanFilter.add(f2, Occur.MUST);

			collector = new DocumentCollector();
			SimpleCollector simpleCollector = new SimpleCollector() {

				@Override
				public void collect(int arg0) throws IOException {
				}
			};
			// f2 = filterCache.doCache(f2, defaultCachingPolicy);
			indexSearcher.search(new MatchAllDocsQuery(), booleanFilter,
					collector);
			iterator = collector.iterator();

			while (iterator.hasNext()) {
				iterator.next().getContainer();
			}
			System.out.println("TIME: " + (System.currentTimeMillis() - time));
			indexWriter.close();
		}
		System.out.println(collector.resultSize());

	}
}
