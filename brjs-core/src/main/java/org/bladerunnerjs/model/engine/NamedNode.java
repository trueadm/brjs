package org.bladerunnerjs.model.engine;

import javax.naming.InvalidNameException;

public interface NamedNode extends Node
{
	String UNDEFINED_NODE_NAME = "undefined";
	
	String getName();
	boolean isValidName();
	String getTypeName();
	void assertValidName() throws InvalidNameException;
}