package org.bladerunnerjs.model.aliasing;

public class UnresolvableAliasException extends AliasException {
	private static final long serialVersionUID = 1L;
	
	public UnresolvableAliasException(AliasesFile aliasesFile, AliasName aliasName, String scenarioName) {
		super("Alias '" + aliasName.getName() + "' for scenario '" + scenarioName + "' has multiple definitions for aliases file 'aliasesFile', or other " + aliasesFile.getPath() + " files that it inherits from");
	}
}
