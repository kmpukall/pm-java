/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

*******************************************************************************/

package pm_refactoring;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.dom.ASTMatcher;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;

public class PMASTMatcher {

	ASTNode _oldNode;
	
	ASTNode _newNode;
	
	ASTMatcher _builtInMatcher;
	
	
	Map<ASTNode, ASTNode> _isomorphicNodes;
	
	List<ASTNode> _newlyAddedNodes;
	
	public PMASTMatcher(ASTNode oldNode, ASTNode newNode) {
		_builtInMatcher = new ASTMatcher();
		
		_isomorphicNodes = new HashMap<ASTNode,ASTNode>();
		
		_newlyAddedNodes = new ArrayList<ASTNode>();
		
		_newNode = newNode;
		_oldNode = oldNode;
	}
	
	
	
	public boolean match() {
		
		
		
		boolean result =  recursiveMatch(_oldNode, _newNode);
		
		if (!result) {
			_isomorphicNodes.clear();
			_newlyAddedNodes.clear();
		}
			
		
		return result;
	}
	
	public boolean recursiveMatch(ASTNode oldNode, ASTNode newNode) {
		
		//eventually we'll get smarter here and detect new nodes, etc.
                final int old_type = oldNode.getNodeType();
                final int new_type = newNode.getNodeType();

                if ((old_type == org.eclipse.jdt.core.dom.ASTNode.BLOCK_COMMENT
                     || old_type == org.eclipse.jdt.core.dom.ASTNode.LINE_COMMENT
                     || old_type == org.eclipse.jdt.core.dom.ASTNode.JAVADOC)
                    && (new_type == org.eclipse.jdt.core.dom.ASTNode.BLOCK_COMMENT
                        || new_type == org.eclipse.jdt.core.dom.ASTNode.LINE_COMMENT
                        || new_type == org.eclipse.jdt.core.dom.ASTNode.JAVADOC))
                        return true; // Nothing to do for comments, really

		if (oldNode instanceof MethodDeclaration && newNode instanceof MethodDeclaration) {
			
			MethodDeclaration oldMethodDeclaration = (MethodDeclaration)oldNode;
			MethodDeclaration newMethodDeclaration = (MethodDeclaration)newNode;
			
			//fixup method declarations
			//if the old declaration is a constructor and the new one is a method with the same name then
			//we set the new one to be a constructor too.
			//We do this because if we rename a class or constructor, the old ast will think the constructor
			//is a constructor while the new one will think it is just a method with a missing return type.
			
			if (oldMethodDeclaration.isConstructor()) {
				newMethodDeclaration.setConstructor(true);
			}
		} 
		
		List<StructuralPropertyDescriptor> oldStructuralProperties =  oldNode.structuralPropertiesForType();
		
		if (oldStructuralProperties.size() == newNode.structuralPropertiesForType().size()) {
			for (StructuralPropertyDescriptor structuralPropertyDescriptor : oldStructuralProperties) {
				
				
			
				Object oldPropertyValue = oldNode.getStructuralProperty(structuralPropertyDescriptor);
				
				Object newPropertyValue = newNode.getStructuralProperty(structuralPropertyDescriptor);
				
				if ((oldPropertyValue == null && newPropertyValue != null) || (newPropertyValue != null && newPropertyValue == null))
					return false;
				
				if (oldPropertyValue == null && newPropertyValue == null)
					 continue;
				else {
					//property values are ptr different and not null
					
					if (structuralPropertyDescriptor.isSimpleProperty()) {
						if (oldPropertyValue.equals(newPropertyValue))
							continue;
						else
							return false;
					} else if (structuralPropertyDescriptor.isChildProperty()) {
						if (recursiveMatch((ASTNode)oldPropertyValue, (ASTNode)newPropertyValue)) {
							
							
							continue;
						} else
							return false;
					} else if (structuralPropertyDescriptor.isChildListProperty()) {
						
						List<ASTNode> oldList = (List<ASTNode>)oldPropertyValue;
						
						List<ASTNode> newList = (List<ASTNode>)newPropertyValue;
						
						if (oldList.size() == newList.size()) {
							
							for (int i = 0; i < oldList.size(); i++) {
								ASTNode oldChildNode = oldList.get(i);
								
								ASTNode newChildNode = newList.get(i);
								
								if (recursiveMatch(oldChildNode, newChildNode))
									continue;
								else
									return false;
							}
							
						} else
							return false;
						
						
						
					} else {
						
						throw new RuntimeException("Unknown kind of structuralPropertyDescriptor");
					}
				}
				
				
			}
		} else
			return false;
		
		
			 
		//if we've gotten this far then all the structural properties match, so we
		//add to the isomorphism map
		
		_isomorphicNodes.put(oldNode, newNode);
	
		return true;
	}
	
	
	public List<ASTNode> newlyAddedNodes() {
		//We don't actually handle non isomorphic graphs yet, so this is always empty
		return _newlyAddedNodes;
	}
	
	
	public Map<ASTNode, ASTNode> isomorphicNodes() {
		return _isomorphicNodes;
	}
	
}
