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

        if (!findInsertionPointInBlock()) {
            findInsertionPointInTypeDeclaration();
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

    private boolean findInsertionPointInBlock() {
        final Block containingBlock = BlockFinder.findOn(this.compilationUnit, this.offset);
        if (containingBlock != null) {
            return findInsertionPointUnderNode(containingBlock, Block.STATEMENTS_PROPERTY);
        } else {
            this.insertionIndex = -1;
            return false;
        }
    }

    private boolean findInsertionPointInTypeDeclaration() {
        final TypeDeclaration typeDeclaration = TypeDeclarationFinder.findOn(this.compilationUnit,
                this.offset);
        if (typeDeclaration != null) {
            return findInsertionPointUnderNode(typeDeclaration,
                    TypeDeclaration.BODY_DECLARATIONS_PROPERTY);
        } else {
            System.err.println("Couldn't find containing type declaration");
            this.insertionIndex = -1;
            return false;
        }
    }

    private boolean findInsertionPointUnderNode(final ASTNode parentNode,
            final ChildListPropertyDescriptor property) {
        final List<ASTNode> statements = getStructuralProperty(property, parentNode);

        final int statementCount = statements.size();

        if (statementCount > 0) {
            if (this.offset <= statements.get(0).getStartPosition()) {
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

    private static final class BlockFinder extends SurroundingNodeFinder<Block> {

        private BlockFinder(final int position) {
            super(position);
        }

        @Override
        public boolean visit(final Block block) {
            if (isContainingNode(block)) {
                setContainingNode(block);
                return true;
            }
            return false;
        }

        public static Block findOn(final ASTNode node, final int position) {
            final BlockFinder finder = new BlockFinder(position);
            node.accept(finder);
            return finder.getContainingNode();
        }
    }

    private static final class TypeDeclarationFinder extends SurroundingNodeFinder<TypeDeclaration> {

        private TypeDeclarationFinder(final int position) {
            super(position);
        }

        @Override
        public boolean visit(final TypeDeclaration typeDeclaration) {
            if (isContainingNode(typeDeclaration)) {
                setContainingNode(typeDeclaration);
                return true;
            }
            return false;
        }

        public static TypeDeclaration findOn(final ASTNode node, final int position) {
            final TypeDeclarationFinder finder = new TypeDeclarationFinder(position);
            node.accept(finder);
            return finder.getContainingNode();
        }
    }

    private abstract static class SurroundingNodeFinder<E extends ASTNode> extends ASTVisitor {
        private final int position;
        private E containingNode;

        public SurroundingNodeFinder(final int position) {
            this.position = position;
        }

        public E getContainingNode() {
            return this.containingNode;
        }

        protected boolean isContainingNode(final ASTNode node) {
            return node.getStartPosition() < this.position
                    && this.position < node.getStartPosition() + node.getLength();
        }

        protected void setContainingNode(final E containingNode) {
            this.containingNode = containingNode;
        }
    }
}
