/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm.selection;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ChildListPropertyDescriptor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;

public class Selection {

    @SuppressWarnings("restriction")
    private static class PMExactSelectionVisitor extends
            org.eclipse.jdt.internal.corext.dom.GenericVisitor {

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

    private final CompilationUnit compilationUnit;
    private int offset;

    private int length;

    private ASTNode singleSelectedNode;
    private ASTNode propertyParentNode;
    private StructuralPropertyDescriptor selectedPropertyDescriptor;
    private int childListPropertyOffset;

    private int childListPropertyLength;

    public Selection(final CompilationUnit compilationUnit, final int offset, final int length) {

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

            final InsertionPoint startInsertionPoint = new InsertionPoint(this.compilationUnit,
                    offset);

            final InsertionPoint endInsertionPoint = new InsertionPoint(this.compilationUnit,
                    offset + length);

            if (startInsertionPoint.isValid()
                    && endInsertionPoint.isValid()
                    && startInsertionPoint.getInsertionParent() == endInsertionPoint.getInsertionParent()
                    && startInsertionPoint.getInsertionProperty().equals(
                            endInsertionPoint.getInsertionProperty())) {

                this.propertyParentNode = startInsertionPoint.getInsertionParent();

                this.selectedPropertyDescriptor = startInsertionPoint.getInsertionProperty();

                this.childListPropertyOffset = startInsertionPoint.getInsertionIndex();

                this.childListPropertyLength = endInsertionPoint.getInsertionIndex()
                        - startInsertionPoint.getInsertionIndex();
            }
        }

    }

    public String getSelectionAsString() {

        String result = null;

        String entireSource = null;

        try {
            final ICompilationUnit iCompilationUnit = ((ICompilationUnit) this.compilationUnit
                    .getJavaElement());

            // Sometimes CompilationUnits don't have an associated
            // ICompilationUnit (e.g if they were parsed from a string)
            // If not, just us the (for debugging purposes only)
            // _compilation.toString()

            if (iCompilationUnit != null) {
                entireSource = ((ICompilationUnit) this.compilationUnit.getJavaElement())
                        .getSource();
            } else {
                entireSource = this.compilationUnit.toString();
            }

        } catch (final JavaModelException e) {
            System.err.println("Exception in PMSelection.getSelectionAsString(): " + e);

            throw new RuntimeException(e);
        }

        result = entireSource.substring(this.offset, this.offset + this.length);

        return result;
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
