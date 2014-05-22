/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

*******************************************************************************/

package pm_refactoring.refactorings;

import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import pm_refactoring.PMNodeReference;
import pm_refactoring.PMProject;
import pm_refactoring.steps.PMCutStep;
import pm_refactoring.steps.PMPasteStep;


//We move a field by PMCutting it from its old parent and PMPasteing it in its new parent

public class PMMoveFieldRefactoring {
	PMProject _project;
	
	PMNodeReference _fieldReference;
	
	PMNodeReference _newParentReference;
	
	public PMMoveFieldRefactoring(PMProject project, FieldDeclaration fieldDeclaration, TypeDeclaration newParent) {
		_project = project;
		
		_fieldReference = _project.getReferenceForNode(fieldDeclaration);
		
		
		_newParentReference = _project.getReferenceForNode(newParent);
	}
 
	public void apply() {
		PMCutStep cutStep = new PMCutStep(_project, _fieldReference.getNode());
		
	
		cutStep.applyAllAtOnce();
		
		//race here? Will _fieldReference go away if we call gc?
		//NO: since the field is held in the pasteboard
		//But otherwise would be a problem
		//So: should node store hold strong refs to ast nodes???
		
		
		TypeDeclaration newParent = (TypeDeclaration)_newParentReference.getNode();
		
		PMPasteStep pasteStep = new PMPasteStep(_project, newParent, TypeDeclaration.BODY_DECLARATIONS_PROPERTY, newParent.bodyDeclarations().size());
	
		pasteStep.applyAllAtOnce();
	}
}
