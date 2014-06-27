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

    private final PMProject project;

    private final PMSplitStep splitStep;

    private PMRenameStep renameStep;

    private final String newVariableName;

    public PMSplitTemporaryRefactoring(final PMProject project,
            final ExpressionStatement assignmentExpressionStatement, final String newVariableName) {

        this.project = project;

        this.splitStep = new PMSplitStep(project, assignmentExpressionStatement);

        this.newVariableName = newVariableName;
    }

    public void apply() {

        this.splitStep.applyAllAtOnce();

        // now find the name for the new declaration and rename it

        final VariableDeclarationStatement newlyCreatedDeclaration = this.splitStep
                .getReplacementDeclarationStatement();

        final SimpleName simpleNameToRename = ((VariableDeclarationFragment) newlyCreatedDeclaration
                .fragments().get(0)).getName();

        this.renameStep = new PMRenameStep(this.project, simpleNameToRename);

        this.renameStep.setNewName(this.newVariableName);

        this.renameStep.applyAllAtOnce();

    }
}
