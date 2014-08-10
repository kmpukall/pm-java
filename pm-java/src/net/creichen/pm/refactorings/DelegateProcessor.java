/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm.refactorings;

import net.creichen.pm.consistency.ConsistencyValidator;
import net.creichen.pm.core.Workspace;
import net.creichen.pm.models.Project;
import net.creichen.pm.models.ProjectListener;
import net.creichen.pm.steps.DelegateStep;

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

public class DelegateProcessor extends RefactoringProcessor implements ProjectListener {

    private final ICompilationUnit iCompilationUnit;
    private final ITextSelection textSelection;

    private String delegateIdentifier;

    private DelegateStep step;

    public DelegateProcessor(final ITextSelection selection, final ICompilationUnit iCompilationUnit) {
        this.textSelection = selection;
        this.iCompilationUnit = iCompilationUnit;
    }

    @Override
    public RefactoringStatus checkFinalConditions(final IProgressMonitor pm, final CheckConditionsContext context) {
        return new RefactoringStatus();
    }

    @Override
    public RefactoringStatus checkInitialConditions(final IProgressMonitor pm) throws CoreException {
        final Project project = Workspace.getInstance().getProject(
                this.iCompilationUnit.getJavaProject());

        if (!project.sourcesAreOutOfSync()) {
            final ASTNode selectedNode = project.nodeForSelection(this.textSelection, this.iCompilationUnit);

            if (selectedNode instanceof MethodInvocation) {
                return new RefactoringStatus();
            } else {
                return RefactoringStatus.createFatalErrorStatus("Please select a method invocation [not a "
                        + selectedNode.getClass() + "]");
            }
        } else {
            return RefactoringStatus.createWarningStatus("PM Model is out of date. This will reinitialize.");
        }

    }

    @Override
    public Change createChange(final IProgressMonitor pm) throws CoreException {
        final Project project = Workspace.getInstance().getProject(
                this.iCompilationUnit.getJavaProject());
        project.syncSources();
        ConsistencyValidator.getInstance().reset();
        Change result = new NullChange();
        final ASTNode selectedNode = project.nodeForSelection(this.textSelection, this.iCompilationUnit);
        this.step = new DelegateStep(project, selectedNode);
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
    public void projectDidReparse(final Project project) {
        this.step.updateAfterReparse();
        this.step.cleanup();
    }

    public void setDelegateIdentifier(final String delegateIdentifier) {
        this.delegateIdentifier = delegateIdentifier;
    }

}
