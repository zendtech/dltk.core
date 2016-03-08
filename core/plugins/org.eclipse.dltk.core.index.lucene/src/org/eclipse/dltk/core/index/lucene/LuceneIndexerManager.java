package org.eclipse.dltk.core.index.lucene;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.util.CharTokenizer;
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

public enum LuceneIndexerManager {

	INSTANCE;

	public enum IndexType {

		TIMESTAMPS("timestamps"), //$NON-NLS-1$
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

	private final class IndexCleaner extends Job {

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

		@Override
		public boolean belongsTo(Object family) {
			return family == LuceneIndexerPlugin.LUCENE_JOB_FAMILY;
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
	
	private final class DefaultAnalyzer extends Analyzer {

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

	private static final String INDEX_DIR = "index"; //$NON-NLS-1$
	private static final String IDS_FILE = "mappings"; //$NON-NLS-1$

	private Map<String, String> fContainerIds;

	private Map<String, Map<IndexType, IndexWriter>> fWriters = new HashMap<String, Map<IndexType, IndexWriter>>();
	private Map<String, Map<IndexType, SearcherManager>> fSearchers = new HashMap<String, Map<IndexType, SearcherManager>>();

	private LuceneIndexerManager() {
		fContainerIds = new HashMap<String, String>();
		fWriters = new HashMap<String, Map<IndexType, IndexWriter>>();
		fSearchers = new HashMap<String, Map<IndexType, SearcherManager>>();
		startup();
	}

	private synchronized String getContainerId(String container) {
		String containerId = fContainerIds.get(container);
		if (containerId == null) {
			containerId = UUID.randomUUID().toString();
			fContainerIds.put(container, containerId);
			fWriters.put(containerId, new HashMap<IndexType, IndexWriter>());
			fSearchers.put(containerId, new HashMap<IndexType, SearcherManager>());
			saveContainerIds();
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
		try {
			if (searcher == null) {
				searcher = new SearcherManager(findWriter(container, type), true, new SearcherFactory());
				fSearchers.get(containerId).put(type, searcher);
			}
			// Try to achieve the up-to-date index state
			searcher.maybeRefresh();
		} catch (IOException e) {
			Logger.logException(e);
		}
		return searcher;
	}

	synchronized void cleanup(final String container) {
		String containerId = getContainerId(container);
		fContainerIds.remove(container);
		saveContainerIds();
		(new IndexCleaner(containerId)).schedule();
	}

	synchronized void startup() {
		loadContainerIds();
		// TODO - cleanup possibly non-existing container mappings
	}

	synchronized void shutdown() {
		// Close all writers
		for (Map<IndexType, IndexWriter> writers : fWriters.values()) {
			for (IndexWriter writer : writers.values()) {
				try {
					writer.close();
				} catch (IOException e) {
					Logger.logException(e);
				}
			}
		}
	}

	private void saveContainerIds() {
		File file = Paths.get(Platform.getStateLocation(LuceneIndexerPlugin.getDefault().getBundle()).append(INDEX_DIR)
				.append(IDS_FILE).toOSString()).toFile();
		ObjectOutputStream oos = null;
		try {
			if (!file.exists()) {
				file.createNewFile();
			}
			FileOutputStream fos = new FileOutputStream(file);
			oos = new ObjectOutputStream(fos);
			oos.writeObject(fContainerIds);
		} catch (IOException e) {
			Logger.logException(e);
		} finally {
			if (oos != null) {
				try {
					oos.close();
				} catch (IOException e) {
					Logger.logException(e);
				}
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void loadContainerIds() {
		IPath bundlePath = Platform.getStateLocation(LuceneIndexerPlugin.getDefault().getBundle());
		File indexDir = Paths.get(bundlePath.append(INDEX_DIR).toOSString()).toFile();
		if (!indexDir.exists()) {
			indexDir.mkdirs();
		}
		File file = Paths.get(bundlePath.append(INDEX_DIR).append(IDS_FILE).toOSString()).toFile();
		ObjectInputStream ois = null;
		try {
			if (!file.exists()) {
				return;
			}
			FileInputStream fis = new FileInputStream(file);
			ois = new ObjectInputStream(fis);
			fContainerIds = (Map<String, String>) ois.readObject();
			for (String container : fContainerIds.keySet()) {
				String containerId = fContainerIds.get(container);
				fWriters.put(containerId, new HashMap<IndexType, IndexWriter>());
				fSearchers.put(containerId, new HashMap<IndexType, SearcherManager>());
			}
		} catch (Exception e) {
			Logger.logException(e);
		} finally {
			if (ois != null) {
				try {
					ois.close();
				} catch (IOException e) {
					Logger.logException(e);
				}
			}
		}
	}

}
