package org.bladerunnerjs.utility.trie.node;


public class OptimisedTrieTrunkLeafNode<T> extends AbstractOptimisedTrieNode<T>
{
	
 	private char character;
 	private T value;
	private TrieNode<T>[] children;
 	
 	public OptimisedTrieTrunkLeafNode(char character, T value, TrieNode<T>[] children)
 	{
 		this.character = character;
 		this.children = children;
 	}
 	
 	@Override
	public TrieNode<T> getNextNode(char character)
	{
 		return getNextNode(children, character);
	}

 	@Override
 	public char getChar()
 	{
 		return character;
 	}
 	
    @Override
    public T getValue()
    {
    	return value;
    }
    
}
