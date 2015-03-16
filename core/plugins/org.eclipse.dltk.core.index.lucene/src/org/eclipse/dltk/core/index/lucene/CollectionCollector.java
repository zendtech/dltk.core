package org.eclipse.dltk.core.index.lucene;

import java.util.Iterator;

import org.apache.lucene.search.Collector;

public interface CollectionCollector extends Collector {

	int resultSize();

	Iterator<DocumentEntity> iterator();

}
