/*******************************************************************************
 * Copyright (c) 2015 Zend Techologies Ltd.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Zend Technologies Ltd. - initial API and implementation
 *******************************************************************************/
package org.eclipse.dltk.core.tests;

import java.io.IOException;
import java.io.StringReader;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.eclipse.core.runtime.Path;
import org.eclipse.dltk.core.DLTKCore;
import org.eclipse.dltk.core.IBuildpathEntry;
import org.eclipse.dltk.core.environment.EnvironmentManager;
import org.eclipse.dltk.core.environment.EnvironmentPathUtils;
import org.eclipse.dltk.internal.core.UserLibrary;

public class UserLibraryTests extends TestCase {

	private static String TEMPLATE = "";
	static {
		TEMPLATE += "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";
		TEMPLATE += "<userlibrary {2}systemlibrary=\"{1}\" version=\"1\">\n";
		TEMPLATE += "	<archive path=\"org.eclipse.dltk.core.environment.localEnvironment/:{0}\"/>\n";
		TEMPLATE += "</userlibrary>\n";
	}

	public void testSerializationSystemLibrary() throws IOException {
		String path = "/segment01";

		IBuildpathEntry[] entries = new IBuildpathEntry[] { createBuildpathEntry(path) };

		String output = UserLibrary.serialize(entries, true);

		assertEquals(output, createXML(path, true));
	}

	public void testSerializationNotSystemLibrary() throws IOException {
		String path = "/segment01/segment02";

		IBuildpathEntry[] entries = new IBuildpathEntry[] { createBuildpathEntry(path) };

		String output = UserLibrary.serialize(entries, false);

		assertEquals(output, createXML(path, false));
	}

	public void testSerializationAttributes() throws IOException {
		String path = "/segment01/segment02";

		IBuildpathEntry[] entries = new IBuildpathEntry[] { createBuildpathEntry(path) };

		Map<String, String> attributes = new HashMap<String, String>();
		attributes.put("libraryVersion", "1.0.0");
		attributes.put("builtin", "true");
		String output = UserLibrary.serialize(entries, false, attributes);

		String attributesOutput = "__attribute__builtin=\"true\" ";
		attributesOutput += "__attribute__libraryVersion=\"1.0.0\" ";
		assertEquals(output, createXML(path, false, attributesOutput));
	}

	public void testDeserializationSystemLibrary() throws IOException {
		String path = "/library/path";
		IBuildpathEntry buildpathEntry = createBuildpathEntry(path);
		String xml = createXML(path, true);
		StringReader reader = new StringReader(xml);
		UserLibrary userLibrary = UserLibrary.createFromString(reader);

		assertEquals(userLibrary.getEntries().length, 1);
		assertEquals(userLibrary.getEntries()[0], buildpathEntry);
		assertEquals(userLibrary.isSystemLibrary(), true);
		assertEquals(userLibrary.getAttributes().size(), 0);
	}

	public void testDeserializationNotSystemLibrary() throws IOException {
		String path = "/library/path";
		IBuildpathEntry buildpathEntry = createBuildpathEntry(path);
		String xml = createXML(path, false);
		StringReader reader = new StringReader(xml);
		UserLibrary userLibrary = UserLibrary.createFromString(reader);

		assertEquals(userLibrary.getEntries().length, 1);
		assertEquals(userLibrary.getEntries()[0], buildpathEntry);
		assertEquals(userLibrary.isSystemLibrary(), false);
		assertEquals(userLibrary.getAttributes().size(), 0);
	}

	public void testDeserializationAttributes() throws IOException {
		String path = "/library/path";
		IBuildpathEntry buildpathEntry = createBuildpathEntry(path);

		String attributesOutput = "__attribute__builtin=\"true\" ";
		attributesOutput += "__attribute__libraryVersion=\"1.0.0\" ";
		String xml = createXML(path, false, attributesOutput);
		StringReader reader = new StringReader(xml);
		UserLibrary userLibrary = UserLibrary.createFromString(reader);

		Map<String, String> attributes = new HashMap<String, String>();
		attributes.put("libraryVersion", "1.0.0");
		attributes.put("builtin", "true");

		assertEquals(userLibrary.getEntries().length, 1);
		assertEquals(userLibrary.getEntries()[0], buildpathEntry);
		assertEquals(userLibrary.isSystemLibrary(), false);
		assertEquals(userLibrary.getAttributes(), attributes);
	}

	private IBuildpathEntry createBuildpathEntry(String path) {
		return DLTKCore.newLibraryEntry(EnvironmentPathUtils.getFullPath(
				EnvironmentManager.getLocalEnvironment(), new Path(path)),
				false, true);
	}

	private String createXML(String path, boolean isSystemLibrary) {
		return MessageFormat.format(TEMPLATE, path, isSystemLibrary, "");
	}

	private String createXML(String path, boolean isSystemLibrary,
			String attributes) {
		return MessageFormat
				.format(TEMPLATE, path, isSystemLibrary, attributes);
	}

}
