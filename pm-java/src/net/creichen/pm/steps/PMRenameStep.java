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





import net.creichen.pm.PMCompilationUnit;
import net.creichen.pm.PMProject;
import net.creichen.pm.models.PMNameModel;

import org.eclipse.jdt.core.ICompilationUnit;




import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;






public class PMRenameStep extends PMStep {
	SimpleName _nameNode;
	
	String _newName;
	
	
	List<SimpleName> _nameNodesToChange;
	
	public PMRenameStep(PMProject project, SimpleName nameNode) {
		super(project);
		
		if (nameNode != null) {
			_nameNode = nameNode;
			_nameNodesToChange = new ArrayList<SimpleName>();
		} else
			throw new RuntimeException("Cannot create PMRenameStep with null nameNode");
		
		
		
	}
	
	
	public String getNewName() {
		return _newName;
	}
	
	public void setNewName(String newName) {
		_newName = newName;
	}
	
//need method to test for errors before asking for changes
	
	public Map<ICompilationUnit, ASTRewrite> calculateTextualChange() {
		Map<ICompilationUnit, ASTRewrite> result = new HashMap<ICompilationUnit, ASTRewrite>();
		
				
		_project.syncSources();
		
		PMNameModel nameModel = _project.getNameModel();
						
		ArrayList<SimpleName> nodesToRename = nameModel.nameNodesRelatedToNameNode(_nameNode);
				
		_nameNodesToChange.addAll(nodesToRename);
				
		HashMap<ICompilationUnit, List<SimpleName>>  nodesByICompilationUnit = new HashMap<ICompilationUnit, List<SimpleName>>();
		
		 for (SimpleName nodeToRename : nodesToRename) {
			 CompilationUnit containingCompilationUnit = (CompilationUnit)nodeToRename.getRoot();
			 
			 ICompilationUnit containingICompilationUnit = (ICompilationUnit)containingCompilationUnit.getJavaElement();
		
			 List<SimpleName> namesForUnit = nodesByICompilationUnit.get(containingICompilationUnit);
			 
			 if (namesForUnit == null) {
				 namesForUnit = new ArrayList<SimpleName>();
				 nodesByICompilationUnit.put(containingICompilationUnit, namesForUnit);
			 }
			 
			 namesForUnit.add(nodeToRename);
		 }
		 
		 if (nodesByICompilationUnit.size() > 0) {				
			 for (ICompilationUnit unitForRename: nodesByICompilationUnit.keySet()) {
				 
				
				 List<SimpleName> nodesInUnit = nodesByICompilationUnit.get(unitForRename);
				 
				 ASTRewrite astRewrite = ASTRewrite.create(nodesInUnit.get(0).getAST());
				 
				 result.put(unitForRename, astRewrite);
				 
				 
				for (SimpleName sameNode  : nodesInUnit) {
					
					SimpleName newNode = _nameNode.getAST().newSimpleName(_newName);
					
					astRewrite.replace(sameNode, newNode, null);
					
				}
			 }	 
		 }
				 
		 return result;
	}
	
	
	public void performASTChange() {
		for (SimpleName nameNode : _nameNodesToChange) {
			nameNode.setIdentifier(_newName);
				
			
			//Need to rename file if this is the name of a class and it is the highest level class in the compilation unit
			if (nameNode.getParent() instanceof TypeDeclaration && nameNode.getParent().getParent() instanceof CompilationUnit) {
				ICompilationUnit iCompilationUnitToRename = (ICompilationUnit)((CompilationUnit)nameNode.getParent().getParent()).getJavaElement();
				
				PMCompilationUnit pmCompilationUnitToRename = _project.getPMCompilationUnitForICompilationUnit(iCompilationUnitToRename);
				
				pmCompilationUnitToRename.rename(_newName);
				
				
				
				
			}
		}
		
		
	}
	
	public void updateAfterReparse() {
		
	}
	
	
	public void cleanup() {
		//called regardless of whether updateAfterReparse() was called
	}
}
