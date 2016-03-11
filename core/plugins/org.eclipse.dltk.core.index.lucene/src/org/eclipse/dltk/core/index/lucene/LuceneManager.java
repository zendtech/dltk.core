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
import org.apache.lucene.index.Term;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.queries.BooleanFilter;
import org.apache.lucene.queries.TermFilter;
import org.apache.lucene.search.ConstantScoreQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.SearcherFactory;
import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

public enum LuceneManager {

	INSTANCE;

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

	private final class ContainerEntry {

		private static final String TIMESTAMPS_ID = "0"; //$NON-NLS-1$

		private final String fContainerId;

		private IndexWriter fTimestampsWriter;
		private SearcherManager fTimestampsSearcher;

		private Map<ContainerDataType, Map<Integer, IndexWriter>> fDataWriters;
		private Map<ContainerDataType, Map<Integer, SearcherManager>> fDataSearchers;

		public ContainerEntry(String containerId) {
			fContainerId = containerId;
			initialize();
		}

		private void initialize() {
			fDataWriters = new HashMap<ContainerDataType, Map<Integer, IndexWriter>>();
			fDataWriters.put(ContainerDataType.DECLARATIONS, new HashMap<Integer, IndexWriter>());
			fDataWriters.put(ContainerDataType.REFERENCES, new HashMap<Integer, IndexWriter>());
			fDataSearchers = new HashMap<ContainerDataType, Map<Integer, SearcherManager>>();
			fDataSearchers.put(ContainerDataType.DECLARATIONS, new HashMap<Integer, SearcherManager>());
			fDataSearchers.put(ContainerDataType.REFERENCES, new HashMap<Integer, SearcherManager>());
		}

		public final String getId() {
			return fContainerId;
		}

		public synchronized IndexWriter getTimestampsWriter() {
			if (fTimestampsWriter == null) {
				try {
					Directory indexDir = FSDirectory.open(Paths.get(
							fBundlePath.append(INDEX_DIR).append(fContainerId).append(TIMESTAMPS_ID).toOSString()));
					IndexWriterConfig config = new IndexWriterConfig(new DefaultAnalyzer());
					config.setOpenMode(OpenMode.CREATE_OR_APPEND);
					fTimestampsWriter = new IndexWriter(indexDir, config);
				} catch (IOException e) {
					Logger.logException(e);
				}
			}
			return fTimestampsWriter;
		}

		public synchronized SearcherManager getTimestampsSearcher() {
			try {
				if (fTimestampsSearcher == null) {
					fTimestampsSearcher = new SearcherManager(getTimestampsWriter(), true, new SearcherFactory());
				}
				// Try to achieve the up-to-date index state
				fTimestampsSearcher.maybeRefresh();
			} catch (IOException e) {
				Logger.logException(e);
			}
			return fTimestampsSearcher;
		}

		public synchronized IndexWriter getDataWriter(ContainerDataType dataType, int elementType) {
			IndexWriter writer = fDataWriters.get(dataType).get(elementType);
			if (writer == null) {
				try {
					Directory indexDir = FSDirectory.open(Paths.get(fBundlePath.append(INDEX_DIR).append(fContainerId)
							.append(dataType.getId()).append(String.valueOf(elementType)).toOSString()));
					IndexWriterConfig config = new IndexWriterConfig(new DefaultAnalyzer());
					config.setOpenMode(OpenMode.CREATE_OR_APPEND);
					writer = new IndexWriter(indexDir, config);
					fDataWriters.get(dataType).put(elementType, writer);
				} catch (IOException e) {
					Logger.logException(e);
				}
			}
			return writer;
		}

		public synchronized SearcherManager getDataSearcher(ContainerDataType dataType, int elementType) {
			SearcherManager searcher = fDataSearchers.get(dataType).get(elementType);
			try {
				if (searcher == null) {
					searcher = new SearcherManager(getDataWriter(dataType, elementType), true, new SearcherFactory());
					fDataSearchers.get(dataType).put(elementType, searcher);
				}
				// Try to achieve the up-to-date index state
				searcher.maybeRefresh();
			} catch (IOException e) {
				Logger.logException(e);
			}
			return searcher;
		}

		public synchronized void delete() {
			// Delete container entry entirely
			(new ContainerEntryCleaner(this)).schedule();
		}

		public synchronized void cleanup(String sourceModule) {
			BooleanFilter filter = new BooleanFilter();
			filter.add(new TermFilter(new Term(IndexFields.PATH, sourceModule)), Occur.MUST);
			Query query = new ConstantScoreQuery(filter);
			try {
				// Cleanup related time stamp
				getTimestampsWriter().deleteDocuments(query);
				// Cleanup all related documents in data writers
				for (Map<Integer, IndexWriter> dataWriters : fDataWriters.values()) {
					for (IndexWriter writer : dataWriters.values()) {
						writer.deleteDocuments(query);
					}
				}
			} catch (IOException e) {
				Logger.logException(e);
			}
		}

