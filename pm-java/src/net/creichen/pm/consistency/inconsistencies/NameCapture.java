/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm.consistency.inconsistencies;

import net.creichen.pm.analysis.ASTQuery;
import net.creichen.pm.api.PMCompilationUnit;
import net.creichen.pm.models.NameModel;
import net.creichen.pm.models.Project;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.SimpleName;

public class NameCapture extends Inconsistency {

    private final ASTNode expectedDeclaration;
    private final ASTNode actualDeclaration;
    private Project project;

    public NameCapture(final Project project, final PMCompilationUnit iCompilationUnit, final ASTNode capturedNode,
            final ASTNode expectedDeclaration, final ASTNode actualDeclaration) {
        super(iCompilationUnit, capturedNode);
        this.project = project;
        this.expectedDeclaration = expectedDeclaration;
        this.actualDeclaration = actualDeclaration;
    }

    @Override
    public void acceptBehavioralChange() {
        final Name capturedName = (Name) getNode();
        final NameModel nameModel = this.project.getNameModel();
        final Name capturingName = ASTQuery.getSimpleName(this.actualDeclaration);
        final String capturingIdentifier = nameModel.getIdentifierForName(capturingName);

        nameModel.setIdentifierForName(capturingIdentifier, capturedName);
    }

    @Override
    public boolean allowsAcceptBehavioralChange() {
        return true;
    }

    public ASTNode getActualDeclaration() {
        return this.actualDeclaration;
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
