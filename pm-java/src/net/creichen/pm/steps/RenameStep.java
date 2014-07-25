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
import net.creichen.pm.api.PMProject;
import net.creichen.pm.models.NameModel;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

public class RenameStep extends Step {
    private SimpleName nameNode;

    private String newName;

    private List<SimpleName> nameNodesToChange;

    public RenameStep(final PMProject project, final SimpleName nameNode) {
        super(project);

        if (nameNode != null) {
            this.nameNode = nameNode;
            this.nameNodesToChange = new ArrayList<SimpleName>();
        } else {
            throw new RuntimeException("Cannot create PMRenameStep with null nameNode");
        }

    }

    @Override
    public Map<ICompilationUnit, ASTRewrite> calculateTextualChange() {
        final Map<ICompilationUnit, ASTRewrite> result = new HashMap<ICompilationUnit, ASTRewrite>();

        this.getProject().syncSources();

        final NameModel nameModel = this.getProject().getNameModel();

        final ArrayList<SimpleName> nodesToRename = nameModel
                .nameNodesRelatedToNameNode(this.nameNode);

        this.nameNodesToChange.addAll(nodesToRename);

        final HashMap<ICompilationUnit, List<SimpleName>> nodesByICompilationUnit = new HashMap<ICompilationUnit, List<SimpleName>>();

        for (final SimpleName nodeToRename : nodesToRename) {
            final CompilationUnit containingCompilationUnit = (CompilationUnit) nodeToRename
                    .getRoot();

            final ICompilationUnit containingICompilationUnit = (ICompilationUnit) containingCompilationUnit
                    .getJavaElement();

            List<SimpleName> namesForUnit = nodesByICompilationUnit.get(containingICompilationUnit);

            if (namesForUnit == null) {
                namesForUnit = new ArrayList<SimpleName>();
                nodesByICompilationUnit.put(containingICompilationUnit, namesForUnit);
            }

            namesForUnit.add(nodeToRename);
        }

        if (nodesByICompilationUnit.size() > 0) {
            for (final ICompilationUnit unitForRename : nodesByICompilationUnit.keySet()) {

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

    @Override
    public void cleanup() {
        // called regardless of whether updateAfterReparse() was called
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

                final PMCompilationUnit pmCompilationUnitToRename = this.getProject()
                        .getPMCompilationUnitForICompilationUnit(iCompilationUnitToRename);

                pmCompilationUnitToRename.rename(this.newName);

            }
        }

    }

    public void setNewName(final String newName) {
        this.newName = newName;
    }

    @Override
    public void updateAfterReparse() {

    }
}
