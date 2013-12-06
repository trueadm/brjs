package org.bladerunnerjs.core.plugin.content;

import java.io.OutputStream;
import java.util.List;

import org.bladerunnerjs.core.plugin.Plugin;
import org.bladerunnerjs.model.BundleSet;
import org.bladerunnerjs.model.ParsedContentPath;
import org.bladerunnerjs.model.exception.request.BundlerProcessingException;
import org.bladerunnerjs.model.utility.ContentPathParser;

public interface ContentPlugin extends Plugin {
	String getRequestPrefix();
	String getMimeType();
	ContentPathParser getContentPathParser();
	void writeContent(ParsedContentPath contentPath, BundleSet bundleSet, OutputStream os) throws BundlerProcessingException;
	List<String> getValidDevRequestPaths(BundleSet bundleSet, String locale) throws BundlerProcessingException;
	List<String> getValidProdRequestPaths(BundleSet bundleSet, String locale) throws BundlerProcessingException;
}
