package org.eclipse.dltk.core.index.lucene;

import java.io.IOException;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.search.BitsFilteredDocIdSet;
import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.Filter;
import org.apache.lucene.util.BitDocIdSet;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.FixedBitSet;

public class TimestampFilter extends Filter {

	private String field;

	public TimestampFilter(String field) {
		this.field = field;
	}

	@Override
	public DocIdSet getDocIdSet(LeafReaderContext context, Bits acceptDocs)
			throws IOException {
		NumericDocValues timestampValues = context.reader()
				.getNumericDocValues(field);
		if (timestampValues == null) {
			return null;
		}
		final int maxDoc = context.reader().maxDoc();
		FixedBitSet bitSet = new FixedBitSet(maxDoc);
		for (int i = 0; i < maxDoc; i++) {
			long timestamp = timestampValues.get(i);
			if (timestamp != 0) {
				bitSet.set(i);
			}
		}
		return BitsFilteredDocIdSet.wrap(new BitDocIdSet(bitSet), acceptDocs);
	}

}
