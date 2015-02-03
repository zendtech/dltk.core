/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Zend Technologies
 *******************************************************************************/
package org.eclipse.dltk.internal.core.index.sql.h2;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.runtime.jobs.ILock;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.dltk.core.index.sql.Container;
import org.eclipse.dltk.core.index.sql.File;

/**
 * This is a cache layer between H2 database and model access
 * 
 * @author michael
 */
public class H2Cache {

	private static final ILock containerLock = Job.getJobManager().newLock();
	private static final Map<Integer, Container> containerById = new HashMap<Integer, Container>();

	private static final ILock fileLock = Job.getJobManager().newLock();
	private static final Map<Integer, Map<Integer, File>> filesByContainer = new HashMap<Integer, Map<Integer, File>>();
	private static final Map<Integer, Map<String, File>> filesByContainerAndPath = new HashMap<Integer, Map<String, File>>();

	public static void addContainer(Container container) {
		containerLock.acquire();
		try {
			int containerId = container.getId();
			containerById.put(containerId, container);
		} finally {
			containerLock.release();
		}
	}

	public static void addFile(File file) {
		int containerId = file.getContainerId();

		fileLock.acquire();
		try {
			Map<Integer, File> filesById = filesByContainer.get(containerId);
			if (filesById == null) {
				filesById = new HashMap<Integer, File>();
				filesByContainer.put(containerId, filesById);
			}
			filesById.put(file.getId(), file);

			Map<String, File> filesByPath = filesByContainerAndPath
					.get(containerId);
			if (filesByPath == null) {
				filesByPath = new HashMap<String, File>();
				filesByContainerAndPath.put(containerId, filesByPath);
			}
			filesByPath.put(file.getPath(), file);
		} finally {
			fileLock.release();
		}
	}

	public static void deleteContainerById(int id) {
		containerLock.acquire();
		try {
			containerById.remove(id);
			deleteFilesByContainerId(id);
		} finally {
			containerLock.release();
		}
	}

	public static void deleteContainerByPath(String path) {
		containerLock.acquire();
		try {
			Container container = selectContainerByPath(path);
			if (container != null) {
				deleteContainerById(container.getId());
			}
		} finally {
			containerLock.release();
		}
	}

	public static void deleteFileByContainerIdAndPath(int containerId,
			String path) {
		fileLock.acquire();
		try {
			File file = null;

			Map<String, File> filesByPath = filesByContainerAndPath
					.get(containerId);
			if (filesByPath != null) {
				file = filesByPath.remove(path);
			}

			if (file != null) {
				Map<Integer, File> filesById = filesByContainer
						.get(containerById);
				if (filesById != null) {
					filesById.remove(file.getId());
				}
			}

			deleteElementsByFileId(file.getId());
		} finally {
			fileLock.release();
		}
	}

	public static void deleteFileById(int id) {
		fileLock.acquire();
		try {
			File file = null;

			Iterator<Map<Integer, File>> i = filesByContainer.values()
					.iterator();
			while (i.hasNext()) {
				file = i.next().remove(id);
			}

			if (file != null) {
				Map<String, File> filesByPath = filesByContainerAndPath
						.get(file.getContainerId());
				if (filesByPath != null) {
					filesByPath.remove(file.getPath());
				}
			}

			deleteElementsByFileId(id);
		} finally {
			fileLock.release();
		}
	}

	public static void deleteFilesByContainerId(int id) {
		fileLock.acquire();
		try {
			Map<Integer, File> filesById = filesByContainer.remove(id);
			if (filesById != null) {
				Iterator<Integer> i = filesById.keySet().iterator();
				while (i.hasNext()) {
					deleteElementsByFileId(i.next());
				}
			}
			filesByContainerAndPath.remove(id);
		} finally {
			fileLock.release();
		}
	}

	public static Container selectContainerById(int id) {
		containerLock.acquire();
		try {
			return containerById.get(id);
		} finally {
			containerLock.release();
		}
	}

	public static Container selectContainerByPath(String path) {
		containerLock.acquire();
		try {
			Iterator<Container> i = containerById.values().iterator();
			while (i.hasNext()) {
				Container container = i.next();
				if (container.getPath().equals(path)) {
					return container;
				}
			}
			return null;
		} finally {
			containerLock.release();
		}
	}

	public static File selectFileByContainerIdAndPath(int containerId,
			String path) {
		fileLock.acquire();
		try {
			Map<String, File> files = filesByContainerAndPath.get(containerId);
			return (files == null) ? null : files.get(path);
		} finally {
			fileLock.release();
		}
	}

	public static File selectFileById(int id) {
		fileLock.acquire();
		try {
			Iterator<Map<Integer, File>> i = filesByContainer.values()
					.iterator();
			while (i.hasNext()) {
				File file = i.next().get(id);
				if (file != null) {
					return file;
				}
			}
			return null;
		} finally {
			fileLock.release();
		}
	}

	public static Collection<File> selectFilesByContainerId(int id) {
		fileLock.acquire();
		try {
			Map<Integer, File> files = filesByContainer.get(id);
			if (files != null) {
				return files.values();
			}
			return Collections.emptyList();
		} finally {
			fileLock.release();
		}
	}
}
