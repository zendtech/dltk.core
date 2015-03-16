package org.eclipse.dltk.core.index.lucene;

import java.io.IOException;
import java.nio.file.Paths;

import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.search.SearcherFactory;
import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Plugin;
import org.osgi.framework.BundleContext;

public class LuceneIndexerPlugin extends Plugin {

	private static final String INDEX_DIRECTORY = "index";

	private static LuceneIndexerPlugin plugin;

	public static LuceneIndexerPlugin getDefault() {
		return plugin;
	}

	private static IndexWriter indexWriter;
	private static SearcherManager searcherManager;

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext
	 * )
	 */
	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext
	 * )
	 */
	@Override
	public void stop(BundleContext context) throws Exception {
		if (indexWriter != null) {
			indexWriter.close();
			indexWriter = null;
		}
		if (searcherManager != null) {
			searcherManager.close();
			searcherManager = null;
		}
		plugin = null;
		super.stop(context);
	}

	public static IndexWriter getIndexWriter() {
		if (indexWriter == null) {
			IPath path = Platform.getStateLocation(LuceneIndexerPlugin.getDefault().getBundle());
			Directory indexDir = null;
			try {
				indexDir = FSDirectory.open(Paths.get(path.append(INDEX_DIRECTORY).toOSString()));
				IndexWriterConfig config = new IndexWriterConfig(new DefaultAnalyzer());
				config.setOpenMode(OpenMode.CREATE_OR_APPEND);
				config.setRAMBufferSizeMB(256.0);
				return indexWriter = new IndexWriter(indexDir, config);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return indexWriter;
	}

	public static SearcherManager getSearcherManager() {
		if (searcherManager == null) {
			try {
				searcherManager = new SearcherManager(getIndexWriter(), true, new SearcherFactory());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return searcherManager;
	}

}
