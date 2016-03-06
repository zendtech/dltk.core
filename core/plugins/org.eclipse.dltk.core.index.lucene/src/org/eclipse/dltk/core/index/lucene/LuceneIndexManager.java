package org.eclipse.dltk.core.index.lucene;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.search.SearcherFactory;
import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

public enum LuceneIndexManager {

	INSTANCE;

	public enum IndexType {

		SOURCES_INFO("sourcesInfo"), //$NON-NLS-1$
		DECLARATIONS("declarations"), //$NON-NLS-1$
		REFERENCES("references"); //$NON-NLS-1$

		private final String descriptor;

		private IndexType(String descriptor) {
			this.descriptor = descriptor;
		}

		public String getDescriptor() {
			return descriptor;
		}

	}

	private class IndexCleaner extends Job {

		private String containerId;

		public IndexCleaner(String containerId) {
			super(""); //$NON-NLS-1$
			setUser(false);
			setSystem(true);
			this.containerId = containerId;
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			for (IndexType type : IndexType.values()) {
				IndexWriter writer = fWriters.get(containerId).get(type);
				SearcherManager searcher = fSearchers.get(containerId).get(type);
				try {
					if (writer != null)
						writer.close();
					if (searcher != null)
						searcher.close();
				} catch (IOException e) {
					Logger.logException(e);
				}
			}
			fWriters.remove(containerId);
			fSearchers.remove(containerId);
			IPath bundlePath = Platform.getStateLocation(LuceneIndexerPlugin.getDefault().getBundle());
			Path path = Paths.get(bundlePath.append(INDEX_DIR).append(containerId).toOSString());
			delete(path.toFile());
			return Status.OK_STATUS;
		}

		private boolean delete(File path) {
			if (path.exists()) {
				File[] files = path.listFiles();
				for (int i = 0; i < files.length; i++) {
					if (files[i].isDirectory()) {
						delete(files[i]);
					} else {
						files[i].delete();
					}
				}
			}
			return (path.delete());
		}

	}

	private static final String INDEX_DIR = "index"; //$NON-NLS-1$

	private final Map<String, String> fContainerIds = new HashMap<String, String>();

	private final Map<String, Map<IndexType, IndexWriter>> fWriters = new HashMap<String, Map<IndexType, IndexWriter>>();
	private final Map<String, Map<IndexType, SearcherManager>> fSearchers = new HashMap<String, Map<IndexType, SearcherManager>>();

	private synchronized String getContainerId(String container) {
		String containerId = fContainerIds.get(container);
		if (containerId == null) {
			containerId = UUID.randomUUID().toString();
			fContainerIds.put(container, containerId);
			fWriters.put(containerId, new HashMap<IndexType, IndexWriter>());
			fSearchers.put(containerId, new HashMap<IndexType, SearcherManager>());
		}
		return containerId;
	}

	public synchronized IndexWriter findWriter(String container, IndexType type) {
		String containerId = getContainerId(container);
		IndexWriter writer = fWriters.get(containerId).get(type);
		if (writer == null) {
			IPath bundlePath = Platform.getStateLocation(LuceneIndexerPlugin.getDefault().getBundle());
			Directory indexDir = null;
			try {
				indexDir = FSDirectory.open(Paths.get(
						bundlePath.append(INDEX_DIR).append(containerId).append(type.getDescriptor()).toOSString()));
				IndexWriterConfig config = new IndexWriterConfig(new DefaultAnalyzer());
				config.setOpenMode(OpenMode.CREATE_OR_APPEND);
				writer = new IndexWriter(indexDir, config);
				fWriters.get(containerId).put(type, writer);
			} catch (IOException e) {
				Logger.logException(e);
			}
		}
		return writer;
	}

	public synchronized SearcherManager findSearcher(String container, IndexType type) {
		String containerId = getContainerId(container);
		SearcherManager searcher = fSearchers.get(containerId).get(type);
		if (searcher == null) {
			try {
				searcher = new SearcherManager(findWriter(container, type), true, new SearcherFactory());
				fSearchers.get(containerId).put(type, searcher);
			} catch (IOException e) {
				Logger.logException(e);
			}
		}
		return searcher;
	}

	synchronized void cleanup(final String container) {
		String containerId = getContainerId(container);
		fContainerIds.remove(container);
		(new IndexCleaner(containerId)).schedule();
	}

}
