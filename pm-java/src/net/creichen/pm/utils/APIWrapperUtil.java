package net.creichen.pm.utils;

import java.util.List;

import org.eclipse.jdt.core.dom.*;

/**
 * Wraps some pre-Generics API calls in a manner that ensures type safety.
 */
public final class APIWrapperUtil {

    /**
     * Wraps the unchecked cast necessary by non-Generics method definition
     * {@link MethodInvocation#arguments()}.
     * 
     * @return a {@link List} of {@link Expression}.
     */
    @SuppressWarnings("unchecked")
    public static List<Expression> arguments(final MethodInvocation methodInvocation) {
        // according to Javadoc of arguments, this cast is
        // safe to make
        return methodInvocation.arguments();
    }

    /**
     * Wraps the unchecked cast necessary by non-Generics method definition
     * {@link SuperMethodInvocation#arguments()}.
     * 
     * @return a {@link List} of {@link Expression}.
     */
    @SuppressWarnings("unchecked")
    public static List<Expression> arguments(final SuperMethodInvocation methodInvocation) {
        return methodInvocation.arguments();
    }

    /**
     * Wraps the unchecked cast necessary by non-Generics method definition
     * {@link TypeDeclaration#bodyDeclarations()}.
     * 
     * @return a {@link List} of {@link BodyDeclaration}.
     */
    @SuppressWarnings("unchecked")
    public static List<BodyDeclaration> bodyDeclarations(final TypeDeclaration typeDeclaration) {
        return typeDeclaration.bodyDeclarations();
    }

    /**
     * Wraps the unchecked cast necessary by non-Generics method definition
     * {@link FieldDeclaration#fragments()}.
     * 
     * @return a {@link List} of {@link VariableDeclarationFragment}.
     */
    @SuppressWarnings("unchecked")
    public static List<VariableDeclarationFragment> fragments(
            final FieldDeclaration fieldDeclaration) {
        return fieldDeclaration.fragments();
    }

    /**
     * Wraps the unchecked cast necessary by non-Generics method definition
     * {@link VariableDeclarationExpression#fragments()}.
     * 
     * @return a {@link List} of {@link VariableDeclarationFragment}.
     */
    @SuppressWarnings("unchecked")
    public static List<VariableDeclarationFragment> fragments(
            final VariableDeclarationExpression expression) {
        return expression.fragments();
    }

    /**
     * Wraps the unchecked cast necessary by non-Generics method definition
     * {@link ASTNode#getStructuralProperty()} on {@link ASTNode}.This method handles the
     * "ChildListPropertyDescriptor" case of valid parameters.
     * 
     * @param propertyDescriptor
     *            the property descriptor for the child list property.
     * @param node
     *            the node containing the property.
     * @return a {@link List} of {@link ASTNode}.
     */
    @SuppressWarnings("unchecked")
    public static List<ASTNode> getStructuralProperty(
            final ChildListPropertyDescriptor propertyDescriptor, final ASTNode node) {
        // according to Javadoc of getStructuralProperty, this cast is
        // safe to make
        return (List<ASTNode>) node.getStructuralProperty(propertyDescriptor);
    }

    /**
     * Wraps the unchecked cast necessary by non-Generics method definition
     * {@link ASTNode#getStructuralProperty()} on {@link ASTNode}. This method handles the
     * "ChildPropertyDescriptor" case of valid parameters.
     * 
     * @param propertyDescriptor
     *            the property descriptor for the child property.
     * @param node
     *            the node containing the property.
     * @return an {@link ASTNode}.
     */
    public static ASTNode getStructuralProperty(final ChildPropertyDescriptor propertyDescriptor,
            final ASTNode node) {
        // according to Javadoc of getStructuralProperty, this cast is
        // safe to make
        return (ASTNode) node.getStructuralProperty(propertyDescriptor);
    }

    /**
     * Wraps the unchecked cast necessary by non-Generics method definition
     * {@link MethodDeclaration#modifiers()}.
     * 
     * @return a {@link List} of {@link IExtendedModifier}.
     */
    @SuppressWarnings("unchecked")
    public static List<IExtendedModifier> modifiers(final MethodDeclaration newMethodDeclaration) {
        return newMethodDeclaration.modifiers();
    }

    /**
     * Wraps the unchecked cast necessary by non-Generics method definition
     * {@link MethodDeclaration#parameters()}.
     * 
     * @return a {@link List} of {@link SingleVariableDeclaration}.
     */
    @SuppressWarnings("unchecked")
    public static List<SingleVariableDeclaration> parameters(
            final MethodDeclaration newMethodDeclaration) {
        return newMethodDeclaration.parameters();
    }

    /**
     * Wraps the unchecked cast necessary by non-Generics method definition
     * {@link Block#statements()}.
     * 
     * @return a {@link List} of {@link SingleVariableDeclaration}.
     */
    @SuppressWarnings("unchecked")
    public static List<Statement> statements(final Block block) {
        return block.statements();
    }

    /**
     * Wraps the unchecked cast necessary by non-Generics method definition
     * {@link ASTNode#structuralPropertiesForType()} on {@link ASTNode}.
     * 
     * @param node
     *            any ASTNode
     * @return a {@link List} of {@link StructuralPropertyDescriptor}.
     */
    @SuppressWarnings("unchecked")
    public static List<StructuralPropertyDescriptor> structuralPropertiesForType(final ASTNode node) {
        // according to Javadoc of structuralPropertiesForType, this cast is
        // safe to make
        return node.structuralPropertiesForType();
    }

    /**
     * Wraps the unchecked cast necessary by non-Generics method definition
     * {@link CompilationUnit#types()}.
     * 
     * @return a {@link List} of {@link AbstractTypeDeclaration}.
     */
    @SuppressWarnings("unchecked")
    public static List<AbstractTypeDeclaration> types(final CompilationUnit compilationUnit) {
        return compilationUnit.types();
    }

    private APIWrapperUtil() {
        // private utility class constructor
    }

}
