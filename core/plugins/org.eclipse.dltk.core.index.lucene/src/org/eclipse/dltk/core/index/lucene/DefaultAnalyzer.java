package org.eclipse.dltk.core.index.lucene;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.util.CharTokenizer;

public class DefaultAnalyzer extends Analyzer {

	@Override
	protected TokenStreamComponents createComponents(String fieldName) {
		final Tokenizer src = new CharTokenizer() {
			@Override
			protected boolean isTokenChar(int arg0) {
				return true;
			}
		};
		TokenStream tok = new StandardFilter(src);
		return new TokenStreamComponents(src, tok);
	}

}
