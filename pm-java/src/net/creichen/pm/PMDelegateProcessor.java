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

public class PMDelegateProcessor extends RefactoringProcessor implements PMProcessor,
        PMProjectListener {

    private final ICompilationUnit iCompilationUnit;
    private final ITextSelection textSelection;

    private String delegateIdentifier;

    private PMDelegateStep step;

    public PMDelegateProcessor(final ITextSelection selection,
            final ICompilationUnit iCompilationUnit) {
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
        final PMProject project = PMWorkspace.sharedWorkspace().projectForIJavaProject(
                this.iCompilationUnit.getJavaProject());

        if (!project.sourcesAreOutOfSync()) {
            final ASTNode selectedNode = project.nodeForSelection(this.textSelection,
                    this.iCompilationUnit);

            if (selectedNode instanceof MethodInvocation) {
                return new RefactoringStatus();
            } else {
                return RefactoringStatus
                        .createFatalErrorStatus("Please select a method invocation [not a "
                                + selectedNode.getClass() + "]");
            }
        } else {
            return RefactoringStatus
                    .createWarningStatus("PM Model is out of date. This will reinitialize.");
        }

    }

    @Override
    public Change createChange(final IProgressMonitor pm) throws CoreException {

        final PMProject project = PMWorkspace.sharedWorkspace().projectForIJavaProject(
                this.iCompilationUnit.getJavaProject());

        project.syncSources();

        Change result = new NullChange();

        final ASTNode selectedNode = project.nodeForSelection(this.textSelection,
                this.iCompilationUnit);

        this.step = new PMDelegateStep(project, selectedNode);

        this.step.setDelegateIdentifier(this.delegateIdentifier);

        result = this.step.createCompositeChange("Delegate");

        return result;
    }

    @Override
    public Object[] getElements() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ICompilationUnit getICompilationUnit() {
        return this.iCompilationUnit;
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
    public RefactoringParticipant[] loadParticipants(final RefactoringStatus status,
            final SharableParticipants sharedParticipants) throws CoreException {
        return new RefactoringParticipant[0];
    }

    @Override
    public void projectDidReparse(final PMProject project) {
        this.step.updateAfterReparse();
        this.step.cleanup();
    }

    public void setDelegateIdentifier(final String delegateIdentifier) {
        this.delegateIdentifier = delegateIdentifier;
    }

    @Override
    public void textChangeWasApplied() {
        // this is after the text change was applied but before the model
        // has sync'd itself to the new text

        this.step.performASTChange();

        final PMProject project = PMWorkspace.sharedWorkspace().projectForIJavaProject(
                this.iCompilationUnit.getJavaProject());

        project.addProjectListener(this);
    }

    @Override
    public void textChangeWasNotApplied() {

        this.step.cleanup();

    }

}
