/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm.inconsistencies;

import net.creichen.pm.PMCompilationUnit;
import net.creichen.pm.PMProject;
import net.creichen.pm.models.PMNameModel;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.SimpleName;

public class PMNameCapture extends PMInconsistency {

    private final ASTNode expectedDeclaration;
    private final ASTNode actualDeclaration;

    public PMNameCapture(final PMProject project, final PMCompilationUnit iCompilationUnit,
            final ASTNode capturedNode, final ASTNode expectedDeclaration,
            final ASTNode actualDeclaration) {
        super(project, iCompilationUnit, capturedNode);

        this.expectedDeclaration = expectedDeclaration;
        this.actualDeclaration = actualDeclaration;
    }

    @Override
    public void acceptBehavioralChange() {
        final Name capturedName = (Name) getCapturedNode();

        final PMNameModel nameModel = this.getProject().getNameModel();

        final Name capturingName = this.getProject().simpleNameForDeclaringNode(this.actualDeclaration);

        final String capturingIdentifier = nameModel.identifierForName(capturingName);

        nameModel.setIdentifierForName(capturingIdentifier, capturedName);

        this.getProject().rescanForInconsistencies();
    }

    @Override
    public boolean allowsAcceptBehavioralChange() {
        return true;
    }

    public ASTNode getActualDeclaration() {
        return this.actualDeclaration;
    }

    public ASTNode getCapturedNode() {
        return getNode();
    }

    public String getCapturedNodeDescription() {

        if (getNode() instanceof SimpleName) {
            return ((SimpleName) getNode()).getIdentifier();
        } else {
            return "Unknown node";
        }
    }

    public ASTNode getExpectedDeclaration() {
        return this.expectedDeclaration;
    }

    @Override
    public String getHumanReadableDescription() {
        return getCapturedNodeDescription() + " was captured.";
    }

}
