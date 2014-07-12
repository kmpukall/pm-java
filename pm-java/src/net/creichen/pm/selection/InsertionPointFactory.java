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

import net.creichen.pm.utils.RangeUtil;

import org.eclipse.jdt.core.dom.*;

import com.google.common.collect.Range;

public final class InsertionPointFactory {

	private InsertionPointFactory() {

	}

	public static final class InsertionPoint {

		public static final InsertionPoint INVALID = new InsertionPoint(null, -1, null);

		private final ASTNode parent;
		private final int index;
		private final ChildListPropertyDescriptor property;

		private InsertionPoint(final ASTNode parent, final int insertionIndex,
				final ChildListPropertyDescriptor insertionProperty) {
			this.parent = parent;
			this.index = insertionIndex;
			this.property = insertionProperty;
		}

		public int getIndex() {
			return this.index;
		}

		public ASTNode getParent() {
			return this.parent;
		}

		public ChildListPropertyDescriptor getProperty() {
			return this.property;
		}

		public boolean isValid() {
			return !equals(InsertionPoint.INVALID);
		}

	}

	public static InsertionPoint createInsertionPoint(final CompilationUnit compilationUnit,
			final int offset) {
		InsertionPoint insertionPoint = InsertionPoint.INVALID;

		/*
		 * a point is an insertion point if:
		 * 
		 * - it is in a block or a type declaration - it is NOT in a child of
		 * the above - OR it is the first/last character of such a child
		 */

		final ASTNode node = new SurroundingNodeFinder(offset).findOn(compilationUnit);
		if (node != null) {
			final List<Range<Integer>> ranges = RangeUtil.rangesBetween(getStatements(node));
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
				return Collections.emptyList();
		}

	}

	private static ChildListPropertyDescriptor getProperty(final ASTNode node) {
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
