/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm.refactorings;

import net.creichen.pm.api.Node;
import net.creichen.pm.core.Project;
import net.creichen.pm.data.NodeStore;
import net.creichen.pm.steps.CutStep;
import net.creichen.pm.steps.PasteStep;

import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;

//We move a field by PMCutting it from its old parent and PMPasteing it in its new parent

class MoveFieldRefactoring {
    private final Project project;

    private final Node fieldReference;

    private final Node newParentReference;

    public MoveFieldRefactoring(final Project project, final FieldDeclaration fieldDeclaration,
            final TypeDeclaration newParent) {
        this.project = project;

        this.fieldReference = NodeStore.getInstance().getReference(fieldDeclaration);

        this.newParentReference = NodeStore.getInstance().getReference(newParent);
    }

    public void apply() {
        final CutStep cutStep = new CutStep(this.project, this.fieldReference.getNode());

        cutStep.apply();

        // race here? Will _fieldReference go away if we call gc?
        // NO: since the field is held in the pasteboard
        // But otherwise would be a problem
        // So: should node store hold strong refs to ast nodes???

        final TypeDeclaration newParent = (TypeDeclaration) this.newParentReference.getNode();

        final PasteStep pasteStep = new PasteStep(this.project, newParent, TypeDeclaration.BODY_DECLARATIONS_PROPERTY,
                newParent.bodyDeclarations().size());

        pasteStep.apply();
    }
}