		public synchronized void close() {
			try {
				// Close time stamps searcher & writer
				getTimestampsSearcher().close();
				getTimestampsWriter().close();
				// Close all data searchers
				for (Map<Integer, SearcherManager> dataSearchers : fDataSearchers.values()) {
					for (SearcherManager searcher : dataSearchers.values()) {
						if (searcher != null)
							searcher.close();
					}
				}
				// Close all data writers
				for (Map<Integer, IndexWriter> dataWriters : fDataWriters.values()) {
					for (IndexWriter writer : dataWriters.values()) {
						if (writer != null)
							writer.close();
					}
				}
			} catch (IOException e) {
				Logger.logException(e);
			}
		}

	}

	private final class ContainerEntryCleaner extends Job {

		private ContainerEntry fContainerEntry;

		public ContainerEntryCleaner(ContainerEntry containerEntry) {
			super(""); //$NON-NLS-1$
			setUser(false);
			setSystem(true);
			fContainerEntry = containerEntry;
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			fContainerEntry.close();
			Path path = Paths.get(fBundlePath.append(INDEX_DIR).append(fContainerEntry.getId()).toOSString());
			delete(path.toFile());
			return Status.OK_STATUS;
		}

		@Override
		public boolean belongsTo(Object family) {
			return family == LucenePlugin.LUCENE_JOB_FAMILY;
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

	public enum ContainerDataType {

		DECLARATIONS("1"), //$NON-NLS-1$
		REFERENCES("2"); //$NON-NLS-1$

		private final String fId;

		private ContainerDataType(String id) {
			this.fId = id;
		}

		public String getId() {
			return fId;
		}

	}

	private static final String INDEX_DIR = "index"; //$NON-NLS-1$
	private static final String CONTAINERS_MAP = "mappings"; //$NON-NLS-1$

	private final IPath fBundlePath;

	private final Map<String, String> fContainerMappings;
	private final Map<String, ContainerEntry> fContainerEntries;

	private LuceneManager() {
		fContainerMappings = new HashMap<String, String>();
		fContainerEntries = new HashMap<String, ContainerEntry>();
		fBundlePath = Platform.getStateLocation(LucenePlugin.getDefault().getBundle());
		startup();
	}

	public final IndexWriter findDataWriter(String container, ContainerDataType dataType, int elementType) {
		return getContainerEntry(container).getDataWriter(dataType, elementType);
	}

	public final IndexWriter findTimestampsWriter(String container) {
		return getContainerEntry(container).getTimestampsWriter();
	}

	public final SearcherManager findDataSearcher(String container, ContainerDataType dataType, int elementType) {
		return getContainerEntry(container).getDataSearcher(dataType, elementType);
	}

	public final SearcherManager findTimestampsSearcher(String container) {
		return getContainerEntry(container).getTimestampsSearcher();
	}

	public final void cleanup(final String container) {
		removeContainerEntry(container);
	}

	public final void cleanup(String container, String sourceModule) {
		getContainerEntry(container).cleanup(sourceModule);
	}

	synchronized void startup() {
		loadContainerIds();
		// TODO - cleanup possibly non-existing container mappings
	}

	synchronized void shutdown() {
		// Close all searchers & writers in all container entries
		for (ContainerEntry entry : fContainerEntries.values()) {
			entry.close();
		}
	}

	private synchronized ContainerEntry getContainerEntry(String container) {
		String containerId = fContainerMappings.get(container);
		if (containerId == null) {
			do {
				// Just to be sure that ID does not already exist
				containerId = UUID.randomUUID().toString();
			} while (fContainerMappings.containsValue(containerId));
			fContainerMappings.put(container, containerId);
			fContainerEntries.put(containerId, new ContainerEntry(containerId));
			// Persist mapping
			saveContainerIds();
		}
		return fContainerEntries.get(containerId);
	}

	private synchronized void removeContainerEntry(String container) {
		String containerId = fContainerMappings.remove(container);
		if (containerId != null) {
			ContainerEntry containerEntry = fContainerEntries.remove(containerId);
			saveContainerIds();
			containerEntry.delete();
		}
	}

	private void saveContainerIds() {
		File file = Paths.get(fBundlePath.append(INDEX_DIR).append(CONTAINERS_MAP).toOSString()).toFile();
		ObjectOutputStream oos = null;
		try {
			if (!file.exists()) {
				file.createNewFile();
			}
			FileOutputStream fos = new FileOutputStream(file);
			oos = new ObjectOutputStream(fos);
			oos.writeObject(fContainerMappings);
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
		File indexDir = Paths.get(fBundlePath.append(INDEX_DIR).toOSString()).toFile();
		if (!indexDir.exists()) {
			indexDir.mkdirs();
		}
		File file = Paths.get(fBundlePath.append(INDEX_DIR).append(CONTAINERS_MAP).toOSString()).toFile();
		ObjectInputStream ois = null;
		try {
			if (!file.exists()) {
				return;
			}
			FileInputStream fis = new FileInputStream(file);
			ois = new ObjectInputStream(fis);
			fContainerMappings.putAll((Map<String, String>) ois.readObject());
			for (String container : fContainerMappings.keySet()) {
				String containerId = fContainerMappings.get(container);
				fContainerEntries.put(containerId, new ContainerEntry(containerId));
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
