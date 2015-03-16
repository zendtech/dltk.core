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

public class DocumentCollector implements CollectionCollector {

	private static final String[] NUM_FIELDS = new String[] { ELEMENT_TYPE,
			OFFSET, LENGTH, FLAGS, NAME_OFFSET, NAME_LENGTH };

	private static final String[] STRING_FIELDS = new String[] { PATH,
			CONTAINER, ELEMENT_NAME, QUALIFIER, PARENT, METADATA, DOC };

	private class IteratorWrapper implements Iterator<DocumentEntity> {

		private Iterator<DocumentEntity> iterator;

		public IteratorWrapper(Set<DocumentEntity> result) {
			this.iterator = result.iterator();
		}

		@Override
		public boolean hasNext() {
			return iterator.hasNext();
		}

		@Override
		public DocumentEntity next() {
			return iterator.next();
		}

		@Override
		public void remove() {
			iterator.remove();
		}

	}

	private Set<String> fields;
	private Map<String, NumericDocValues> docValuesMap;
	private Set<DocumentEntity> result = new HashSet<>();

	public DocumentCollector() {
		this.fields = new HashSet<>(Arrays.asList(STRING_FIELDS));
	}

	@Override
	public LeafCollector getLeafCollector(final LeafReaderContext context)
			throws IOException {
		final LeafReader reader = context.reader();
		docValuesMap = new HashMap<String, NumericDocValues>();
		for (String field : NUM_FIELDS) {
			NumericDocValues docValues = context.reader().getNumericDocValues(
					field);
			if (docValues != null) {
				docValuesMap.put(field, docValues);
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

	private void addDocument(int docId, LeafReader reader) throws IOException {
		DocumentEntity documentEntity = new DocumentEntity();
		documentEntity.setElementType(get(ELEMENT_TYPE, docId));
		documentEntity.setOffset(get(OFFSET, docId));
		documentEntity.setLength(get(LENGTH, docId));
		documentEntity.setFlags(get(FLAGS, docId));
		documentEntity.setNameOffset(get(NAME_OFFSET, docId));
		documentEntity.setNameLength(get(NAME_LENGTH, docId));

		Document document = reader.document(docId, fields);

		documentEntity.setPath(document.get(PATH));
		documentEntity.setContainer(document.get(CONTAINER));
		documentEntity.setElementName(document.get(ELEMENT_NAME));

		documentEntity.setQualifier(document.get(QUALIFIER));
		documentEntity.setParent(document.get(PARENT));
		documentEntity.setDoc(document.get(DOC));
		documentEntity.setMetadata(document.get(METADATA));

		result.add(documentEntity);
	}

	private int get(String field, int docId) {
		return (int) docValuesMap.get(field).get(docId);
	}

	@Override
	public int resultSize() {
		return result.size();
	}

	@Override
	public Iterator<DocumentEntity> iterator() {
		return new IteratorWrapper(result);
	}

}