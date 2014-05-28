/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

*******************************************************************************/

package net.creichen.pm.refactorings;

import net.creichen.pm.PMProject;
import net.creichen.pm.steps.PMRenameStep;
import net.creichen.pm.steps.PMSplitStep;

import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;


// This class will apply the Split Temporary Refactoring
// on an assignment statement by first applying the PMSplit step
// and then the PMRename step


public class PMSplitTemporaryRefactoring {

	PMProject _project;
	
	PMSplitStep _splitStep;
	
	PMRenameStep _renameStep;
	
	String _newVariableName;
	
	public PMSplitTemporaryRefactoring(PMProject project, ExpressionStatement assignmentExpressionStatement, String newVariableName) {
		
		_project = project;
		
		_splitStep = new PMSplitStep(project, assignmentExpressionStatement);
		
		_newVariableName = newVariableName;
	}
	
	
	public void apply() {
		
		
		_splitStep.applyAllAtOnce();
		
		//now find the name for the new declaration and rename it
		
		VariableDeclarationStatement newlyCreatedDeclaration = _splitStep.getReplacementDeclarationStatement();
		
		SimpleName simpleNameToRename = ((VariableDeclarationFragment)newlyCreatedDeclaration.fragments().get(0)).getName();
		
		_renameStep = new PMRenameStep(_project, simpleNameToRename);
		
		_renameStep.setNewName(_newVariableName);
		
		_renameStep.applyAllAtOnce();
		
		
	}
}
