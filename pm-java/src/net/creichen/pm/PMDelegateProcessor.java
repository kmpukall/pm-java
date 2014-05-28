/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm;

import net.creichen.pm.steps.PMDelegateStep;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.ICompilationUnit;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.MethodInvocation;

import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.NullChange;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.RefactoringParticipant;
import org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor;
import org.eclipse.ltk.core.refactoring.participants.SharableParticipants;

public class PMDelegateProcessor extends RefactoringProcessor implements
		PMProcessor, PMProjectListener {

	ICompilationUnit _iCompilationUnit;
	ITextSelection _textSelection;

	String _delegateIdentifier;

	PMDelegateStep _step;

	public PMDelegateProcessor(ITextSelection selection,
			ICompilationUnit iCompilationUnit) {
		_textSelection = selection;
		_iCompilationUnit = iCompilationUnit;
	}

	public ICompilationUnit getICompilationUnit() {
		return _iCompilationUnit;
	}

	public void setDelegateIdentifier(String delegateIdentifier) {
		_delegateIdentifier = delegateIdentifier;
	}

	public RefactoringStatus checkFinalConditions(IProgressMonitor pm,
			CheckConditionsContext context) throws CoreException,
			OperationCanceledException {
		return new RefactoringStatus();
	}

	@Override
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm)
			throws CoreException, OperationCanceledException {
		PMProject project = PMWorkspace.sharedWorkspace()
				.projectForIJavaProject(_iCompilationUnit.getJavaProject());

		if (!project.sourcesAreOutOfSync()) {
			ASTNode selectedNode = project.nodeForSelection(_textSelection,
					_iCompilationUnit);

			if (selectedNode instanceof MethodInvocation) {
				return new RefactoringStatus();
			} else
				return RefactoringStatus
						.createFatalErrorStatus("Please select a method invocation [not a "
								+ selectedNode.getClass() + "]");
		} else
			return RefactoringStatus
					.createWarningStatus("PM Model is out of date. This will reinitialize.");

	}

	@Override
	public Change createChange(IProgressMonitor pm) throws CoreException,
			OperationCanceledException {

		PMProject project = PMWorkspace.sharedWorkspace()
				.projectForIJavaProject(_iCompilationUnit.getJavaProject());

		project.syncSources();

		Change result = new NullChange();

		ASTNode selectedNode = project.nodeForSelection(_textSelection,
				_iCompilationUnit);

		_step = new PMDelegateStep(project, selectedNode);

		_step.setDelegateIdentifier(_delegateIdentifier);

		result = _step.createCompositeChange("Delegate");

		return result;
	}

	@Override
	public Object[] getElements() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getIdentifier() {
		return "edu.colorado.plan.PMRDelegateRefactoring";
	}

	@Override
	public String getProcessorName() {
		return "PMDelegateRefactoring";
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
		// this is after the text change was applied but before the model
		// has sync'd itself to the new text

		_step.performASTChange();

		PMProject project = PMWorkspace.sharedWorkspace()
				.projectForIJavaProject(_iCompilationUnit.getJavaProject());

		project.addProjectListener(this);
	}

	public void textChangeWasNotApplied() {

		_step.cleanup();

	}

	public void projectDidReparse(PMProject project) {
		_step.updateAfterReparse();
		_step.cleanup();
	}

}
