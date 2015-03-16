package org.eclipse.dltk.core.index.lucene;

import java.io.IOException;

import org.apache.lucene.index.DocValues;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.DocValuesDocIdSet;
import org.apache.lucene.search.Filter;
import org.apache.lucene.util.Bits;

public class NumberFilter extends Filter {

	private String field;
	private int value;

	public NumberFilter(String field, int value) {
		this.field = field;
		this.value = value;
	}

	@Override
	public DocIdSet getDocIdSet(final LeafReaderContext context, Bits acceptDocs)
			throws IOException {
		final NumericDocValues values = DocValues.getNumeric(context.reader(),
				field);

		return new DocValuesDocIdSet(context.reader().maxDoc(), acceptDocs) {

			@Override
			protected boolean matchDoc(int doc) {
				long tmpValue = values.get(doc);
				if (value == tmpValue) {
					return true;

				}
				return false;
			}
		};

	}

	@Override
	public int hashCode() {
		return field.hashCode() * value;
	}

	@Override
	public String toString() {
		StringBuilder buffer = new StringBuilder();
		buffer.append("NumberEqualFilter(");
		buffer.append(field);
		buffer.append(":");
		buffer.append(value);
		buffer.append(")");
		return buffer.toString();
	}

}
