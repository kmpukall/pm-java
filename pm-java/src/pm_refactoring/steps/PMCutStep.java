/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

*******************************************************************************/

package pm_refactoring.steps;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.ICompilationUnit;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;


import pm_refactoring.PMPasteboard;
import pm_refactoring.PMProject;

public class PMCutStep extends PMStep {
	List<ASTNode> _selectedNodes;
	
	public PMCutStep(PMProject project, List<ASTNode> selectedNodes) {
		super(project);
		
		initWithSelectedNodes(selectedNodes);
	}
	
	public PMCutStep(PMProject project, ASTNode node) {
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
		
		ASTRewrite astRewrite = ASTRewrite.create(_selectedNodes.get(0).getAST());
		
		for (ASTNode node : _selectedNodes) {
			astRewrite.remove(node, null);
			
			result.put(_project.findPMCompilationUnitForNode(node).getICompilationUnit(), astRewrite);
		}
		
		
		
		return result;
	}
	
	
	public void performASTChange() {
		/*
		 * 
		 _project.setPasteboardRoot(_selectedNodes.get(0));
		
		PMCompilationUnitModel usingModel = _project.modelForASTNode(_selectedNodes.get(0));
		usingModel.removeIdentifiersForTreeStartingAtNode(_selectedNodes.get(0));
		
		*/

		PMPasteboard pasteboard = _project.getPasteboard();
		
		pasteboard.setPasteboardRoots(_selectedNodes);
		
		
		for (ASTNode node : _selectedNodes) {
			node.delete();
		}
		
	}
	
	public void updateAfterReparse() {
		
	}
	
	
	public void cleanup() {
		//called regardless of whether updateAfterReparse() was called
	}

	
}
