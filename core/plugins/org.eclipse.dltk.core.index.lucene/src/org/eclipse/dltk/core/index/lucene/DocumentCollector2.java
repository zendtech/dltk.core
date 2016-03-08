package org.eclipse.dltk.core.index.lucene;

import static org.eclipse.dltk.core.index.lucene.IndexFields.DOC;
import static org.eclipse.dltk.core.index.lucene.IndexFields.ELEMENT_NAME;
import static org.eclipse.dltk.core.index.lucene.IndexFields.ELEMENT_TYPE;
import static org.eclipse.dltk.core.index.lucene.IndexFields.FLAGS;
import static org.eclipse.dltk.core.index.lucene.IndexFields.LENGTH;
import static org.eclipse.dltk.core.index.lucene.IndexFields.METADATA;
import static org.eclipse.dltk.core.index.lucene.IndexFields.NAME_LENGTH;
import static org.eclipse.dltk.core.index.lucene.IndexFields.NAME_OFFSET;
import static org.eclipse.dltk.core.index.lucene.IndexFields.OFFSET;
import static org.eclipse.dltk.core.index.lucene.IndexFields.PARENT;
import static org.eclipse.dltk.core.index.lucene.IndexFields.PATH;
import static org.eclipse.dltk.core.index.lucene.IndexFields.QUALIFIER;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.index.StoredFieldVisitor;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.LeafCollector;
import org.apache.lucene.search.Scorer;

public class DocumentCollector2 implements Collector {

	private String fContainer;

	private static final String[] NUMERIC_FIELDS = new String[] { ELEMENT_TYPE, OFFSET, LENGTH, FLAGS, NAME_OFFSET,
			NAME_LENGTH };

	private static final String[] TEXTUAL_FIELDS = new String[] { PATH, ELEMENT_NAME, QUALIFIER, PARENT, METADATA,
			DOC };

	private Set<String> fFields;
	private Map<String, NumericDocValues> fDocNumValues;
	private List<DocumentEntity> fResult;
	private Map<String, String> fValues = new HashMap<>(6);

	public DocumentCollector2(String container, List<DocumentEntity> result) {
		this.fContainer = container;
		this.fResult = result;
		this.fFields = new HashSet<>(Arrays.asList(TEXTUAL_FIELDS));
	}

	@Override
	public LeafCollector getLeafCollector(final LeafReaderContext context) throws IOException {
		final LeafReader reader = context.reader();
		fDocNumValues = new HashMap<String, NumericDocValues>();
		for (String field : NUMERIC_FIELDS) {
			NumericDocValues docValues = context.reader().getNumericDocValues(field);
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
		reader.document(docId, new StoredFieldVisitor() {

			@Override
			public Status needsField(FieldInfo fieldInfo) throws IOException {
				return fFields.contains(fieldInfo.name) == true ? Status.YES : Status.STOP;
			}

			@Override
			public void stringField(FieldInfo fieldInfo, String value) throws IOException {
				fValues.put(fieldInfo.name, value);
			}
		});
		documentEntity.setQualifier(fValues.get(QUALIFIER));
		documentEntity.setParent(fValues.get(PARENT));
		documentEntity.setElementName(fValues.get(ELEMENT_NAME));

		documentEntity.setPath(fValues.get(PATH));
		documentEntity.setDoc(fValues.get(DOC));
		documentEntity.setMetadata(fValues.get(METADATA));

		fResult.add(documentEntity);

		fValues.clear();
	}

	private int get(String field, int docId) {
		NumericDocValues docValues = fDocNumValues.get(field);
		if (docValues != null) {
			return (int) docValues.get(docId);
		}
		return 0;
	}
}