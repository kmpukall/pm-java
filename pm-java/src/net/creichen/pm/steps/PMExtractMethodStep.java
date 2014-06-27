/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm.steps;

import static net.creichen.pm.utils.APIWrapperUtil.arguments;
import static net.creichen.pm.utils.APIWrapperUtil.bodyDeclarations;
import static net.creichen.pm.utils.APIWrapperUtil.modifiers;
import static net.creichen.pm.utils.APIWrapperUtil.parameters;
import static net.creichen.pm.utils.APIWrapperUtil.statements;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.creichen.pm.PMASTNodeUtil;
import net.creichen.pm.PMProject;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

public class PMExtractMethodStep extends PMStep {

    private static Type newTypeASTNodeForTypeBinding(final AST ast, final ITypeBinding typeBinding) {
        // for now we only support simple types and primitive types

        if (typeBinding.isPrimitive()) {
            return ast.newPrimitiveType(PrimitiveType.toCode(typeBinding.getName()));
        } else if (typeBinding.isClass() || typeBinding.isInterface()) {
            return ast.newSimpleType(ast.newSimpleName(typeBinding.getName()));
        }

        return null;
    }

    private static List<SimpleName> variablesReferredToInExpression(final Expression e) {
        final List<SimpleName> result = new ArrayList<SimpleName>();

        // we find all simple names and get their bindings
        // if their binding is a variable binding and it's getDeclaringMethod()
        // returns non-null, we assume it is a local variable

        e.accept(new ASTVisitor() {
            @Override
            public boolean visit(final SimpleName simpleName) {
                final IBinding nameBinding = simpleName.resolveBinding();

                if (nameBinding instanceof IVariableBinding) {
                    final IVariableBinding variableNameBinding = (IVariableBinding) nameBinding;

                    if (variableNameBinding.getDeclaringMethod() != null) {
                        result.add(simpleName);
                    }
                }

                return false; // Simple names don't have any children
            }
        });

        return result;
    }

    private final MethodDeclaration extractedMethodDeclaration;

    private final MethodInvocation replacementMethodInvocation;

    private final List<SimpleName> namesToExtract;

    private final Expression originalExpression;

    private Expression extractedExpression;

    public PMExtractMethodStep(final PMProject project, final Expression expression) {
        super(project);

        this.namesToExtract = PMExtractMethodStep.variablesReferredToInExpression(expression);

        this.originalExpression = expression;

        this.extractedMethodDeclaration = newMethodDeclaration();

        this.replacementMethodInvocation = newMethodInvocation();

        // System.err.println("_extractedMethodDeclaration is " +
        // _extractedMethodDeclaration);

        // System.err.println("_replacementMethodInvocation is " +
        // _replacementMethodInvocation);
    }

    @Override
    public Map<ICompilationUnit, ASTRewrite> calculateTextualChange() {
        final Map<ICompilationUnit, ASTRewrite> result = new HashMap<ICompilationUnit, ASTRewrite>();

        final AST ast = this.originalExpression.getAST();

        final ASTRewrite astRewrite = ASTRewrite.create(ast);

        final TypeDeclaration containingClass = containingClass(this.originalExpression);

        final int insertionIndex = containingClass.bodyDeclarations().size();

        final ListRewrite lrw = astRewrite.getListRewrite(containingClass,
                TypeDeclaration.BODY_DECLARATIONS_PROPERTY);

        lrw.insertAt(this.extractedMethodDeclaration, insertionIndex, null /* textEditGroup */);

        astRewrite.replace(this.originalExpression, this.replacementMethodInvocation, null);

        result.put(this.getProject().findPMCompilationUnitForNode(this.originalExpression)
                .getICompilationUnit(), astRewrite);

        return result;
    }

    private TypeDeclaration containingClass(final ASTNode node) {

        ASTNode iterator = node;

        while (iterator != null) {
            if (iterator instanceof TypeDeclaration) {
                return (TypeDeclaration) iterator;
            } else {
                iterator = iterator.getParent();
            }
        }

        return null;
    }

    public List<SimpleName> getNamesToExtract() {
        return new ArrayList<SimpleName>(this.namesToExtract);
    }

    protected MethodDeclaration newMethodDeclaration() {

        final AST ast = this.originalExpression.getAST();

        final MethodDeclaration newMethodDeclaration = ast.newMethodDeclaration();

        newMethodDeclaration.setName(ast.newSimpleName("extractedMethod"));

        newMethodDeclaration.setReturnType2(PMExtractMethodStep.newTypeASTNodeForTypeBinding(ast,
                this.originalExpression.resolveTypeBinding()));

        modifiers(newMethodDeclaration)
                .add(ast.newModifier(Modifier.ModifierKeyword.FINAL_KEYWORD));

        for (final SimpleName nameToExtract : this.namesToExtract) {
            final SingleVariableDeclaration parameter = ast.newSingleVariableDeclaration();

            parameter.setName(ast.newSimpleName(nameToExtract.getIdentifier()));

            parameter.setType(PMExtractMethodStep.newTypeASTNodeForTypeBinding(ast,
                    nameToExtract.resolveTypeBinding()));

            parameters(newMethodDeclaration).add(parameter);
        }

        final Block methodBody = ast.newBlock();

        newMethodDeclaration.setBody(methodBody);

        final ReturnStatement returnStatement = ast.newReturnStatement();

        this.extractedExpression = (Expression) ASTNode.copySubtree(ast, this.originalExpression);

        returnStatement.setExpression(this.extractedExpression);

        statements(methodBody).add(returnStatement);

        return newMethodDeclaration;
    }

    protected MethodInvocation newMethodInvocation() {
        final AST ast = this.originalExpression.getAST();

        final MethodInvocation newMethodInvocation = ast.newMethodInvocation();

        newMethodInvocation.setName(ast.newSimpleName(this.extractedMethodDeclaration.getName()
                .getIdentifier()));

        for (final SimpleName nameToExtract : this.namesToExtract) {
            arguments(newMethodInvocation).add(ast.newSimpleName(nameToExtract.getIdentifier()));
        }

        return newMethodInvocation;
    }

    @Override
    public void performASTChange() {
        final TypeDeclaration containingClass = containingClass(this.originalExpression);

        bodyDeclarations(containingClass).add(this.extractedMethodDeclaration);

        this.getProject().recursivelyReplaceNodeWithCopy(this.originalExpression,
                this.extractedExpression);

        PMASTNodeUtil.replaceNodeInParent(this.originalExpression,
                this.replacementMethodInvocation);

        performNameModelChange();
        performUDModelChange();

    }

    public void performNameModelChange() {

    }

    public void performUDModelChange() {

    }
}
