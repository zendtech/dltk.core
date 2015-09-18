package org.eclipse.dltk.core;

import java.util.Map;

public interface IBuildpathContainerExtension3 {

	/**
	 * Answers container'a additional attributes. May be used to store various
	 * container data e.g. version identifier.
	 * <p>
	 * Should not return <code>null</code>.
	 * </p>
	 * 
	 * @return Map<String, String> - collection of attributes
	 */
	public Map<String, String> getAttributes();
}
