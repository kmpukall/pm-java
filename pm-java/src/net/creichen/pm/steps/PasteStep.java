/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm.steps;

import static net.creichen.pm.utils.APIWrapperUtil.getStructuralProperty;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.creichen.pm.consistency.ConsistencyValidator;
import net.creichen.pm.core.Project;
import net.creichen.pm.models.NameModel;
import net.creichen.pm.utils.Pasteboard;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.ASTMatcher;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ChildListPropertyDescriptor;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

public class PasteStep extends Step {

    private final ASTNode parent;

    private final ChildListPropertyDescriptor property;
    private final int index;

    public PasteStep(final Project project, final ASTNode parent, final ChildListPropertyDescriptor property,
            final int index) {
        super(project);

        this.parent = parent;
        this.property = property;

        this.index = index;
    }

    @Override
    public Map<ICompilationUnit, ASTRewrite> calculateTextualChange() {

        final Pasteboard pasteboard = Pasteboard.getInstance();

        final List<ASTNode> nodesToPaste = pasteboard.getPasteboardRoots();

        final Map<ICompilationUnit, ASTRewrite> result = new HashMap<ICompilationUnit, ASTRewrite>();

        final ASTRewrite astRewrite = ASTRewrite.create(this.parent.getAST());

        int index = this.index;

        for (final ASTNode nodeToPaste : nodesToPaste) {
            final ASTNode copiedNode = ASTNode.copySubtree(this.parent.getAST(), nodeToPaste);

            final ListRewrite lrw = astRewrite.getListRewrite(this.parent, this.property);
            lrw.insertAt(copiedNode, index++, null /* textEditGroup */);

            result.put(getProject().findPMCompilationUnitForNode(this.parent).getICompilationUnit(), astRewrite);
        }

        return result;
    }

    @Override
    public void cleanup() {
        // called regardless of whether updateAfterReparse() was called
    }

    @Override
    public void performASTChange() {

        final Pasteboard pasteboard = Pasteboard.getInstance();

        final List<ASTNode> nodesToPaste = pasteboard.getPasteboardRoots();

        final NameModel nameModel = getProject().getNameModel();

        for (int i = 0; i < nodesToPaste.size(); i++) {
            final ASTNode node = nodesToPaste.get(i);
            final int insertionIndex = i + this.index;

            final List<ASTNode> childList = getStructuralProperty(this.property, this.parent);

            final ASTNode copiedNode = ASTNode.copySubtree(this.parent.getAST(), node);
            childList.add(insertionIndex, copiedNode);

            final ASTMatcher identifierMatcher = new ASTMatcher() {

                @Override
                public boolean match(final SimpleName pasteboardName, final Object other) {
                    if (super.match(pasteboardName, other)) {

                        final SimpleName copyName = (SimpleName) other;

                        final String identifier = nameModel.getIdentifierForName(pasteboardName);

                        // System.out.println("Identifier for " + copyName +
                        // " is " + identifier);

                        nameModel.setIdentifierForName(identifier, copyName);

                        return true;
                    } else {
                        return false;
                    }
                }
            };

            if (node.subtreeMatch(identifierMatcher, copiedNode)) {
                getProject().recursivelyReplaceNodeWithCopy(node, copiedNode);

            } else {
                System.err.println("Couldn't match copied statement to original");

                throw new RuntimeException("PM Paste Error: Couldn't match copied statement to original");
            }
        }

        // FIXME(dcc) is this update necessary?
        getProject().updateToNewVersionsOfICompilationUnits();
        ConsistencyValidator.getInstance().reset();
    }

    @Override
    public void updateAfterReparse() {

    }

}
