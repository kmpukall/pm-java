/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

*******************************************************************************/

package net.creichen.pm.steps;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.creichen.pm.PMNodeReference;
import net.creichen.pm.PMPasteboard;
import net.creichen.pm.PMProject;
import net.creichen.pm.analysis.PMRDefsAnalysis;
import net.creichen.pm.models.PMNameModel;
import net.creichen.pm.models.PMUDModel;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.ASTMatcher;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

public class PMCopyStep extends PMStep {
	List<ASTNode> _selectedNodes;
	
	public PMCopyStep(PMProject project, List<ASTNode> selectedNodes) {
		super(project);
		
		initWithSelectedNodes(selectedNodes);
	}
	
	public PMCopyStep(PMProject project, ASTNode node) {
		super(project);
		
		List<ASTNode> selectedNodes = new ArrayList<ASTNode>();
		
		selectedNodes.add(node);
		
		initWithSelectedNodes(selectedNodes);
	}
	
	private void initWithSelectedNodes(List<ASTNode> selectedNodes) {
		_selectedNodes = selectedNodes;
	}
	
	
//need method to test for errors before asking for changes
	
	public Map<ICompilationUnit, ASTRewrite> calculateTextualChange() {
		Map<ICompilationUnit, ASTRewrite> result = new HashMap<ICompilationUnit, ASTRewrite>();	
		
		return result;
	}
	
	
	public void copyNameModel(List<ASTNode> originalRootNodes, List<ASTNode> copiedRootNodes) {
		/* Generate fresh identifiers for all copied declarations (and definitions) and
		 * keep a mapping from the original identifiers to the new ones so we can later
		 * fix up references. We have to do this in two passes because a reference may come before
		 * a declaration in the AST.
		 */
		
		final PMNameModel nameModel = _project.getNameModel();
		
		final Map<String, String> copyNameIdentifiersForOriginals = new HashMap<String, String>();
		
		ASTMatcher matcher = new ASTMatcher() {
			public boolean match(SimpleName originalName, Object copyNameObject) {
				SimpleName copyName = (SimpleName)copyNameObject;
				
				if (_project.nameNodeIsDeclaring(originalName)) {
					//Generate fresh identifier for node with name model
					
					String freshNameModelIdentifier = nameModel.generateNewIdentifierForName(copyName);
					nameModel.setIdentifierForName(freshNameModelIdentifier, copyName);
					
					copyNameIdentifiersForOriginals.put(nameModel.identifierForName(originalName), freshNameModelIdentifier);
				} else {
					nameModel.setIdentifierForName(nameModel.identifierForName(originalName), copyName);
				}
							
				return true;
			}
		};
		
		for (int i = 0; i < originalRootNodes.size(); i++) {
			ASTNode original = originalRootNodes.get(i);
			ASTNode copy = copiedRootNodes.get(i);
			
			original.subtreeMatch(matcher, copy);
			
			
		}
		
		
		/* Now that we've generated fresh identifiers for all copied declarations, we go through and fix up references
		 * to the old identifiers in the copies to now point to the new identifiers
		 */
		
		ASTVisitor fixupReferenceVisitor = new ASTVisitor() {
			@Override public boolean visit(SimpleName name) {
				String nameIdentifier = nameModel.identifierForName(name);
				
				if (copyNameIdentifiersForOriginals.containsKey(nameIdentifier))
					nameModel.setIdentifierForName(copyNameIdentifiersForOriginals.get(nameIdentifier), name);
				return true;
			}
		};
		
		for (ASTNode copiedPasteboardRoot : copiedRootNodes) {						
			copiedPasteboardRoot.accept(fixupReferenceVisitor);
		}
	}
	
