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

    protected ASTNode _expectedDeclaration;
    protected ASTNode _actualDeclaration;

    public PMNameCapture(PMProject project, PMCompilationUnit iCompilationUnit,
            ASTNode capturedNode, ASTNode expectedDeclaration, ASTNode actualDeclaration) {
        super(project, iCompilationUnit, capturedNode);

        _expectedDeclaration = expectedDeclaration;
        _actualDeclaration = actualDeclaration;
    }

    public ASTNode getCapturedNode() {
        return _node;
    }

    public ASTNode getExpectedDeclaration() {
        return _expectedDeclaration;
    }

    public ASTNode getActualDeclaration() {
        return _actualDeclaration;
    }

    public String getCapturedNodeDescription() {

        if (_node instanceof SimpleName) {
            return ((SimpleName) _node).getIdentifier();
        } else {
            return "Unknown node";
        }
    }

    @Override
    public String getHumanReadableDescription() {
        return getCapturedNodeDescription() + " was captured.";
    }

    @Override
    public boolean allowsAcceptBehavioralChange() {
        return true;
    }

    @Override
    public void acceptBehavioralChange() {
        Name capturedName = (Name) getCapturedNode();

        PMNameModel nameModel = _project.getNameModel();

        Name capturingName = _project.simpleNameForDeclaringNode(_actualDeclaration);

        String capturingIdentifier = nameModel.identifierForName(capturingName);

        nameModel.setIdentifierForName(capturingIdentifier, capturedName);

        _project.rescanForInconsistencies();
    }

}
