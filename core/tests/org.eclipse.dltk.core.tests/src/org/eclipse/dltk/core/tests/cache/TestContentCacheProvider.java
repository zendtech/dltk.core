package org.eclipse.dltk.core.tests.cache;

import java.io.InputStream;

import org.eclipse.dltk.core.caching.IContentCache;
import org.eclipse.dltk.core.caching.IContentCacheProvider;
import org.eclipse.dltk.core.environment.IFileHandle;

public class TestContentCacheProvider implements IContentCacheProvider {

	public TestContentCacheProvider() {
	}

	@Override
	public InputStream getAttributeAndUpdateCache(IFileHandle handle,
			String attribute) {
		return null;
	}

	@Override
	public void setCache(IContentCache cache) {
	}
}
