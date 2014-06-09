package net.creichen.pm.utils;

import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ChildListPropertyDescriptor;
import org.eclipse.jdt.core.dom.ChildPropertyDescriptor;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;

public final class ASTNodeUtil {

	/**
	 * Wraps the unchecked cast necessary by non-Generics method definition
	 * {@link ASTNode#structuralPropertiesForType()} on {@link ASTNode}.
	 * 
	 * @param node
	 *            any ASTNode
	 * @return a {@link List} of {@link StructuralPropertyDescriptor}.
	 */
	@SuppressWarnings("unchecked")
	public static List<StructuralPropertyDescriptor> structuralPropertiesForType(
			ASTNode node) {
		// according to Javadoc of structuralPropertiesForType, this cast is
		// safe to make
		return (List<StructuralPropertyDescriptor>) node
				.structuralPropertiesForType();
	}

	/**
	 * Wraps the unchecked cast necessary by non-Generics method definition
	 * {@link ASTNode#getStructuralProperty()} on {@link ASTNode}. This method
	 * handles the "ChildPropertyDescriptor" case of valid parameters.
	 * 
	 * @param propertyDescriptor
	 *            the property descriptor for the child property.
	 * @param node
	 *            the node containing the property.
	 * @return an {@link ASTNode}.
	 */
	public static ASTNode getStructuralProperty(
			ChildPropertyDescriptor propertyDescriptor, ASTNode node) {
		// according to Javadoc of getStructuralProperty, this cast is
		// safe to make
		return (ASTNode) node.getStructuralProperty(propertyDescriptor);
	}

	/**
	 * Wraps the unchecked cast necessary by non-Generics method definition
	 * {@link ASTNode#getStructuralProperty()} on {@link ASTNode}.This method
	 * handles the "ChildListPropertyDescriptor" case of valid parameters.
	 * 
	 * @param propertyDescriptor
	 *            the property descriptor for the child list property.
	 * @param node
	 *            the node containing the property.
	 * @return a {@link List} of {@link ASTNode}.
	 */
	@SuppressWarnings("unchecked")
	public static List<ASTNode> getStructuralProperty(
			ChildListPropertyDescriptor propertyDescriptor, ASTNode node) {
		// according to Javadoc of getStructuralProperty, this cast is
		// safe to make
		return (List<ASTNode>) node.getStructuralProperty(propertyDescriptor);
	}

}
