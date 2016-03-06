package org.eclipse.dltk.core.index.lucene;

import static org.eclipse.dltk.core.index.lucene.IndexFields.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.search.LeafCollector;
import org.apache.lucene.search.Scorer;

public class DocumentCollector2 implements CollectionCollector {
	
	private String fContainer;
	
	private static final String[] NUMERIC_FIELDS = new String[] { ELEMENT_TYPE,
			OFFSET, LENGTH, FLAGS, NAME_OFFSET, NAME_LENGTH };

	private static final String[] TEXTUAL_FIELDS = new String[] { PATH,
			ELEMENT_NAME, QUALIFIER, PARENT, METADATA, DOC };

	private class IteratorWrapper implements Iterator<DocumentEntity> {

		private Iterator<DocumentEntity> fIterator;

		public IteratorWrapper(Set<DocumentEntity> result) {
			this.fIterator = result.iterator();
		}

		@Override
		public boolean hasNext() {
			return fIterator.hasNext();
		}

		@Override
		public DocumentEntity next() {
			return fIterator.next();
		}

		@Override
		public void remove() {
			fIterator.remove();
		}

	}

	private Set<String> fFields;
	private Map<String, NumericDocValues> fDocNumValues;
	private Set<DocumentEntity> fResult = new HashSet<>();

	public DocumentCollector2(String container) {
		this.fContainer = container;
		this.fFields = new HashSet<>(Arrays.asList(TEXTUAL_FIELDS));
	}

	@Override
	public LeafCollector getLeafCollector(final LeafReaderContext context)
			throws IOException {
		final LeafReader reader = context.reader();
		fDocNumValues = new HashMap<String, NumericDocValues>();
		for (String field : NUMERIC_FIELDS) {
			NumericDocValues docValues = context.reader().getNumericDocValues(
					field);
			if (docValues != null) {
				fDocNumValues.put(field, docValues);
			}
		}
		return new LeafCollector() {

			@Override
			public void setScorer(Scorer arg0) throws IOException {
			}

			@Override
			public void collect(int docId) throws IOException {
				addDocument(docId, reader);
			}

		};
		
	}

	@Override
	public int resultSize() {
		return fResult.size();
	}

	@Override
	public Iterator<DocumentEntity> iterator() {
		return new IteratorWrapper(fResult);
	}

	private int get(String field, int docId) {
		return (int) fDocNumValues.get(field).get(docId);
	}

	private void addDocument(int docId, LeafReader reader) throws IOException {
		DocumentEntity documentEntity = new DocumentEntity();
		documentEntity.setContainer(fContainer);
		// Read numeric doc values
		documentEntity.setElementType(get(ELEMENT_TYPE, docId));
		documentEntity.setOffset(get(OFFSET, docId));
		documentEntity.setLength(get(LENGTH, docId));
		documentEntity.setFlags(get(FLAGS, docId));
		documentEntity.setNameOffset(get(NAME_OFFSET, docId));
		documentEntity.setNameLength(get(NAME_LENGTH, docId));
		// Read other field values
		Document document = reader.document(docId, fFields);
		documentEntity.setPath(document.get(PATH));
		documentEntity.setElementName(document.get(ELEMENT_NAME));
		documentEntity.setQualifier(document.get(QUALIFIER));
		documentEntity.setParent(document.get(PARENT));
		documentEntity.setDoc(document.get(DOC));
		documentEntity.setMetadata(document.get(METADATA));
		fResult.add(documentEntity);
	}

}