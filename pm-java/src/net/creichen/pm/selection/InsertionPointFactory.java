/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm.selection;

import static net.creichen.pm.utils.APIWrapperUtil.getStructuralProperty;

import java.util.ArrayList;
import java.util.List;

import net.creichen.pm.utils.visitors.finders.SurroundingNodeFinder;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.ChildListPropertyDescriptor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import com.google.common.collect.Iterables;
import com.google.common.collect.Range;

public final class InsertionPointFactory {

    private InsertionPointFactory() {

    }

    private static List<Range<Integer>> rangesBetween(final List<ASTNode> statements) {
        final List<Range<Integer>> ranges = new ArrayList<Range<Integer>>();
        if (statements.isEmpty()) {
            // in this case, any cursor position within the parent node is valid
            final Range<Integer> unboundedRange = Range.all();
            ranges.add(unboundedRange);
        } else {
            ranges.add(Range.atMost(statements.get(0).getStartPosition()));
            for (int i = 0; i < statements.size() - 1; i++) {
                final ASTNode current = statements.get(i);
                final ASTNode next = statements.get(i + 1);
                ranges.add(Range.closed(current.getStartPosition() + current.getLength(), next.getStartPosition()));
            }
            final ASTNode lastStatement = Iterables.getLast(statements);
            ranges.add(Range.atLeast(lastStatement.getStartPosition() + lastStatement.getLength()));
        }
        return ranges;
    }

    public static InsertionPoint createInsertionPoint(final CompilationUnit compilationUnit, final int offset) {
        InsertionPoint insertionPoint = InsertionPoint.INVALID;

        /*
         * a point is an insertion point if:
         * 
         * - it is in a block or a type declaration - it is NOT in a child of
         * the above - OR it is the first/last character of such a child
         */

        final ASTNode node = new SurroundingNodeFinder(offset).findOn(compilationUnit);
        if (node != null) {
            final List<Range<Integer>> ranges = InsertionPointFactory.rangesBetween(getStatements(node));
            for (int i = 0; i < ranges.size(); i++) {
                if (ranges.get(i).contains(offset)) {
                    insertionPoint = new InsertionPoint(node, i, getProperty(node));
                    break;
                }
            }
        }
        return insertionPoint;
    }

    private static List<ASTNode> getStatements(final ASTNode parentNode) {
        switch (parentNode.getNodeType()) {
            case ASTNode.BLOCK:
            case ASTNode.TYPE_DECLARATION:
                final ChildListPropertyDescriptor property = getProperty(parentNode);
                return getStructuralProperty(property, parentNode);
            default:
                throw new IllegalArgumentException("attempted creation of insertion point on node "
                        + parentNode.toString() + ", must be one of Block, TypeDeclaration");
        }

    }

    private static ChildListPropertyDescriptor getProperty(final ASTNode node) {
        if (node.getNodeType() == ASTNode.TYPE_DECLARATION) {
            return TypeDeclaration.BODY_DECLARATIONS_PROPERTY;
        } else {
            return Block.STATEMENTS_PROPERTY;
        }
    }
}
