/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm.steps;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.creichen.pm.PMASTNodeUtils;
import net.creichen.pm.PMProject;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

public class PMExtractMethodStep extends PMStep {

	MethodDeclaration _extractedMethodDeclaration;

	MethodInvocation _replacementMethodInvocation;

	List<SimpleName> _namesToExtract;

	Expression _originalExpression;

	Expression _extractedExpression;

	public PMExtractMethodStep(PMProject project, Expression expression) {
		super(project);

		_namesToExtract = PMExtractMethodStep
				.variablesReferredToInExpression(expression);

		_originalExpression = expression;

		_extractedMethodDeclaration = newMethodDeclaration();

		_replacementMethodInvocation = newMethodInvocation();

		// System.err.println("_extractedMethodDeclaration is " +
		// _extractedMethodDeclaration);

		// System.err.println("_replacementMethodInvocation is " +
		// _replacementMethodInvocation);
	}

	public List<SimpleName> getNamesToExtract() {
		return new ArrayList(_namesToExtract);
	}

	protected MethodDeclaration newMethodDeclaration() {

		AST ast = _originalExpression.getAST();

		MethodDeclaration newMethodDeclaration = ast.newMethodDeclaration();

		newMethodDeclaration.setName(ast.newSimpleName("extractedMethod"));

		newMethodDeclaration.setReturnType2(PMExtractMethodStep
				.newTypeASTNodeForTypeBinding(ast,
						_originalExpression.resolveTypeBinding()));

		newMethodDeclaration.modifiers().add(
				ast.newModifier(Modifier.ModifierKeyword.FINAL_KEYWORD));

		for (SimpleName nameToExtract : _namesToExtract) {
			SingleVariableDeclaration parameter = ast
					.newSingleVariableDeclaration();

			parameter.setName(ast.newSimpleName(nameToExtract.getIdentifier()));

			parameter.setType(PMExtractMethodStep.newTypeASTNodeForTypeBinding(
					ast, nameToExtract.resolveTypeBinding()));

			newMethodDeclaration.parameters().add(parameter);
		}

		Block methodBody = ast.newBlock();

		newMethodDeclaration.setBody(methodBody);

		ReturnStatement returnStatement = ast.newReturnStatement();

		_extractedExpression = (Expression) ASTNode.copySubtree(ast,
				_originalExpression);

		returnStatement.setExpression(_extractedExpression);

		methodBody.statements().add(returnStatement);

		return newMethodDeclaration;
	}

	protected MethodInvocation newMethodInvocation() {
		AST ast = _originalExpression.getAST();

		MethodInvocation newMethodInvocation = ast.newMethodInvocation();

		newMethodInvocation.setName(ast
				.newSimpleName(_extractedMethodDeclaration.getName()
						.getIdentifier()));

		for (SimpleName nameToExtract : _namesToExtract) {
			newMethodInvocation.arguments().add(
					ast.newSimpleName(nameToExtract.getIdentifier()));
		}

		return newMethodInvocation;
	}

	private static Type newTypeASTNodeForTypeBinding(AST ast,
			ITypeBinding typeBinding) {
		// for now we only support simple types and primitive types

		if (typeBinding.isPrimitive()) {
			return ast.newPrimitiveType(PrimitiveType.toCode(typeBinding
					.getName()));
		} else if (typeBinding.isClass() || typeBinding.isInterface()) {
			return ast.newSimpleType(ast.newSimpleName(typeBinding.getName()));
		}

		return null;
	}

	private static List<SimpleName> variablesReferredToInExpression(Expression e) {
		final List<SimpleName> result = new ArrayList<SimpleName>();

		// we find all simple names and get their bindings
		// if their binding is a variable binding and it's getDeclaringMethod()
		// returns non-null, we assume it is a local variable

		e.accept(new ASTVisitor() {
			public boolean visit(SimpleName simpleName) {
				IBinding nameBinding = simpleName.resolveBinding();

				if (nameBinding instanceof IVariableBinding) {
					IVariableBinding variableNameBinding = (IVariableBinding) nameBinding;

					if (variableNameBinding.getDeclaringMethod() != null)
						result.add(simpleName);
				}

				return false; // Simple names don't have any children
			}
		});

		return result;
	}

	private TypeDeclaration containingClass(ASTNode node) {

		ASTNode iterator = node;

		while (iterator != null) {
			if (iterator instanceof TypeDeclaration)
				return (TypeDeclaration) iterator;
			else
				iterator = iterator.getParent();
		}

		return null;
	}

	public Map<ICompilationUnit, ASTRewrite> calculateTextualChange() {
		Map<ICompilationUnit, ASTRewrite> result = new HashMap<ICompilationUnit, ASTRewrite>();

		AST ast = _originalExpression.getAST();

		ASTRewrite astRewrite = ASTRewrite.create(ast);

		TypeDeclaration containingClass = containingClass(_originalExpression);

		int insertionIndex = containingClass.bodyDeclarations().size();

		ListRewrite lrw = astRewrite.getListRewrite(containingClass,
				TypeDeclaration.BODY_DECLARATIONS_PROPERTY);

		lrw.insertAt(_extractedMethodDeclaration, insertionIndex, null /* textEditGroup */);

		astRewrite.replace(_originalExpression, _replacementMethodInvocation,
				null);

		result.put(_project.findPMCompilationUnitForNode(_originalExpression)
				.getICompilationUnit(), astRewrite);

		return result;
	}

	public void performNameModelChange() {

	}

	public void performUDModelChange() {

	}

	public void performASTChange() {
		TypeDeclaration containingClass = containingClass(_originalExpression);

		containingClass.bodyDeclarations().add(_extractedMethodDeclaration);

		_project.recursivelyReplaceNodeWithCopy(_originalExpression,
				_extractedExpression);

		PMASTNodeUtils.replaceNodeInParent(_originalExpression,
				_replacementMethodInvocation);

		performNameModelChange();
		performUDModelChange();

	}
}
