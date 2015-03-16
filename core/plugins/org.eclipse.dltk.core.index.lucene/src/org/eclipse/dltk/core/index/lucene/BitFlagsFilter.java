package org.eclipse.dltk.core.index.lucene;

import java.io.IOException;

import org.apache.lucene.index.DocValues;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.DocValuesDocIdSet;
import org.apache.lucene.search.Filter;
import org.apache.lucene.util.Bits;

public class BitFlagsFilter extends Filter {

	private String field;
	private int trueFlags;
	private int falseFlags;

	public BitFlagsFilter(String field, int trueFlags, int falseFlags) {
		this.field = field;
		this.trueFlags = trueFlags;
		this.falseFlags = falseFlags;
	}

	@Override
	public DocIdSet getDocIdSet(LeafReaderContext context, Bits acceptDocs)
			throws IOException {
		final NumericDocValues values = DocValues.getNumeric(context.reader(),
				field);

		return new DocValuesDocIdSet(context.reader().maxDoc(), acceptDocs) {

			@Override
			protected boolean matchDoc(int doc) {
				long flags = values.get(doc);

				if (trueFlags != 0) {
					if ((trueFlags & flags) == 0) {
						return false;
					}
				}
				if (falseFlags != 0) {
					if ((falseFlags & flags) != 0) {
						return false;
					}
				}
				return true;
			}
		};
	}

}
