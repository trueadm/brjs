package org.bladerunnerjs.utility.deps;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.bladerunnerjs.api.Aspect;
import org.bladerunnerjs.api.LinkedAsset;
import org.bladerunnerjs.api.SourceModule;
import org.bladerunnerjs.api.model.exception.ModelOperationException;
import org.bladerunnerjs.api.model.exception.RequirePathException;
import org.bladerunnerjs.api.model.exception.request.ContentFileProcessingException;
import org.bladerunnerjs.model.BrowsableNode;
import org.bladerunnerjs.model.BundlableNode;
import org.bladerunnerjs.model.BladeWorkbench;
import org.bladerunnerjs.plugin.bundlers.aliasing.AliasAsset;
import org.bladerunnerjs.plugin.bundlers.aliasing.AliasDefinition;
import org.bladerunnerjs.plugin.bundlers.aliasing.AliasDefinitionsFile;
import org.bladerunnerjs.plugin.bundlers.aliasing.AliasException;
import org.bladerunnerjs.plugin.bundlers.aliasing.AliasOverride;
import org.bladerunnerjs.plugin.bundlers.aliasing.AliasesFile;

public class DependencyGraphReportBuilder {
	private final List<LinkedAsset> linkedAssets;
	private final boolean showAllDependencies;
	private final DependencyInfo dependencyInfo;
	private final Set<LinkedAsset> manyLinkedAssets;
	private final StringBuilder reportBuilder;
	private final MutableBoolean hasOmittedDependencies;
	
	public static String createReport(BundlableNode bundlableNode, boolean showAllDependencies) throws ModelOperationException {
		fixIncompleteAliases(bundlableNode);
		
		return "Bundle '" + bundlableNode.root().dir().getRelativePath(bundlableNode.dir()) + "' dependencies found:\n" +
			new DependencyGraphReportBuilder(bundlableNode.seedAssets(), DependencyInfoFactory.buildForwardDependencyMap(bundlableNode), showAllDependencies).createReport();
	}
	
	public static String createReport(Aspect aspect, boolean showAllDependencies) throws ModelOperationException {
		fixIncompleteAliases(aspect);
		
		return "Aspect '" + aspect.getName() + "' dependencies found:\n" +
			new DependencyGraphReportBuilder(aspect.seedAssets(), DependencyInfoFactory.buildForwardDependencyMap(aspect), showAllDependencies).createReport();
	}
	
	public static String createReport(BladeWorkbench workbench, boolean showAllDependencies) throws ModelOperationException {
		fixIncompleteAliases(workbench);
		
		return "Workbench dependencies found:\n" +
			new DependencyGraphReportBuilder(workbench.seedAssets(), DependencyInfoFactory.buildForwardDependencyMap(workbench), showAllDependencies).createReport();
	}
	
	public static String createReport(BrowsableNode browsableNode, String requirePath, boolean showAllDependencies) throws ModelOperationException {
		try {
			fixIncompleteAliases(browsableNode);
			
			SourceModule sourceModule =  (SourceModule)browsableNode.getLinkedAsset(requirePath);
			List<LinkedAsset> linkedAssets = new ArrayList<>();
			linkedAssets.add(sourceModule);
			
			return "Source module '" + sourceModule.getPrimaryRequirePath() + "' dependencies found:\n" +
			new DependencyGraphReportBuilder(linkedAssets, DependencyInfoFactory.buildReverseDependencyMap(browsableNode, sourceModule), showAllDependencies).createReport();
		}
		catch(RequirePathException e) {
			return e.getMessage();
		}
	}
	
	public static String createReportForRequirePrefix(BrowsableNode browsableNode, String requirePathPrefix, boolean showAllDependencies) throws ModelOperationException {
		List<LinkedAsset> linkedAssets = new ArrayList<>();
		
		fixIncompleteAliases(browsableNode);
		
		for(SourceModule sourceModule : browsableNode.getBundleSet().getSourceModules()) {
			if(sourceModule.getPrimaryRequirePath().startsWith(requirePathPrefix)) {
				linkedAssets.add(sourceModule);
			}
		}
		
		return "Require path prefix '" + requirePathPrefix + "' dependencies found:\n" +
		new DependencyGraphReportBuilder(linkedAssets, DependencyInfoFactory.buildReverseDependencyMap(browsableNode, null), showAllDependencies).createReport();
	}
	
	public static String createReportForAlias(BrowsableNode browsableNode, String aliasName, boolean showAllDependencies) throws ModelOperationException {
		try {
			List<LinkedAsset> linkedAssets = new ArrayList<>();
			
			fixIncompleteAliases(browsableNode);
			
			AliasDefinition alias = browsableNode.getAlias(aliasName);
			SourceModule sourceModule =  (SourceModule)browsableNode.getLinkedAsset(alias.getRequirePath());
			linkedAssets.add(sourceModule);
			
			return "Alias '" + aliasName + "' dependencies found:\n" +
			new DependencyGraphReportBuilder(linkedAssets, DependencyInfoFactory.buildReverseDependencyMap(browsableNode, sourceModule), showAllDependencies).createReport();
		}
		catch(AliasException | RequirePathException e) {
			return e.getMessage();
		}
		catch(ContentFileProcessingException e) {
			throw new ModelOperationException(e);
		}
	}
	
