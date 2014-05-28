/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm;

import java.util.HashMap;

import net.creichen.pm.steps.PMRenameStep;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.RefactoringParticipant;
import org.eclipse.ltk.core.refactoring.participants.RenameProcessor;
import org.eclipse.ltk.core.refactoring.participants.SharableParticipants;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jface.text.ITextSelection;

public class PMRenameProcessor extends RenameProcessor implements PMProcessor {

	ICompilationUnit _iCompilationUnit;
	ITextSelection _textSelection;

	PMRenameStep _renameStep;

	String _newName;

	HashMap<SimpleName, String> _newIdentifersForSimpleNames;

	public PMRenameProcessor(ITextSelection selection,
			ICompilationUnit iCompilationUnit) {
		_textSelection = selection;
		_iCompilationUnit = iCompilationUnit;

		_newIdentifersForSimpleNames = new HashMap<SimpleName, String>();

	}

	public String getNewName() {
		return _newName;
	}

	public void setNewName(String newName) {
		_newName = newName;
	}

	public ICompilationUnit getICompilationUnit() {
		return _iCompilationUnit;
	}

	@Override
	public RefactoringStatus checkFinalConditions(IProgressMonitor pm,
			CheckConditionsContext context) throws CoreException,
			OperationCanceledException {

		return new RefactoringStatus();
	}

	@Override
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm)
			throws CoreException, OperationCanceledException {

		PMTimer.sharedTimer().start("STEP");

		PMProject project = PMWorkspace.sharedWorkspace()
				.projectForIJavaProject(_iCompilationUnit.getJavaProject());

		RefactoringStatus result = null;

		if (!project.sourcesAreOutOfSync()) {

			ASTNode selectedNode = project.nodeForSelection(_textSelection,
					_iCompilationUnit);

			if (selectedNode instanceof SimpleName) {

				result = new RefactoringStatus();
			} else
				result = RefactoringStatus
						.createFatalErrorStatus("Please select a name to use the Rename refactoring.");
		} else
			result = RefactoringStatus
					.createWarningStatus("PM Model is out of date. This will reinitialize.");

		PMTimer.sharedTimer().stop("STEP");

		return result;
	}

	@Override
	public Change createChange(IProgressMonitor pm) throws CoreException,
			OperationCanceledException {

		PMTimer.sharedTimer().start("STEP");

		PMProject project = PMWorkspace.sharedWorkspace()
				.projectForIJavaProject(_iCompilationUnit.getJavaProject());

		project.syncSources();

		_renameStep = new PMRenameStep(project,
				(SimpleName) project.nodeForSelection(_textSelection,
						_iCompilationUnit));

		_renameStep.setNewName(_newName);

		Change result = _renameStep.createCompositeChange("Rename");

		PMTimer.sharedTimer().stop("STEP");

		return result;
	}

	@Override
	public Object[] getElements() {
		return null;
	}

	@Override
	public String getIdentifier() {
		return "edu.colorado.plan.PMRenameRefactoring";
	}

	@Override
	public String getProcessorName() {
		return "PMRenameRefactoring";
	}

	@Override
	public boolean isApplicable() throws CoreException {
		return true;
	}

	@Override
	public RefactoringParticipant[] loadParticipants(RefactoringStatus status,
			SharableParticipants sharedParticipants) throws CoreException {

		// RefactoringParticipant[] result =
		// ParticipantManager.loadRenameParticipants(status, this,
		// ((SimpleName)_nodeToRename).resolveBinding().getJavaElement(), new
		// RenameArguments("foo", true), new String[]
		// {"org.eclipse.jdt.core.javanature"}, sharedParticipants);

		// return result;

		return new RefactoringParticipant[0];
	}

	public void textChangeWasApplied() {
		PMTimer.sharedTimer().start("STEP");

		_renameStep.performASTChange();

		PMTimer.sharedTimer().stop("STEP");
	}

	public void textChangeWasNotApplied() {

	}

}
