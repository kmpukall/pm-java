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
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.NullChange;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.RefactoringParticipant;
import org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor;
import org.eclipse.ltk.core.refactoring.participants.SharableParticipants;

public class PMSplitProcessor extends RefactoringProcessor {

    private final ICompilationUnit iCompilationUnit;
    private final ITextSelection textSelection;

    private PMSplitStep step;

    public PMSplitProcessor(final ITextSelection selection, final ICompilationUnit iCompilationUnit) {
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

            project.syncSources();

            ASTNode selectedNode = project.nodeForSelection(this.textSelection,
                    this.iCompilationUnit);

            // We expect the selected node to be an ExpressionStatement with an
            // Assignment as the expression.
            // If an Assignment inside an ExpressionStatement is selected, we
            // fix up the selectednode to be the ExpressionStatement.

            if (selectedNode instanceof Assignment) {
                final Assignment assignment = (Assignment) selectedNode;

                if (assignment.getParent() instanceof ExpressionStatement) {
                    selectedNode = assignment.getParent();
                }
            }

            if (selectedNode instanceof ExpressionStatement) {

                final ExpressionStatement assignmentStatement = (ExpressionStatement) selectedNode;

                if (assignmentStatement.getExpression() instanceof Assignment) {
                    final Assignment assignmentExpression = (Assignment) assignmentStatement
                            .getExpression();

                    if (assignmentExpression.getLeftHandSide() instanceof SimpleName) {

                        final SimpleName name = (SimpleName) assignmentExpression.getLeftHandSide();

                        final VariableDeclaration declaration = PMASTNodeUtil
                                .localVariableDeclarationForSimpleName(name);

                        if (declaration != null
                                && PMASTNodeUtil.variableDeclarationIsLocal(declaration)) {
                            this.step = new PMSplitStep(project, (ExpressionStatement) selectedNode);

                            return new RefactoringStatus();
                        }

                    }
                }
            }

            return RefactoringStatus
                    .createFatalErrorStatus("Split temporary can only be run on an assignment to a local variable.");

        } else {
            return RefactoringStatus
                    .createWarningStatus("PM Model is out of date. This will reinitialize.");
        }

    }

    @Override
    public Change createChange(final IProgressMonitor pm) throws CoreException {

        Change result = new NullChange();

        result = this.step.createCompositeChange("Split");

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
    public RefactoringParticipant[] loadParticipants(final RefactoringStatus status,
            final SharableParticipants sharedParticipants) throws CoreException {
        return new RefactoringParticipant[0];
    }

}
