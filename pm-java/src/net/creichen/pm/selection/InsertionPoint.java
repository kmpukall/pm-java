/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm.selection;

import static net.creichen.pm.utils.APIWrapperUtil.getStructuralProperty;

import java.util.List;

import org.eclipse.jdt.core.dom.*;

// This class is a mess

public class InsertionPoint {

    private static final class PMContainingBlockVisitor extends TextSelectionNodeFinder {

        private PMContainingBlockVisitor(final int offset, final int length) {
            super(offset, length);
        }

        @Override
        public boolean visit(final Block block) {
            return visitContainingNode(block);
        }
    }

    private static final class PMContainingTypeDeclarationVisitor extends TextSelectionNodeFinder {

        private PMContainingTypeDeclarationVisitor(final int offset, final int length) {
            super(offset, length);
        }

        @Override
        public boolean visit(final TypeDeclaration typeDeclaration) {
            return visitContainingNode(typeDeclaration);
        }
    }

    private abstract static class TextSelectionNodeFinder extends ASTVisitor {
        private final int offset;
        private final int length;

        private ASTNode containingNode = null;

        private TextSelectionNodeFinder(final int offset, final int length) {
            this.offset = offset;
            this.length = length;
        }

        public ASTNode getContainingNode() {
            return this.containingNode;
        }

        public boolean visitContainingNode(final ASTNode node) {
            if (node.getStartPosition() < this.offset
                    && this.offset + this.length < node.getStartPosition() + node.getLength()) {

                this.containingNode = node;
                return true;
            } else {
                return false;
            }
        }
    }

    private static Block findContainingBlockForSelection(final ASTNode nodeToSearch,
            final int offset, final int length) {

        final PMContainingBlockVisitor containingBlockVisitor = new PMContainingBlockVisitor(
                offset, length);

        nodeToSearch.accept(containingBlockVisitor);

        return (Block) containingBlockVisitor.getContainingNode();
    }

    private static TypeDeclaration findContainingTypeDeclarationForSelection(
            final ASTNode nodeToSearch, final int offset, final int length) {
        final PMContainingTypeDeclarationVisitor containingTypeDeclarationVisitor = new PMContainingTypeDeclarationVisitor(
                offset, length);

        nodeToSearch.accept(containingTypeDeclarationVisitor);

        return (TypeDeclaration) containingTypeDeclarationVisitor.getContainingNode();
    }

    private final CompilationUnit compilationUnit;

    private final int offset;

    private int insertionIndex;

    private ChildListPropertyDescriptor insertionProperty;

    private ASTNode insertionParent;

    public InsertionPoint(final CompilationUnit compilationUnit, final int offset) {
        this.insertionIndex = -1;

        this.compilationUnit = compilationUnit;

        this.offset = offset;

        if (!findInsertionPointInBlock(this.offset)) {
            findInsertionPointInTypeDeclaration(this.offset);
        }
    }

    private boolean findInsertionPointInBlock(final int offset) {

        /*
         * a point is an insertion point if:
         * 
         * - it is in a block or a type declaration - it is NOT in a child of the above - OR it is
         * the first/last character of such a child
         */

        final Block containingBlock = findContainingBlockForSelection(this.compilationUnit, offset,
                0);

        if (containingBlock != null) {
            return findInsertionPointUnderNode(containingBlock, Block.STATEMENTS_PROPERTY, offset);

        } else {
            this.insertionIndex = -1;

            return false;
        }
    }

    private boolean findInsertionPointInTypeDeclaration(final int offset) {

        final TypeDeclaration containingTypeDeclaration = findContainingTypeDeclarationForSelection(
                this.compilationUnit, offset, 0);

        if (containingTypeDeclaration != null) {
            return findInsertionPointUnderNode(containingTypeDeclaration,
                    TypeDeclaration.BODY_DECLARATIONS_PROPERTY, offset);

        } else {
            System.err.println("Couldn't find containing type declaration");
            this.insertionIndex = -1;

            return false;
        }
    }

    private boolean findInsertionPointUnderNode(final ASTNode parentNode,
            final ChildListPropertyDescriptor property, final int offset) {
        final List<ASTNode> statements = getStructuralProperty(property, parentNode);

        final int statementCount = statements.size();

        if (statementCount > 0) {
            if (offset <= statements.get(0).getStartPosition()) {
                this.insertionIndex = 0;
            } else {
                final ASTNode lastStatement = statements.get(statementCount - 1);

                if (offset >= lastStatement.getStartPosition() + lastStatement.getLength()) {
                    this.insertionIndex = statementCount;
                } else {
                    // offset is not before the first statement, or after the
                    // last statement, so see if it is between two statements

                    for (int i = 1; i < statementCount; i++) {
                        final ASTNode statement1 = statements.get(i - 1);
                        final ASTNode statement2 = statements.get(i);

                        if (offset >= statement1.getStartPosition() + statement1.getLength()
                                && offset <= statement2.getStartPosition()) {

                            this.insertionIndex = i;
                            break;
                        }
                    }
                }
            }
        } else {
            this.insertionIndex = 0;
        }

        if (this.insertionIndex != -1) {
            this.insertionParent = parentNode;
            this.insertionProperty = property;
            return true;
        } else {
            this.insertionParent = null;
            this.insertionProperty = null;

            return false;
        }

    }

    public int insertionIndex() {
        return this.insertionIndex;
    }

    public ASTNode insertionParent() {
        return this.insertionParent;
    }

    public ChildListPropertyDescriptor insertionProperty() {
        return this.insertionProperty;
    }

    public boolean isSaneInsertionPoint() {
        return this.insertionIndex != -1;
    }
}
