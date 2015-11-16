package org.eclipse.dltk.core.tests.model;

import org.eclipse.dltk.ast.parser.ISourceParser;
import org.eclipse.dltk.ast.parser.ISourceParserFactory;

public class TestSourceParserFactory implements ISourceParserFactory {

	@Override
	public ISourceParser createSourceParser() {
		return new TestSourceParser();
	}
}
