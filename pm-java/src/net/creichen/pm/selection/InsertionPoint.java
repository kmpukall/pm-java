/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm.selection;

import static net.creichen.pm.utils.APIWrapperUtil.getStructuralProperty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.jdt.core.dom.*;

import com.google.common.collect.Iterables;
import com.google.common.collect.Range;

// This class is a mess

public class InsertionPoint {

	private final CompilationUnit compilationUnit;
	private int insertionIndex;
	private ChildListPropertyDescriptor insertionProperty;
	private ASTNode insertionParent;

	public InsertionPoint(final CompilationUnit compilationUnit, final int offset) {
		this.insertionIndex = -1;
		this.compilationUnit = compilationUnit;

		/*
		 * a point is an insertion point if:
		 * 
		 * - it is in a block or a type declaration - it is NOT in a child of
		 * the above - OR it is the first/last character of such a child
		 */

		final ASTNode node = new SurroundingNodeFinder(offset).findOn(this.compilationUnit);
		if (node != null) {
			final List<Range<Integer>> ranges = rangesBetween(getStatements(node));
			for (int i = 0; i < ranges.size() && this.insertionIndex < 0; i++) {
				if (ranges.get(i).contains(offset)) {
					this.insertionIndex = i;
				}
			}
		}
		if (isValid()) {
			this.insertionParent = node;
			this.insertionProperty = getProperty(node);
		}
	}

	private List<Range<Integer>> rangesBetween(final List<ASTNode> statements) {
		final ArrayList<Range<Integer>> ranges = new ArrayList<Range<Integer>>();
		if (statements.isEmpty()) {
			// in this case, any cursor position within the parent node is valid
			final Range<Integer> unboundedRange = Range.all();
			ranges.add(unboundedRange);
		} else {
			ranges.add(Range.atMost(statements.get(0).getStartPosition()));
			for (int i = 0; i < statements.size() - 1; i++) {
				final ASTNode current = statements.get(i);
				final ASTNode next = statements.get(i + 1);
				ranges.add(Range.closed(current.getStartPosition() + current.getLength(),
						next.getStartPosition()));
			}
			final ASTNode lastStatement = Iterables.getLast(statements);
			ranges.add(Range.atLeast(lastStatement.getStartPosition() + lastStatement.getLength()));
		}
		return ranges;
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
