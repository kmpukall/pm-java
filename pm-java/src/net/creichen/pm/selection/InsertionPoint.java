package net.creichen.pm.selection;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ChildListPropertyDescriptor;

public final class InsertionPoint {

	public static final InsertionPoint INVALID = new InsertionPoint(null, -1, null);

	private final ASTNode parent;
	private final int index;
	private final ChildListPropertyDescriptor property;

	InsertionPoint(final ASTNode parent, final int insertionIndex,
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