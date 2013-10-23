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

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.jobs.ILock;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.dltk.core.IModelElement;
import org.eclipse.dltk.core.index.sql.Container;
import org.eclipse.dltk.core.index.sql.DbFactory;
import org.eclipse.dltk.core.index.sql.Element;
import org.eclipse.dltk.core.index.sql.File;
import org.eclipse.dltk.core.index.sql.IElementDao;
import org.eclipse.dltk.core.index.sql.IElementHandler;
import org.eclipse.dltk.core.index.sql.h2.H2Index;
import org.eclipse.dltk.core.index2.search.ISearchEngine.MatchRule;

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

	private static final ILock elementLock = Job.getJobManager().newLock();
	private static final Map<Integer, Map<Integer, List<Element>>> elementsMap = new HashMap<Integer, Map<Integer, List<Element>>>();

	private static final ILock loadedLock = Job.getJobManager().newLock();
	private static boolean isLoaded;

	public static void addContainer(Container container) {
		containerLock.acquire();
		try {
			int containerId = container.getId();
			containerById.put(containerId, container);
		} finally {
			containerLock.release();
		}
	}

	public static void addElement(Element element) {
		elementLock.acquire();
		try {
			int elementType = element.getType();
			Map<Integer, List<Element>> elementsByFile = elementsMap
					.get(elementType);
			if (elementsByFile == null) {
				elementsByFile = new HashMap<Integer, List<Element>>();
				elementsMap.put(elementType, elementsByFile);
			}
			int fileId = element.getFileId();
			List<Element> elementsSet = elementsByFile.get(fileId);
			if (elementsSet == null) {
				elementsSet = new LinkedList<Element>();
				elementsByFile.put(fileId, elementsSet);
			}
			elementsSet.add(element);
		} finally {
			elementLock.release();
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

	public static void deleteElementsByFileId(int id) {
		elementLock.acquire();
		try {
			Iterator<Map<Integer, List<Element>>> i = elementsMap.values()
					.iterator();
			while (i.hasNext()) {
				Map<Integer, List<Element>> elementsByFile = i.next();
				elementsByFile.remove(id);
			}
		} finally {
			elementLock.release();
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

	public static Collection<Element> selectElementsByFileId(int id) {
		elementLock.acquire();
		try {
			List<Element> elements = new LinkedList<Element>();
			Iterator<Map<Integer, List<Element>>> i = elementsMap.values()
					.iterator();
			while (i.hasNext()) {
				Map<Integer, List<Element>> elementsByFile = i.next();
				List<Element> l = elementsByFile.get(id);
				if (l != null) {
					elements.addAll(l);
				}
			}
			return elements;
		} finally {
			elementLock.release();
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

	public static Collection<Element> searchElements(String pattern,
			MatchRule matchRule, int elementType, int trueFlags,
			int falseFlags, String qualifier, String parent, int[] filesId,
			int containersId[], String natureId, int limit) {

		Set<Integer> filesIds = new HashSet<Integer>();
		if (filesId != null) {
			for (int fileId : filesId) {
				filesIds.add(fileId);
			}
		} else if (containersId != null) {
			containerLock.acquire();
			try {
				for (int containerId : containersId) {
					fileLock.acquire();
					try {
						Map<Integer, File> files = filesByContainer
								.get(containerId);
						if (files != null) {
							filesIds.addAll(files.keySet());
						}
					} finally {
						fileLock.release();
					}
				}
			} finally {
				containerLock.release();
			}
		}

		elementLock.acquire();
		try {
			Set<String> patternSet = null;
			Pattern posixPattern = null;

			// Pre-cache pattern's lower and upper case variants:
			String patternLC = null;
			String patternUC = null;
			if (pattern != null) {
				patternLC = pattern.toLowerCase();
				patternUC = pattern.toUpperCase();
			}

			if (matchRule == MatchRule.SET) {
				patternSet = new HashSet<String>();
				String[] parts = pattern.split(",");
				for (String part : parts) {
					if (part.length() > 0) {
						patternSet.add(part.toLowerCase());
					}
				}
			} else if (matchRule == MatchRule.PATTERN) {
				posixPattern = createPosixPattern(pattern);
			}

			List<Element> result = new LinkedList<Element>();
			Map<Integer, List<Element>> elementsByFile = elementsMap
					.get(elementType);
			if (elementsByFile != null) {

				if (filesIds.size() == 0) {
					Iterator<List<Element>> i = elementsByFile.values()
							.iterator();
					while (i.hasNext()) {
						searchInElements(i.next(), result, pattern, matchRule,
								trueFlags, falseFlags, qualifier, parent,
								patternSet, posixPattern, patternLC, patternUC,
								limit);
					}
				} else {
					for (Integer fileId : filesIds) {
						searchInElements(elementsByFile.get(fileId), result,
								pattern, matchRule, trueFlags, falseFlags,
								qualifier, parent, patternSet, posixPattern,
								patternLC, patternUC, limit);
					}
				}
			}
			return result;

		} finally {
			elementLock.release();
		}
	}

	private static void searchInElements(List<Element> elements,
			List<Element> result, String pattern, MatchRule matchRule,
			int trueFlags, int falseFlags, String qualifier, String parent,
			Set<String> patternSet, Pattern posixPattern, String patternLC,
			String patternUC, int limit) {

		if (elements != null) {
			Iterator<Element> i = elements.iterator();
			while (i.hasNext()) {
				Element element = i.next();
				if (elementMatches(element, pattern, matchRule, trueFlags,
						falseFlags, qualifier, parent, patternSet,
						posixPattern, patternLC, patternUC)) {

					result.add(element);
					if (--limit == 0) {
						break;
					}
				}
			}
		}
	}

	private static boolean elementMatches(Element element, String pattern,
			MatchRule matchRule, int trueFlags, int falseFlags,
			String qualifier, String parent, Set<String> patternSet,
			Pattern posixPattern, String patternLC, String patternUC) {

		if ((trueFlags == 0 || (element.getFlags() & trueFlags) != 0)
				&& (falseFlags == 0 || (element.getFlags() & falseFlags) == 0)) {

			if (qualifier == null || qualifier.length() == 0
					|| qualifier.equals(element.getQualifier())) {

				if (parent == null || parent.length() == 0
						|| parent.equals(element.getParent())) {

					String elementName = element.getName();
					if (pattern == null
							|| pattern.length() == 0
							|| (matchRule == MatchRule.EXACT && pattern
									.equalsIgnoreCase(elementName))
							|| (matchRule == MatchRule.PREFIX && startsWithIgnoreCase(
									elementName, patternLC))
							|| (matchRule == MatchRule.CAMEL_CASE
									&& element.getCamelCaseName() != null && element
									.getCamelCaseName().startsWith(patternUC))
							|| (matchRule == MatchRule.SET && patternSet
									.contains(elementName.toLowerCase()))
							|| (matchRule == MatchRule.PATTERN && posixPattern
									.matcher(elementName).matches())) {
						return true;
					}
				}
			}
		}
		return false;
	}

	private static Pattern createPosixPattern(String pattern) {
		StringBuilder buf = new StringBuilder();
		boolean inQuoted = false;
		for (int i = 0; i < pattern.length(); ++i) {
			char ch = pattern.charAt(i);
			if (ch == '*') {
				if (inQuoted) {
					buf.append("\\E");
					inQuoted = false;
				}
				buf.append(".*");
			} else if (ch == '?') {
				if (inQuoted) {
					buf.append("\\E");
					inQuoted = false;
				}
				buf.append(".?");
			} else {
				if (!inQuoted) {
					buf.append("\\Q");
					inQuoted = true;
				}
				buf.append(ch);
			}
		}
		return Pattern.compile(buf.toString(), Pattern.CASE_INSENSITIVE);
	}

	private static boolean startsWithIgnoreCase(String str, String prefix) {
		return startsWith(str, prefix, true);
	}

	private static boolean startsWith(String str, String prefix,
			boolean ignoreCase) {
		if (str == null || prefix == null) {
			return (str == null && prefix == null);
		}
		if (prefix.length() > str.length()) {
			return false;
		}
		return str.regionMatches(ignoreCase, 0, prefix, 0, prefix.length());
	}

	public static boolean isLoaded() {
		loadedLock.acquire();
		try {
			return isLoaded;
		} finally {
			loadedLock.release();
		}
	}

	public static void load() {
		loadedLock.acquire();
		try {
			if (!isLoaded) {
				try {
					DbFactory dbFactory = DbFactory.getInstance();
					Connection connection = dbFactory.createConnection();
					try {
						IElementDao elementDao = dbFactory.getElementDao();
						elementDao.search(connection, null, MatchRule.PREFIX,
								IModelElement.FIELD, 0, 0, null, null, null,
								null, "org.eclipse.php.core.PHPNature", 0,
								false, new IElementHandler() {
									public void handle(Element element) {
									}
								}, new NullProgressMonitor());

						elementDao.search(connection, null, MatchRule.PREFIX,
								IModelElement.TYPE, 0, 0, null, null, null,
								null, "org.eclipse.php.core.PHPNature", 0,
								false, new IElementHandler() {
									public void handle(Element element) {
									}
								}, new NullProgressMonitor());

						elementDao.search(connection, null, MatchRule.PREFIX,
								IModelElement.METHOD, 0, 0, null, null, null,
								null, "org.eclipse.php.core.PHPNature", 0,
								false, new IElementHandler() {
									public void handle(Element element) {
									}
								}, new NullProgressMonitor());

						elementDao.search(connection, null, MatchRule.PREFIX,
								IModelElement.IMPORT_DECLARATION, 0, 0, null,
								null, null, null,
								"org.eclipse.php.core.PHPNature", 0, false,
								new IElementHandler() {
									public void handle(Element element) {
									}
								}, new NullProgressMonitor());
					} finally {
						connection.close();
					}
				} catch (SQLException e) {
					if (H2Index.DEBUG) {
						e.printStackTrace();
					}
				} finally {
					isLoaded = true;
				}
			}
		} finally {
			loadedLock.release();
		}
	}
}
