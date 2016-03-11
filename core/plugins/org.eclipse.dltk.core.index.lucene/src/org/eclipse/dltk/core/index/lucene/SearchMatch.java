package org.eclipse.dltk.core.index.lucene;

public class SearchMatch {

	private String fPath;
	private String fContainer;
	private int fElementType;
	private String fElementName;
	private int fOffset;
	private int fLength;
	private String fMetadata;
	private String fQualifier;
	private String fDoc;
	private int fFlags;
	private int fNameOffset;
	private int fNameLength;
	private String fParent;

	public SearchMatch(String container, int elementType, int offset, int length, int nameOffset, int nameLength,
			int flags, String elementName, String path, String parent, String qualifier, String doc, String metadata) {
		super();
		fContainer = container;
		fElementType = elementType;
		fOffset = offset;
		fLength = length;
		fNameOffset = nameOffset;
		fNameLength = nameLength;
		fFlags = flags;
		fElementName = elementName;
		fPath = path;
		fParent = parent;
		fQualifier = qualifier;
		fDoc = doc;
		fMetadata = metadata;
	}

	public String getPath() {
		return fPath;
	}

	public String getContainer() {
		return fContainer;
	}

	public int getElementType() {
		return fElementType;
	}

	public String getElementName() {
		return fElementName;
	}

	public int getOffset() {
		return fOffset;
	}

	public int getLength() {
		return fLength;
	}

	public String getMetadata() {
		return fMetadata;
	}

	public String getQualifier() {
		return fQualifier;
	}

	public String getDoc() {
		return fDoc;
	}

	public int getFlags() {
		return fFlags;
	}

	public int getNameOffset() {
		return fNameOffset;
	}

	public int getNameLength() {
		return fNameLength;
	}

	public String getParent() {
		return fParent;
	}

}
