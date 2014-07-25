/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm.refactorings;

import net.creichen.pm.api.PMProject;
import net.creichen.pm.api.PMWorkspace;
import net.creichen.pm.steps.RenameStep;
import net.creichen.pm.utils.Timer;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.RefactoringParticipant;
import org.eclipse.ltk.core.refactoring.participants.RenameProcessor;
import org.eclipse.ltk.core.refactoring.participants.SharableParticipants;

public class PMRenameProcessor extends RenameProcessor {

	private final ICompilationUnit iCompilationUnit;
	private final ITextSelection textSelection;

	private RenameStep renameStep;

	private String newName;

	public PMRenameProcessor(final ITextSelection selection, final ICompilationUnit iCompilationUnit) {
		this.textSelection = selection;
		this.iCompilationUnit = iCompilationUnit;

	}

	@Override
	public RefactoringStatus checkFinalConditions(final IProgressMonitor pm,
			final CheckConditionsContext context) throws CoreException {

		return new RefactoringStatus();
	}

	@Override
	public RefactoringStatus checkInitialConditions(final IProgressMonitor pm) throws CoreException {

		Timer.sharedTimer().start("STEP");

		final PMProject project = PMWorkspace.sharedWorkspace().projectForIJavaProject(
				this.iCompilationUnit.getJavaProject());

		RefactoringStatus result = null;

		if (project.sourcesAreOutOfSync()) {
			result = RefactoringStatus
					.createWarningStatus("PM Model is out of date. This will reinitialize.");
		} else {
			final ASTNode selectedNode = project.nodeForSelection(this.textSelection,
					this.iCompilationUnit);
			if (selectedNode instanceof SimpleName) {
				result = new RefactoringStatus();
			} else {
				result = RefactoringStatus
						.createFatalErrorStatus("Please select a name to use the Rename refactoring.");
			}
		}

		Timer.sharedTimer().stop("STEP");

		return result;
	}

	@Override
	public Change createChange(final IProgressMonitor pm) throws CoreException {
		Timer.sharedTimer().start("STEP");

		final PMProject project = PMWorkspace.sharedWorkspace().projectForIJavaProject(
				this.iCompilationUnit.getJavaProject());
		project.syncSources();

		this.renameStep = new RenameStep(project, (SimpleName) project.nodeForSelection(
				this.textSelection, this.iCompilationUnit));
		this.renameStep.setNewName(this.newName);

		final Change result = this.renameStep.createCompositeChange("Rename");

		Timer.sharedTimer().stop("STEP");
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

	public String getNewName() {
		return this.newName;
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
	public RefactoringParticipant[] loadParticipants(final RefactoringStatus status,
			final SharableParticipants sharedParticipants) throws CoreException {

		// RefactoringParticipant[] result =
		// ParticipantManager.loadRenameParticipants(status, this,
		// ((SimpleName)_nodeToRename).resolveBinding().getJavaElement(), new
		// RenameArguments("foo", true), new String[]
		// {"org.eclipse.jdt.core.javanature"}, sharedParticipants);

		// return result;

		return new RefactoringParticipant[0];
	}

	public void setNewName(final String newName) {
		this.newName = newName;
	}

}
