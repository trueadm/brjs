package org.bladerunnerjs.legacy.command.test.testrunner;

import java.util.List;

import org.bladerunnerjs.api.Asset;
import org.bladerunnerjs.api.BundleSet;
import org.bladerunnerjs.api.SourceModule;
import org.bladerunnerjs.model.BundlableNode;

public class JsTestDriverBundleSet implements BundleSet {
	private BundleSet bundleSet;
	
	public JsTestDriverBundleSet(BundleSet bundleSet) {
		this.bundleSet = bundleSet;
	}
	
	public BundlableNode getBundlableNode() {
		return new JsTestDriverBundlableNode(bundleSet.getBundlableNode());
	}
	
	public List<SourceModule> getSourceModules() {
		return bundleSet.getSourceModules();
	}
	
	public List<Asset> getAssets() {
		return bundleSet.getAssets();
	}

	@Override
	public List<Asset> getAssetsWithRequirePrefix(String... prefixes)
	{
		return bundleSet.getAssetsWithRequirePrefix(prefixes);
	}
}
