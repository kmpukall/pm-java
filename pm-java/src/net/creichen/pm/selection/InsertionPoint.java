/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm.selection;

import static net.creichen.pm.utils.APIWrapperUtil.getStructuralProperty;

import java.util.Collections;
import java.util.List;

import org.eclipse.jdt.core.dom.*;

// This class is a mess

public class InsertionPoint {

    private final CompilationUnit compilationUnit;
    private final int offset;
    private int insertionIndex;
    private ChildListPropertyDescriptor insertionProperty;
    private ASTNode insertionParent;

    public InsertionPoint(final CompilationUnit compilationUnit, final int offset) {
        this.insertionIndex = -1;
        this.compilationUnit = compilationUnit;
        this.offset = offset;

        /*
         * a point is an insertion point if:
         * 
         * - it is in a block or a type declaration - it is NOT in a child of the above - OR it is
         * the first/last character of such a child
         */

        final ASTNode node = new SurroundingNodeFinder(offset).findOn(this.compilationUnit);
        if (node != null) {
            final List<ASTNode> statements = getStatements(node);
            final int statementCount = statements.size();
            if (statementCount == 0 || this.offset <= statements.get(0).getStartPosition()) {
                this.insertionIndex = 0;
            } else {
                final ASTNode lastStatement = statements.get(statementCount - 1);

                if (this.offset >= lastStatement.getStartPosition() + lastStatement.getLength()) {
                    this.insertionIndex = statementCount;
                } else {
                    // offset is not before the first statement, or after the
                    // last statement, so see if it is between two statements

                    for (int i = 1; i < statementCount; i++) {
                        final ASTNode statement1 = statements.get(i - 1);
                        final ASTNode statement2 = statements.get(i);

                        if (this.offset >= statement1.getStartPosition() + statement1.getLength()
                                && this.offset <= statement2.getStartPosition()) {

                            this.insertionIndex = i;
                            break;
                        }
                    }
                }
            }

            if (isValid()) {
                this.insertionParent = node;
                this.insertionProperty = getProperty(node);
            }
        }
    }

    public int getInsertionIndex() {
        return this.insertionIndex;
    }

    public ASTNode getInsertionParent() {
        return this.insertionParent;
    }

    public ChildListPropertyDescriptor getInsertionProperty() {
        return this.insertionProperty;
    }

    public boolean isValid() {
        return this.insertionIndex != -1;
    }

    private List<ASTNode> getStatements(final ASTNode parentNode) {
        switch (parentNode.getNodeType()) {
            case ASTNode.BLOCK:
            case ASTNode.TYPE_DECLARATION:
                final ChildListPropertyDescriptor property = getProperty(parentNode);
                return getStructuralProperty(property, parentNode);
            default:
                return Collections.emptyList();
        }

    }

    private ChildListPropertyDescriptor getProperty(final ASTNode node) {
        if (node.getNodeType() == ASTNode.TYPE_DECLARATION) {
            return TypeDeclaration.BODY_DECLARATIONS_PROPERTY;
        } else {
            return Block.STATEMENTS_PROPERTY;
        }
    }

    private static final class SurroundingNodeFinder extends ASTVisitor {
        private final int position;
        private ASTNode containingNode;

        private SurroundingNodeFinder(final int position) {
            this.position = position;
        }

        @Override
        public boolean visit(final Block node) {
            return visitInternal(node);
        }

        @Override
        public boolean visit(final TypeDeclaration node) {
            return visitInternal(node);
        }

        private boolean visitInternal(final ASTNode node) {
            if (node.getStartPosition() < this.position
                    && this.position < node.getStartPosition() + node.getLength()) {
                this.containingNode = node;
                return true;
            }
            return false;
        }

        public ASTNode findOn(final ASTNode node) {
            final SurroundingNodeFinder finder = new SurroundingNodeFinder(this.position);
            node.accept(finder);
            return finder.containingNode;
        }
    }
}
