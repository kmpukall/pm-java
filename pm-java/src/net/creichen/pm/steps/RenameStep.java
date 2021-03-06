/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm.steps;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.creichen.pm.api.PMCompilationUnit;
import net.creichen.pm.core.PMException;
import net.creichen.pm.core.Project;
import net.creichen.pm.models.name.NameModel;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

public class RenameStep extends AbstractStep {
    private SimpleName nameNode;

    private String newName;

    private List<SimpleName> nameNodesToChange;

    public RenameStep(final Project project, final SimpleName nameNode) {
        super(project);

        if (nameNode != null) {
            this.nameNode = nameNode;
            this.nameNodesToChange = new ArrayList<SimpleName>();
        } else {
            throw new PMException("Cannot create PMRenameStep with null nameNode");
        }

    }

    @Override
    public Map<PMCompilationUnit, ASTRewrite> calculateTextualChange() {
        final Map<PMCompilationUnit, ASTRewrite> result = new HashMap<PMCompilationUnit, ASTRewrite>();
        final NameModel nameModel = getProject().getNameModel();
        this.nameNodesToChange = nameModel.getRelatedNodes(this.nameNode);

        final Map<PMCompilationUnit, List<SimpleName>> nodesByICompilationUnit = new HashMap<PMCompilationUnit, List<SimpleName>>();

        for (final SimpleName nodeToRename : nameModel.getRelatedNodes(this.nameNode)) {
            PMCompilationUnit pmCompilationUnit = getProject().findPMCompilationUnitForNode(nodeToRename);

            List<SimpleName> namesForUnit = nodesByICompilationUnit.get(pmCompilationUnit);

            if (namesForUnit == null) {
                namesForUnit = new ArrayList<SimpleName>();
                nodesByICompilationUnit.put(pmCompilationUnit, namesForUnit);
            }

            namesForUnit.add(nodeToRename);
        }

        if (nodesByICompilationUnit.size() > 0) {
            for (final PMCompilationUnit unitForRename : nodesByICompilationUnit.keySet()) {

                final List<SimpleName> nodesInUnit = nodesByICompilationUnit.get(unitForRename);

                final ASTRewrite astRewrite = ASTRewrite.create(nodesInUnit.get(0).getAST());

                result.put(unitForRename, astRewrite);

                for (final SimpleName sameNode : nodesInUnit) {

                    final SimpleName newNode = this.nameNode.getAST().newSimpleName(this.newName);

                    astRewrite.replace(sameNode, newNode, null);

                }
            }
        }

        return result;
    }

    // need method to test for errors before asking for changes

    public String getNewName() {
        return this.newName;
    }

    @Override
    public void performASTChange() {
        for (final SimpleName nameNode : this.nameNodesToChange) {
            nameNode.setIdentifier(this.newName);

            // Need to rename file if this is the name of a class and it is the
            // highest level class in the compilation unit
            if (nameNode.getParent() instanceof TypeDeclaration
                    && nameNode.getParent().getParent() instanceof CompilationUnit) {
                final ICompilationUnit iCompilationUnitToRename = (ICompilationUnit) ((CompilationUnit) nameNode
                        .getParent().getParent()).getJavaElement();

                final PMCompilationUnit pmCompilationUnitToRename = getProject().getPMCompilationUnit(
                        iCompilationUnitToRename);

                getProject().rename(pmCompilationUnitToRename, this.newName);
            }
        }
    }

    public void setNewName(final String newName) {
        this.newName = newName;
    }
}
