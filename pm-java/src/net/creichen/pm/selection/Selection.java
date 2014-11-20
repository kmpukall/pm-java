/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm.selection;

import net.creichen.pm.api.PMCompilationUnit;

import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ChildListPropertyDescriptor;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;

public class Selection {

    @SuppressWarnings("restriction")
    private static class PMExactSelectionVisitor extends org.eclipse.jdt.internal.corext.dom.GenericVisitor {

        private final int offset;
        private final int length;

        private ASTNode containingNode;

        public PMExactSelectionVisitor(final int offset, final int length) {
            this.offset = offset;
            this.length = length;
        }

        public ASTNode getContainingNode() {
            return this.containingNode;
        }

        private boolean nodeContainsSelection(final ASTNode node) {
            return this.offset >= node.getStartPosition()
                    && this.offset + this.length <= node.getStartPosition() + node.getLength();
        }

        @Override
        public boolean visitNode(final ASTNode node) {
            // result from this method determines whether it will be called for
            // nodes beneath it.

            if (nodeContainsSelection(node)) {

                this.containingNode = node;

                return true;
            } else {

                return false;
            }

        }
    }

    private final PMCompilationUnit compilationUnit;
    private int offset;

    private int length;

    private ASTNode singleSelectedNode;
    private ASTNode propertyParentNode;
    private StructuralPropertyDescriptor selectedPropertyDescriptor;
    private int childListPropertyOffset;

    private int childListPropertyLength;

    public Selection(final PMCompilationUnit compilationUnit, final int offset, final int length) {

        this.compilationUnit = compilationUnit;

        this.offset = offset;
        this.length = length;

        this.childListPropertyOffset = -1;
        this.childListPropertyLength = -1;

        trimWhitespace();

        // ought to do this lazily
        findSelection(this.offset, this.length);

    }

    private void findSelection(final int offset, final int length) {
        final PMExactSelectionVisitor selectionVisitor = new PMExactSelectionVisitor(offset, length);
        this.compilationUnit.accept(selectionVisitor);

        final ASTNode containingNode = selectionVisitor.getContainingNode();

        if (containingNode.getStartPosition() == offset && containingNode.getLength() == length) {
            this.singleSelectedNode = containingNode;
        } else {
            this.singleSelectedNode = null;

            // if there is no single selected node, find the insertion points
            // corresponding to the start and
            // end of the selection and see if these insertion points contain a
            // sequence of nodes

            final InsertionPoint startInsertionPoint = InsertionPointFactory.createInsertionPoint(this.compilationUnit,
                    offset);

            final InsertionPoint endInsertionPoint = InsertionPointFactory.createInsertionPoint(this.compilationUnit,
                    offset + length);

            if (startInsertionPoint.isValid() && endInsertionPoint.isValid()
                    && startInsertionPoint.getParent() == endInsertionPoint.getParent()
                    && startInsertionPoint.getProperty().equals(endInsertionPoint.getProperty())) {

                this.propertyParentNode = startInsertionPoint.getParent();

                this.selectedPropertyDescriptor = startInsertionPoint.getProperty();

                this.childListPropertyOffset = startInsertionPoint.getIndex();

                this.childListPropertyLength = endInsertionPoint.getIndex() - startInsertionPoint.getIndex();
            }
        }

    }

    public String getSelectionAsString() {
        String source;
        try {
            source = this.compilationUnit.getSource();
        } catch (JavaModelException e) {
            e.printStackTrace();
            return "";
        }
        return source.substring(this.offset, this.offset + this.length);
    }

    public boolean isListSelection() {
        return this.selectedPropertyDescriptor instanceof ChildListPropertyDescriptor;
    }

    public boolean isMultipleSelection() {
        return this.childListPropertyLength > 0;
    }

    public boolean isSaneSelection() {
        return this.selectedPropertyDescriptor != null || this.singleSelectedNode != null;
    }

    public ASTNode selectedNodeParent() {
        if (this.singleSelectedNode != null) {
            return this.singleSelectedNode.getParent();
        } else {
            return this.propertyParentNode;
        }
    }

    public StructuralPropertyDescriptor selectedNodeParentProperty() {
        return this.selectedPropertyDescriptor;
    }

    public int selectedNodeParentPropertyListLength() {
        return this.childListPropertyLength;
    }

    public int selectedNodeParentPropertyListOffset() {
        return this.childListPropertyOffset;
    }

    public ASTNode singleSelectedNode() {
        return this.singleSelectedNode;
    }

    private void trimWhitespace() {
        // move the selection so that it contains no leading or trailing
        // whitespace

        final String selection = getSelectionAsString();

        // trim whitespace at beginning
        for (int index = 0; index < selection.length(); index++) {
            if (Character.isWhitespace(selection.charAt(index))) {
                this.offset++;
                this.length--;
            } else {
                break;
            }
        }

        // trim whitespace at end
        for (int index = selection.length() - 1; index >= 0; index--) {
            if (Character.isWhitespace(selection.charAt(index))) {
                this.length--;
            } else {
                break;
            }
        }

    }

}
