/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm.refactorings;

import net.creichen.pm.core.Project;
import net.creichen.pm.steps.CutStep;
import net.creichen.pm.steps.PasteStep;

import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;

/**
 * We move a field by Cutting it from its old parent and Pasting it in its new parent.
 */
class MoveFieldRefactoring {
    private final Project project;

    private final FieldDeclaration field;

    private final TypeDeclaration newParent;

    public MoveFieldRefactoring(final Project project, final FieldDeclaration fieldDeclaration,
            final TypeDeclaration newParent) {
        this.project = project;
        this.field = fieldDeclaration;
        this.newParent = newParent;
    }

    public void apply() {
        final CutStep cutStep = new CutStep(this.project, this.field);
        cutStep.applyAllAtOnce();

        final PasteStep pasteStep = new PasteStep(this.project, this.newParent,
                TypeDeclaration.BODY_DECLARATIONS_PROPERTY, this.newParent.bodyDeclarations().size());
        pasteStep.applyAllAtOnce();
    }
}
