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

import net.creichen.pm.Pasteboard;
import net.creichen.pm.api.PMProject;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

public class CutStep extends Step {
    private List<ASTNode> selectedNodes;

    public CutStep(final PMProject project, final ASTNode node) {
        super(project);

        final List<ASTNode> selectedNodes = new ArrayList<ASTNode>();

        selectedNodes.add(node);

        initWithSelectedNodes(selectedNodes);
    }

    public CutStep(final PMProject project, final List<ASTNode> selectedNodes) {
        super(project);

        initWithSelectedNodes(selectedNodes);
    }

    @Override
    public Map<ICompilationUnit, ASTRewrite> calculateTextualChange() {
        final Map<ICompilationUnit, ASTRewrite> result = new HashMap<ICompilationUnit, ASTRewrite>();

        final ASTRewrite astRewrite = ASTRewrite.create(this.selectedNodes.get(0).getAST());

        for (final ASTNode node : this.selectedNodes) {
            astRewrite.remove(node, null);

            result.put(this.getProject().findPMCompilationUnitForNode(node).getICompilationUnit(),
                    astRewrite);
        }

        return result;
    }

    // need method to test for errors before asking for changes

    @Override
    public void cleanup() {
        // called regardless of whether updateAfterReparse() was called
    }

    private void initWithSelectedNodes(final List<ASTNode> selectedNodes) {
        this.selectedNodes = selectedNodes;
    }

    @Override
    public void performASTChange() {
        /*
         * 
         * _project.setPasteboardRoot(_selectedNodes.get(0));
         * 
         * PMCompilationUnitModel usingModel = _project.modelForASTNode(_selectedNodes.get(0));
         * usingModel.removeIdentifiersForTreeStartingAtNode (_selectedNodes.get(0));
         */

        final Pasteboard pasteboard = this.getProject().getPasteboard();

        pasteboard.setPasteboardRoots(this.selectedNodes);

        for (final ASTNode node : this.selectedNodes) {
            node.delete();
        }

    }

    @Override
    public void updateAfterReparse() {

    }

}
