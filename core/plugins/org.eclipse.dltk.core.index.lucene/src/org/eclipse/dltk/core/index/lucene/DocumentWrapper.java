package org.eclipse.dltk.core.index.lucene;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;

public class DocumentWrapper {

	private Document document;

	public DocumentWrapper() {
		this.document = new Document();
	}

	public void setPath(String path) {
		addTextEntry(IndexFields.PATH, path);
	}

	public void setContainer(String container) {
		addTextEntry(IndexFields.CONTAINER, container);
	}

	public void setType(int type) {
		addLongEntry(IndexFields.TYPE, type);
	}

	public void setElementName(String elementName) {
		addTextEntry(IndexFields.ELEMENT_NAME, elementName);
	}

	public void setElementType(int elementType) {
		addLongEntry(IndexFields.ELEMENT_TYPE, elementType);
	}

	public void setOffset(int offset) {
		addLongEntry(IndexFields.OFFSET, offset);
	}

	public void setLength(int length) {
		addLongEntry(IndexFields.LENGTH, length);
	}

	public void setMetadata(String metadata) {
		if (metadata == null) {
			return;
		}
		FieldType fieldType = new FieldType();
		fieldType.setOmitNorms(true);
		fieldType.setStored(true);
		fieldType.setTokenized(false);
		document.add(new Field(IndexFields.METADATA, metadata, fieldType));
	}

	public void setQualifier(String qualifier) {
		addStringEntry(IndexFields.QUALIFIER, qualifier);
	}

	public String getQualifier() {
		return document.get(IndexFields.QUALIFIER);
	}

	public void setDoc(String doc) {
		if (doc == null) {
			return;
		}
		FieldType fieldType = new FieldType();
		fieldType.setOmitNorms(true);
		fieldType.setStored(true);
		fieldType.setTokenized(false);
		document.add(new Field(IndexFields.DOC, doc, fieldType));
	}

	public void setParent(String parent) {
		addStringEntry(IndexFields.PARENT, parent);
	}

	public void setNameOffset(int offset) {
		addLongEntry(IndexFields.NAME_OFFSET, offset);
	}

	public void setNameLength(int offset) {
		addLongEntry(IndexFields.NAME_LENGTH, offset);
	}

	public void setFlags(int flags) {
		addLongEntry(IndexFields.FLAGS, flags);
	}

	public void setCCName(String name) {
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
		addTextEntry(IndexFields.CC_NAME, camelCaseName);
	}

	public void setTimestamp(long timestamp) {
		addLongEntry(IndexFields.TIMESTAMP, timestamp);
	}

	private void addLongEntry(String category, long value) {
		document.add(new NumericDocValuesField(category, value));
	}

	private void addStringEntry(String category, String value) {
		if (value == null) {
			return;
		}
		document.add(new StringField(category, value, Field.Store.YES));
	}

	private void addTextEntry(String category, String value) {
		if (value == null) {
			return;
		}
		document.add(new TextField(category, value, Field.Store.YES));
	}

	public Document getDocument() {
		return document;
	}
}
