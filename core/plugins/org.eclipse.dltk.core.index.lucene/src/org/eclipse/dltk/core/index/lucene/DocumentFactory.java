package org.eclipse.dltk.core.index.lucene;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.eclipse.dltk.core.index2.IIndexingRequestor.DeclarationInfo;
import org.eclipse.dltk.core.index2.IIndexingRequestor.ReferenceInfo;

public enum DocumentFactory {

	INSTANCE;

	public Document buildReference(String source, ReferenceInfo info) {
		Document doc = new Document();
		addTextEntry(doc, IndexFields.PATH, source);
		addTextLCEntry(doc, IndexFields.ELEMENT_NAME_LC, info.elementName);
		addTextEntry(doc, IndexFields.ELEMENT_NAME, info.elementName);
		addStringEntry(doc, IndexFields.QUALIFIER, info.qualifier);
		addLongEntry(doc, IndexFields.ELEMENT_TYPE, info.elementType);
		addLongEntry(doc, IndexFields.OFFSET, info.offset);
		addLongEntry(doc, IndexFields.LENGTH, info.length);
		addMetadataEntry(doc, info.metadata);
		return doc;
	}

	public Document buildDeclaration(String source, DeclarationInfo info) {
		Document doc = new Document();
		addTextEntry(doc, IndexFields.PATH, source);
		addTextLCEntry(doc, IndexFields.ELEMENT_NAME_LC, info.elementName);
		addTextEntry(doc, IndexFields.ELEMENT_NAME, info.elementName);
		addCCNameEntry(doc, info.elementName);
		addStringEntry(doc, IndexFields.PARENT, info.parent);
		addStringEntry(doc, IndexFields.QUALIFIER, info.qualifier);
		addLongEntry(doc, IndexFields.ELEMENT_TYPE, info.elementType);
		addLongEntry(doc, IndexFields.OFFSET, info.offset);
		addLongEntry(doc, IndexFields.LENGTH, info.length);
		addLongEntry(doc, IndexFields.NAME_OFFSET, info.nameOffset);
		addLongEntry(doc, IndexFields.NAME_LENGTH, info.nameLength);
		addLongEntry(doc, IndexFields.FLAGS, info.flags);
		addMetadataEntry(doc, info.metadata);
		addDocEntry(doc, info.doc);
		return doc;
	}

	public Document buildSourceInfo(String source, long timestamp) {
		Document doc = new Document();
		addTextEntry(doc, IndexFields.PATH, source);
		addLongEntry(doc, IndexFields.TIMESTAMP, timestamp);
		return doc;
	}

	private void addLongEntry(Document doc, String category, long value) {
		doc.add(new NumericDocValuesField(category, value));
	}

	private void addStringEntry(Document doc, String category, String value) {
		if (value == null) {
			return;
		}
		doc.add(new StringField(category, value, Field.Store.YES));
	}

	private void addTextEntry(Document doc, String category, String value) {
		if (value == null) {
			return;
		}
		doc.add(new TextField(category, value, Field.Store.YES));
	}
	
	private void addTextLCEntry(Document doc, String category, String value) {
		if (value == null) {
			return;
		}
		doc.add(new TextField(category, value.toLowerCase(), Field.Store.NO));
	}

	private void addMetadataEntry(Document doc, String metadata) {
		if (metadata == null) {
			return;
		}
		FieldType fieldType = new FieldType();
		fieldType.setOmitNorms(true);
		fieldType.setStored(true);
		fieldType.setTokenized(false);
		doc.add(new Field(IndexFields.METADATA, metadata, fieldType));
	}
	
	private void addDocEntry(Document doc, String phpDoc) {
		if (phpDoc == null) {
			return;
		}
		FieldType fieldType = new FieldType();
		fieldType.setOmitNorms(true);
		fieldType.setStored(true);
		fieldType.setTokenized(false);
		doc.add(new Field(IndexFields.DOC, phpDoc, fieldType));
	}
	
	private void addCCNameEntry(Document doc, String name) {
		String camelCaseName = null;
		StringBuilder camelCaseNameBuf = new StringBuilder();
		for (int i = 0; i < name.length(); ++i) {
			char ch = name.charAt(i);
			if (Character.isUpperCase(ch)) {
				camelCaseNameBuf.append(ch);
			} else if (i == 0) {
				// not applicable for camel case search
				break;
			}
		}
		camelCaseName = camelCaseNameBuf.length() > 0 ? camelCaseNameBuf
				.toString() : null;
		addTextEntry(doc, IndexFields.CC_NAME, camelCaseName);
	}

}
