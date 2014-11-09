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
import net.creichen.pm.core.Project;
import net.creichen.pm.data.Pasteboard;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

public class CutStep extends Step {
    private List<ASTNode> selectedNodes;

    public CutStep(final Project project, final ASTNode node) {
        super(project);
        this.selectedNodes = new ArrayList<ASTNode>();
        this.selectedNodes.add(node);
    }

    public CutStep(final Project project, final List<ASTNode> selectedNodes) {
        super(project);

        this.selectedNodes = selectedNodes;
    }

    @Override
    public Map<PMCompilationUnit, ASTRewrite> calculateTextualChange() {
        final Map<PMCompilationUnit, ASTRewrite> result = new HashMap<PMCompilationUnit, ASTRewrite>();

        final ASTRewrite astRewrite = ASTRewrite.create(this.selectedNodes.get(0).getAST());

        for (final ASTNode node : this.selectedNodes) {
            astRewrite.remove(node, null);

            result.put(getProject().findPMCompilationUnitForNode(node), astRewrite);
        }

        return result;
    }

    // need method to test for errors before asking for changes

    @Override
    public void performASTChange() {
        /*
         *
         * _project.setPasteboardRoot(_selectedNodes.get(0));
         *
         * PMCompilationUnitModel usingModel = _project.modelForASTNode(_selectedNodes.get(0));
         * usingModel.removeIdentifiersForTreeStartingAtNode (_selectedNodes.get(0));
         */

        final Pasteboard pasteboard = Pasteboard.getInstance();

        pasteboard.setPasteboardRoots(this.selectedNodes);

        for (final ASTNode node : this.selectedNodes) {
            node.delete();
        }

    }

    @Override
    public void updateAfterReparse() {

    }

}