	public void copyUDModel(List<ASTNode> originalRootNodes, List<ASTNode> copiedRootNodes) {
		//find all definitions in the copy
		//and keep a mapping from the new definition to the old
		
		//find all uses in the copy and keep a mapping from the new use to the old
		
		//for each each in the copy, if it's definition is internal, 
		
		final PMUDModel udModel = _project.getUDModel();
		
		final Map<ASTNode, ASTNode> originalUsingNodesForCopiedUsingNodes = new HashMap<ASTNode, ASTNode>();
		final Map<ASTNode, ASTNode> copiedUsingNodesForOriginalUsingNodes = new HashMap<ASTNode, ASTNode>();
		
		ASTMatcher nameMatcher = new ASTMatcher() {
			public boolean match(SimpleName originalName, Object copyNameObject) {
				SimpleName copyName = (SimpleName)copyNameObject;
				
				if (udModel.nameIsUse(originalName)) {
					originalUsingNodesForCopiedUsingNodes.put(copyName, originalName);
					copiedUsingNodesForOriginalUsingNodes.put(originalName, copyName);
				}
				
				return true;
			}
		};
		
		
		
		Map<ASTNode, ASTNode> originalDefiningNodesForCopiedDefiningNodes = new HashMap<ASTNode, ASTNode>();
		Map<ASTNode, ASTNode> copiedDefiningNodesForCopiedOriginalDefiningNodes = new HashMap<ASTNode, ASTNode>();
		
		for (int rootNodeIndex = 0; rootNodeIndex < originalRootNodes.size(); rootNodeIndex++) {
			ASTNode originalRootNode = originalRootNodes.get(rootNodeIndex);
			ASTNode copyRootNode = copiedRootNodes.get(rootNodeIndex);
			
			List<ASTNode> originalDefiningNodes = PMRDefsAnalysis.findDefiningNodesUnderNode(originalRootNode);
			List<ASTNode> copyDefiningNodes = PMRDefsAnalysis.findDefiningNodesUnderNode(copyRootNode);
			
			for (int definingNodeIndex = 0; definingNodeIndex < originalDefiningNodes.size(); definingNodeIndex++) {
				ASTNode originalDefiningNode = originalDefiningNodes.get(definingNodeIndex);
				ASTNode copyDefiningNode = copyDefiningNodes.get(definingNodeIndex);
				
				originalDefiningNodesForCopiedDefiningNodes.put(copyDefiningNode, originalDefiningNode);
				copiedDefiningNodesForCopiedOriginalDefiningNodes.put(originalDefiningNode, copyDefiningNode);
			}
			
			originalRootNode.subtreeMatch(nameMatcher, copyRootNode);
			
		}
		
		
		/* Now that we have the mappings:
		 * 
		 * For each copied definition, find the original definition and get the original uses for it
		 * 		for each original use, 
		 * 				if it is external add it as a use for the copy
		 * 				if it is internal, generate a new identifier for the copy use and add it to the uses for the copied definition
		 */
		
	
		for (ASTNode copiedDefinition  : originalDefiningNodesForCopiedDefiningNodes.keySet()) {
			ASTNode originalDefinition = originalDefiningNodesForCopiedDefiningNodes.get(copiedDefinition);
			
			Set<PMNodeReference> originalUses = udModel.usesForDefinition(_project.getReferenceForNode(originalDefinition));
			
			Set<PMNodeReference> copyUses =  udModel.usesForDefinition(_project.getReferenceForNode(copiedDefinition));
			
			for (PMNodeReference originalUseReference : originalUses) {
				ASTNode originalUseNode = originalUseReference.getNode();
				
				ASTNode copyUseNode = copiedUsingNodesForOriginalUsingNodes.get(originalUseNode);
				
				if (copyUseNode != null) { /* use is internal */
					copyUses.add(_project.getReferenceForNode(copyUseNode));
				} else { /*Use is external, so the original reference is fine */
					copyUses.add(originalUseReference);
				}
			}		
		}
		
		for (ASTNode copiedUse  : originalUsingNodesForCopiedUsingNodes.keySet()) {
			ASTNode originalUse = originalUsingNodesForCopiedUsingNodes.get(copiedUse);
			
			Set<PMNodeReference> originalDefinitions = udModel.definitionIdentifiersForName(_project.getReferenceForNode(originalUse));
			
			Set<PMNodeReference> copyDefinitions =  udModel.definitionIdentifiersForName(_project.getReferenceForNode(copiedUse));
			
			for (PMNodeReference originalDefinitionReference : originalDefinitions) {
				ASTNode originalDefinitionNode = originalDefinitionReference.getNode();
				
				ASTNode copyDefinitionNode = copiedDefiningNodesForCopiedOriginalDefiningNodes.get(originalDefinitionNode);
				
				if (copyDefinitionNode != null) { /* def is internal */
					copyDefinitions.add(_project.getReferenceForNode(copyDefinitionNode));
				} else { /*Use is external, so the original reference is fine */
					copyDefinitions.add(originalDefinitionReference);
				}
			}		
		}
		
		
	}
	
	public void performASTChange() {
		
	
		
		
		
		List<ASTNode> copiedPasteboardRootNodes = new ArrayList<ASTNode>();
		
		for (ASTNode original : _selectedNodes) {
			ASTNode copy = ASTNode.copySubtree(original.getAST(), original);
			
			copiedPasteboardRootNodes.add(copy);
		}
		
		copyNameModel(_selectedNodes, copiedPasteboardRootNodes);
		
		copyUDModel(_selectedNodes, copiedPasteboardRootNodes);
		
		
		
		PMPasteboard pasteboard = _project.getPasteboard();
		
		
		pasteboard.setPasteboardRoots(copiedPasteboardRootNodes);
	}
	
	public void updateAfterReparse() {
		
	}
	
	
	public void cleanup() {
		
	}

	
}