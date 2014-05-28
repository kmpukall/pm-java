/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

*******************************************************************************/

package net.creichen.pm.steps;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.creichen.pm.PMPasteboard;
import net.creichen.pm.PMProject;
import net.creichen.pm.models.PMNameModel;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.ASTMatcher;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ChildListPropertyDescriptor;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

public class PMPasteStep extends PMStep {
	
	ASTNode _parent;
	
	
	ChildListPropertyDescriptor _property;
	int _index;
	
	public PMPasteStep(PMProject project, ASTNode parent, ChildListPropertyDescriptor property, int index) {
		super(project);
		
		_parent = parent;
		_property = property;
		
		_index = index;
	}
	
	
	public Map<ICompilationUnit, ASTRewrite> calculateTextualChange() {
		
		PMPasteboard pasteboard = _project.getPasteboard();
		
		List<ASTNode> nodesToPaste = pasteboard.getPasteboardRoots();
		
		Map<ICompilationUnit, ASTRewrite> result = new HashMap<ICompilationUnit, ASTRewrite>();
		
				
		ASTRewrite astRewrite = ASTRewrite.create(_parent.getAST());
		
		int index = _index;
		
		
		for (ASTNode nodeToPaste: nodesToPaste) {
			ASTNode copiedNode = (ASTNode)ASTNode.copySubtree(_parent.getAST(), nodeToPaste);
			
			ListRewrite lrw = astRewrite.getListRewrite(_parent, _property);
			lrw.insertAt(copiedNode, index++, null /*textEditGroup*/);		
			
			result.put(_project.findPMCompilationUnitForNode(_parent).getICompilationUnit(), astRewrite);
		}
		
		
		return result;
	}
	
	
	public void performASTChange() {
		
		PMPasteboard pasteboard = _project.getPasteboard();
		
		List<ASTNode> nodesToPaste = pasteboard.getPasteboardRoots();
		
				
				
		final PMNameModel nameModel = _project.getNameModel();
		
		for (int i = 0; i < nodesToPaste.size(); i++) {
			ASTNode node = nodesToPaste.get(i);
						int insertionIndex = i + _index;
			
			
			List childList = (List)_parent.getStructuralProperty(_property);
			
			ASTNode copiedNode = ASTNode.copySubtree(_parent.getAST(), node);
			childList.add(insertionIndex, copiedNode);
			
			
			
			
			
			ASTMatcher identifierMatcher = new ASTMatcher() {

				public boolean match(SimpleName pasteboardName, Object other) {
					if (super.match(pasteboardName, other)) {
						
						SimpleName copyName = (SimpleName)other;
						
						String identifier = nameModel.identifierForName(pasteboardName);
						
						//System.out.println("Identifier for " + copyName + " is " + identifier);
						
						nameModel.setIdentifierForName(identifier, copyName);
												
						return true;
					} else
						return false;
				}
			};

			
			
			if  (node.subtreeMatch(identifierMatcher, copiedNode)) {
				_project.recursivelyReplaceNodeWithCopy(node, copiedNode);
				
				
			
			} else {
				System.err.println("Couldn't match copied statement to original");
				
				throw new RuntimeException("PM Paste Error: Couldn't match copied statement to original");
			}
		}
		
		//FIXME(dcc) is this update necessary?
		_project.updateToNewVersionsOfICompilationUnits();
	}
	
	public void updateAfterReparse() {
		
	}
	
	
	public void cleanup() {
		//called regardless of whether updateAfterReparse() was called
	}

	
}
