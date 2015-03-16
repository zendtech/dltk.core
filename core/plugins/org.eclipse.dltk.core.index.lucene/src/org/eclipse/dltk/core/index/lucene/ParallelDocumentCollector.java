package org.eclipse.dltk.core.index.lucene;
//package org.eclipse.php.indexer;
//
//import static org.eclipse.php.indexer.IndexFields.*;
//
//import java.io.IOException;
//import java.util.Arrays;
//import java.util.HashMap;
//import java.util.HashSet;
//import java.util.Iterator;
//import java.util.Map;
//import java.util.Set;
//import java.util.concurrent.Callable;
//import java.util.concurrent.ExecutionException;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Future;
//
//import org.apache.lucene.document.Document;
//import org.apache.lucene.index.LeafReader;
//import org.apache.lucene.index.LeafReaderContext;
//import org.apache.lucene.index.NumericDocValues;
//import org.apache.lucene.search.LeafCollector;
//import org.apache.lucene.search.Scorer;
//
//public class ParallelDocumentCollector implements CollectionCollector {
//
//	private static final String[] NUM_FIELDS = new String[] { ELEMENT_TYPE,
//			OFFSET, LENGTH, FLAGS, NAME_OFFSET };
//
//	private static final String[] STRING_FIELDS = new String[] { PATH,
//			CONTAINER, ELEMENT_NAME, QUALIFIER, PARENT, METADATA, DOC };
//
//	private class IteratorWrapper implements Iterator<DocumentEntity> {
//
//		private Iterator<Future<DocumentEntity>> iterator;
//
//		public IteratorWrapper(Set<Future<DocumentEntity>> result) {
//			this.iterator = result.iterator();
//		}
//
//		@Override
//		public boolean hasNext() {
//			return iterator.hasNext();
//		}
//
//		@Override
//		public DocumentEntity next() {
//			try {
//				return iterator.next().get();
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			} catch (ExecutionException e) {
//				e.printStackTrace();
//			}
//			return null;
//		}
//
//		@Override
//		public void remove() {
//			iterator.remove();
//		}
//
//	}
//
//	private class ReadDocumentTask implements Callable<DocumentEntity> {
//
//		private int docId;
//		private LeafReader reader;
//		private Map<String, NumericDocValues> docValuesMap;
//
//		public ReadDocumentTask(int docId, LeafReader leafReader,
//				Map<String, NumericDocValues> docValuesMap) {
//			this.docId = docId;
//			this.reader = leafReader;
//			this.docValuesMap = docValuesMap;
//		}
//
//		@Override
//		public DocumentEntity call() throws Exception {
//			DocumentEntity documentEntity = new DocumentEntity();
//			documentEntity.setElementType(get(ELEMENT_TYPE, docId));
//			documentEntity.setOffset(get(OFFSET, docId));
//			documentEntity.setLength(get(LENGTH, docId));
//			documentEntity.setFlags(get(FLAGS, docId));
//			documentEntity.setNameOffset(get(NAME_OFFSET, docId));
//
//			Document document = reader.document(docId, fields);
//
//			documentEntity.setPath(document.get(PATH));
//			documentEntity.setContainer(document.get(CONTAINER));
//			documentEntity.setElementName(document.get(ELEMENT_NAME));
//
//			documentEntity.setQualifier(document.get(QUALIFIER));
//			documentEntity.setParent(document.get(PARENT));
//			documentEntity.setDoc(document.get(DOC));
//			documentEntity.setMetadata(document.get(METADATA));
//			return documentEntity;
//		}
//
//		private int get(String field, int docId) {
//			return (int) docValuesMap.get(field).get(docId);
//		}
//
//	}
//
//	private Set<String> fields;
//	private Map<String, NumericDocValues> docValuesMap;
//	private ExecutorService executorService;
//	private Set<Future<DocumentEntity>> result = new HashSet<>();
//
//	public ParallelDocumentCollector(ExecutorService executorService) {
//		this.executorService = executorService;
//		this.fields = new HashSet<>(Arrays.asList(STRING_FIELDS));
//	}
//
//	@Override
//	public LeafCollector getLeafCollector(final LeafReaderContext context)
//			throws IOException {
//		final LeafReader reader = context.reader();
//		docValuesMap = new HashMap<String, NumericDocValues>();
//		for (String field : NUM_FIELDS) {
//			NumericDocValues docValues = context.reader().getNumericDocValues(
//					field);
//			if (docValues != null) {
//				docValuesMap.put(field, docValues);
//			}
//		}
//		return new LeafCollector() {
//
//			@Override
//			public void setScorer(Scorer arg0) throws IOException {
//			}
//
//			@Override
//			public void collect(int docId) throws IOException {
//				ReadDocumentTask documentTask = new ReadDocumentTask(docId,
//						reader, docValuesMap);
//				result.add(executorService.submit(documentTask));
//			}
//		};
//
//	}
//
//	@Override
//	public int resultSize() {
//		return result.size();
//	}
//
//	@Override
//	public Iterator<DocumentEntity> iterator() {
//		return new IteratorWrapper(result);
//	}
//
// }