	private static void fixIncompleteAliases(BundlableNode bundlableNode) {
		try {
			AliasesFile aliasesFile = bundlableNode.aliasesFile();
			
			for(AliasDefinitionsFile aliasDefinitionFile : bundlableNode.aliasDefinitionFiles()) {
				for(AliasDefinition aliasDefinition : aliasDefinitionFile.aliases()) {
					if(!aliasesFile.hasAlias(aliasDefinition.getName()) && (aliasDefinition != null) && (aliasDefinition.getInterfaceName() != null)) {
						aliasesFile.addAlias(new AliasOverride(aliasDefinition.getName(), aliasDefinition.getInterfaceName()));
					}
				}
			}
		}
		catch(ContentFileProcessingException e) {
			throw new RuntimeException(e);
		}
	}
	
	private DependencyGraphReportBuilder(List<LinkedAsset> linkedAssets, DependencyInfo dependencyInfo, boolean showAllDependencies) throws ModelOperationException {
		this.linkedAssets = linkedAssets;
		this.dependencyInfo = dependencyInfo;
		this.showAllDependencies = showAllDependencies;
		
		manyLinkedAssets = determineManyLinkedAssets(new HashSet<LinkedAsset>());
		reportBuilder = new StringBuilder();
		hasOmittedDependencies = new MutableBoolean(false);
	}
	
	private String createReport() throws ModelOperationException {
		HashSet<LinkedAsset> processedAssets = new HashSet<>();
		for(LinkedAsset linkedAsset : linkedAssets) {
			addDependency(linkedAsset, null, processedAssets, 1);
		}
		
		if(!showAllDependencies && !manyLinkedAssets.isEmpty()) {
			reportBuilder.append("\n    (*) - subsequent instances not shown (use -A or --all to show)");
		}
		else if(showAllDependencies && hasOmittedDependencies.isTrue()) {
			reportBuilder.append("\n    (*) - dependencies omitted (listed previously)");
		}
		
		return reportBuilder.toString();
	}
	
	private Set<LinkedAsset> determineManyLinkedAssets(HashSet<LinkedAsset> processedAssets) {
		Set<LinkedAsset> manyLinkedAssets = new HashSet<>();
		for(LinkedAsset linkedAsset : linkedAssets) {
			buildManyLinkedAssets(linkedAsset, processedAssets, manyLinkedAssets);
		}
		
		return manyLinkedAssets;
	}
	
	private void buildManyLinkedAssets(LinkedAsset linkedAsset, Set<LinkedAsset> processedAssets, Set<LinkedAsset> manyLinkedAssets) {
		List<LinkedAsset> assetDependencies = getDependencies(linkedAsset);
		if(processedAssets.add(linkedAsset)) {
			for(LinkedAsset dependentAsset : assetDependencies) {
				buildManyLinkedAssets(dependentAsset, processedAssets, manyLinkedAssets);
			}
		}
		else {
			manyLinkedAssets.add(linkedAsset);
		}
	}
	
	private void addDependency(LinkedAsset linkedAsset, LinkedAsset referringAsset, Set<LinkedAsset> processedAssets, int indentLevel) throws ModelOperationException {
		if(showAllDependencies || !processedAssets.contains(linkedAsset)) {
			appendAssetPath(linkedAsset, referringAsset, indentLevel, processedAssets.contains(linkedAsset));
		}
		
		List<LinkedAsset> assetDependencies = getDependencies(linkedAsset);
		if(processedAssets.add(linkedAsset)) {
			for(LinkedAsset dependentAsset : assetDependencies) {
				addDependency(dependentAsset, linkedAsset, processedAssets, indentLevel + 1);
			}
		}
		else if(assetDependencies.size() > 0) {
			hasOmittedDependencies.setValue(true);
		}
	}
	
	private void appendAssetPath(LinkedAsset linkedAsset, LinkedAsset referringAsset, int indentLevel, boolean alreadyProcessedDependency) {
		reportBuilder.append("    ");
		
		if(indentLevel == 1) {
			reportBuilder.append("+--- ");
		}
		else {
			for(int i = 0; i < indentLevel; ++i) {
				if(i == (indentLevel - 1)) {
					reportBuilder.append("\\--- ");
				}
				else {
					reportBuilder.append("|    ");
				}
			}
		}
		
		reportBuilder.append("'" + linkedAsset.getAssetPath() + "'");
		
		if(dependencyInfo.seedAssets.contains(linkedAsset)) {
			reportBuilder.append(" (seed file)");
		}
		else if((dependencyInfo.staticDeps.get(referringAsset) != null) && dependencyInfo.staticDeps.get(referringAsset).contains(linkedAsset)) {
			reportBuilder.append(" (static dep.)");
		}
		else if(dependencyInfo.resourceAssets.contains(linkedAsset)) {
			reportBuilder.append(" (implicit resource)");
		}
		else if(linkedAsset instanceof AliasAsset) {
			reportBuilder.append(" (alias dep.)");
		}
		
		if((showAllDependencies && alreadyProcessedDependency) || (!showAllDependencies && manyLinkedAssets.contains(linkedAsset))) {
			reportBuilder.append(" (*)");
		}
		
		reportBuilder.append("\n");
	}
	
	private List<LinkedAsset> getDependencies(LinkedAsset linkedAsset) {
		List<LinkedAsset> dependencies =  new ArrayList<>();
		
		if(dependencyInfo.map.containsKey(linkedAsset)) {
			dependencies.addAll(dependencyInfo.map.get(linkedAsset));
		}
		
		return dependencies;
	}
}
