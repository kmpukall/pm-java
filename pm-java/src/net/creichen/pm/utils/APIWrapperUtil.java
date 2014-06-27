package net.creichen.pm.utils;

import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.ChildListPropertyDescriptor;
import org.eclipse.jdt.core.dom.ChildPropertyDescriptor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

/**
 * Wraps some pre-Generics API calls in a manner that ensures type safety.
 */
public final class APIWrapperUtil {

    /**
     * Wraps the unchecked cast necessary by non-Generics method definition
     * {@link ASTNode#structuralPropertiesForType()} on {@link ASTNode}.
     * 
     * @param node
     *            any ASTNode
     * @return a {@link List} of {@link StructuralPropertyDescriptor}.
     */
    @SuppressWarnings("unchecked")
    public static List<StructuralPropertyDescriptor> structuralPropertiesForType(ASTNode node) {
        // according to Javadoc of structuralPropertiesForType, this cast is
        // safe to make
        return (List<StructuralPropertyDescriptor>) node.structuralPropertiesForType();
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
    public static ASTNode getStructuralProperty(ChildPropertyDescriptor propertyDescriptor,
            ASTNode node) {
        // according to Javadoc of getStructuralProperty, this cast is
        // safe to make
        return (ASTNode) node.getStructuralProperty(propertyDescriptor);
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
            ChildListPropertyDescriptor propertyDescriptor, ASTNode node) {
        // according to Javadoc of getStructuralProperty, this cast is
        // safe to make
        return (List<ASTNode>) node.getStructuralProperty(propertyDescriptor);
    }

    /**
     * Wraps the unchecked cast necessary by non-Generics method definition
     * {@link MethodInvocation#arguments()}.
     * 
     * @return a {@link List} of {@link Expression}.
     */
    @SuppressWarnings("unchecked")
    public static List<Expression> arguments(MethodInvocation methodInvocation) {
        // according to Javadoc of arguments, this cast is
        // safe to make
        return (List<Expression>) methodInvocation.arguments();
    }

    /**
     * Wraps the unchecked cast necessary by non-Generics method definition
     * {@link SuperMethodInvocation#arguments()}.
     * 
     * @return a {@link List} of {@link Expression}.
     */
    @SuppressWarnings("unchecked")
    public static List<Expression> arguments(SuperMethodInvocation methodInvocation) {
        return (List<Expression>) methodInvocation.arguments();
    }

    /**
     * Wraps the unchecked cast necessary by non-Generics method definition
     * {@link MethodDeclaration#modifiers()}.
     * 
     * @return a {@link List} of {@link IExtendedModifier}.
     */
    @SuppressWarnings("unchecked")
    public static List<IExtendedModifier> modifiers(MethodDeclaration newMethodDeclaration) {
        return (List<IExtendedModifier>) newMethodDeclaration.modifiers();
    }

    /**
     * Wraps the unchecked cast necessary by non-Generics method definition
     * {@link MethodDeclaration#parameters()}.
     * 
     * @return a {@link List} of {@link SingleVariableDeclaration}.
     */
    @SuppressWarnings("unchecked")
    public static List<SingleVariableDeclaration> parameters(MethodDeclaration newMethodDeclaration) {
        return (List<SingleVariableDeclaration>) newMethodDeclaration.parameters();
    }

    /**
     * Wraps the unchecked cast necessary by non-Generics method definition
     * {@link Block#statements()}.
     * 
     * @return a {@link List} of {@link SingleVariableDeclaration}.
     */
    @SuppressWarnings("unchecked")
    public static List<Statement> statements(Block block) {
        return block.statements();
    }

    /**
     * Wraps the unchecked cast necessary by non-Generics method definition
     * {@link TypeDeclaration#bodyDeclarations()}.
     * 
     * @return a {@link List} of {@link BodyDeclaration}.
     */
    @SuppressWarnings("unchecked")
    public static List<BodyDeclaration> bodyDeclarations(TypeDeclaration typeDeclaration) {
        return (List<BodyDeclaration>) typeDeclaration.bodyDeclarations();
    }

    /**
     * Wraps the unchecked cast necessary by non-Generics method definition
     * {@link CompilationUnit#types()}.
     * 
     * @return a {@link List} of {@link AbstractTypeDeclaration}.
     */
    @SuppressWarnings("unchecked")
    public static List<AbstractTypeDeclaration> types(CompilationUnit compilationUnit) {
        return compilationUnit.types();
    }

    /**
     * Wraps the unchecked cast necessary by non-Generics method definition
     * {@link FieldDeclaration#fragments()}.
     * 
     * @return a {@link List} of {@link VariableDeclarationFragment}.
     */
    @SuppressWarnings("unchecked")
    public static List<VariableDeclarationFragment> fragments(FieldDeclaration fieldDeclaration) {
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
            VariableDeclarationExpression expression) {
        return expression.fragments();
    }

}
