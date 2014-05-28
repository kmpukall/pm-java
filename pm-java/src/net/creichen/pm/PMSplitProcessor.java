/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

*******************************************************************************/

package net.creichen.pm;




import net.creichen.pm.steps.PMSplitStep;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.NullChange;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.RefactoringParticipant;
import org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor;
import org.eclipse.ltk.core.refactoring.participants.SharableParticipants;


public class PMSplitProcessor extends RefactoringProcessor implements PMProcessor {

	ICompilationUnit _iCompilationUnit;
	ITextSelection _textSelection;
	

	PMSplitStep _step;
	
	public PMSplitProcessor(ITextSelection selection, ICompilationUnit iCompilationUnit) {
		_textSelection = selection;
		_iCompilationUnit = iCompilationUnit;	
	}
	
	public ICompilationUnit getICompilationUnit() {
		return _iCompilationUnit;
	}
	

	
	
	
	public RefactoringStatus checkFinalConditions(IProgressMonitor pm,
			CheckConditionsContext context) throws CoreException,
			OperationCanceledException {
		return new RefactoringStatus();
	}

	@Override
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm)
			throws CoreException, OperationCanceledException {
		
		PMProject project = PMWorkspace.sharedWorkspace().projectForIJavaProject(_iCompilationUnit.getJavaProject());
		
		
		if (!project.sourcesAreOutOfSync()) {
			
			project.syncSources();
			
			ASTNode selectedNode = project.nodeForSelection(_textSelection, _iCompilationUnit);
			
				
			//We expect the selected node to be an ExpressionStatement with an Assignment as the expression.
			//If an Assignment inside an ExpressionStatement is selected, we fix up the selectednode to be the ExpressionStatement.

			
			
			if (selectedNode instanceof Assignment) {
				Assignment assignment = (Assignment)selectedNode;
				
				if (assignment.getParent() instanceof ExpressionStatement) {
					selectedNode = assignment.getParent();
				}
			}
			
			if (selectedNode instanceof ExpressionStatement) {
				
				
				
				
				ExpressionStatement assignmentStatement = (ExpressionStatement)selectedNode;
				
				
				
				
				if (assignmentStatement.getExpression() instanceof Assignment) {
					Assignment assignmentExpression = (Assignment)assignmentStatement.getExpression();
				
					if (assignmentExpression.getLeftHandSide() instanceof SimpleName) {
						
						SimpleName name = (SimpleName)assignmentExpression.getLeftHandSide();
						
						VariableDeclaration declaration = PMASTNodeUtils.localVariableDeclarationForSimpleName(name);
						
						if (declaration != null && PMASTNodeUtils.variableDeclarationIsLocal(declaration)) {
							_step = new PMSplitStep(project, (ExpressionStatement)selectedNode);
							
							return new RefactoringStatus();
						}
							
					}				
				}
			} 
				
			return RefactoringStatus.createFatalErrorStatus("Split temporary can only be run on an assignment to a local variable.");
			
			
		} else
			return RefactoringStatus.createWarningStatus("PM Model is out of date. This will reinitialize.");

	}

	

	
	@Override
	public Change createChange(IProgressMonitor pm) throws CoreException,
			OperationCanceledException {
	
		Change result = new NullChange();
		
			
		result = _step.createCompositeChange("Split");
		
		
				
		return result;
	}

	@Override
	public Object[] getElements() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getIdentifier() {
		return "edu.colorado.plan.PMSplitTemporaryRefactoring";
	}

	@Override
	public String getProcessorName() {
		return "PMSplitProcessor";
	}

	@Override
	public boolean isApplicable() throws CoreException {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public RefactoringParticipant[] loadParticipants(RefactoringStatus status,
			SharableParticipants sharedParticipants) throws CoreException {
		return new RefactoringParticipant[0];
	}
	
	public void textChangeWasApplied() {
		//this is after the text change was applied but before the model
		//has sync'd itself to the new text
		
		_step.performASTChange();
		
		
	}
	
	public void textChangeWasNotApplied() {
		_step.cleanup();
	}
	

}
