package org.eclipse.dltk.core.index.lucene;

public interface IndexFields {

	String PATH = "path";
	String CONTAINER = "container";
	String TYPE = "type";
	String TIMESTAMP = "timestamp";

	String ELEMENT_NAME = "elementName";
	String ELEMENT_TYPE = "elementType";
	String CC_NAME = "ccName";
	String OFFSET = "offset";
	String LENGTH = "length";
	String METADATA = "metadata";
	String QUALIFIER = "qualifier";
	String DOC = "doc";
	String FLAGS = "flags";
	String NAME_OFFSET = "nameOffset";
	String NAME_LENGTH = "nameLength";
	String PARENT = "parent";

	// String PARENT_DOCUMENT = "parent_document";
	// int PARENT_DOCUMENT_VALUE = 1;

	int TYPE_REFERENCE = 1;
	int TYPE_DECLARATION = 2;
}
