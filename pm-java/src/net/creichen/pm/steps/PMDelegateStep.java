/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm.steps;

import static net.creichen.pm.utils.APIWrapperUtil.arguments;
import static net.creichen.pm.utils.APIWrapperUtil.getStructuralProperty;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.creichen.pm.PMNodeReference;
import net.creichen.pm.PMProject;
import net.creichen.pm.analysis.PMRDefsAnalysis;
import net.creichen.pm.analysis.PMUse;
import net.creichen.pm.models.PMNameModel;
import net.creichen.pm.models.PMUDModel;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ChildListPropertyDescriptor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

public class PMDelegateStep extends PMStep {

    ASTNode _selectedNode;

    String _delegateIdentifier;

    // iVars to hold state between textual change and ast change

    SuperMethodInvocation _newSuperInvocationNode; // for delegation to super,
    MethodInvocation _selectedMethodInvocation;

    Expression _newExpressionNode;

    // ivar to hold state across reparse

    PMNodeReference _newExpressionNodeReference;

    ICompilationUnit _iCompilationUnit;

    public PMDelegateStep(PMProject project, ASTNode selectedNode) {
        super(project);

        _selectedNode = selectedNode;

        CompilationUnit containingCompilationUnit = (CompilationUnit) _selectedNode.getRoot();

        _iCompilationUnit = (ICompilationUnit) containingCompilationUnit.getJavaElement();

    }

    public String getDelegateIdentifier() {
        return _delegateIdentifier;
    }

    public void setDelegateIdentifier(String delegateIdentifier) {
        _delegateIdentifier = delegateIdentifier;
    }

    void rewriteToDelegateMethodInvocationToIdentifier(ASTRewrite astRewrite,
            MethodInvocation methodInvocation, Expression identifierNode) {
        astRewrite
                .set(methodInvocation, MethodInvocation.EXPRESSION_PROPERTY, identifierNode, null /* textEditGroup */);
    }

    void rewriteToDelegateMethodInvocationToSuperInvocation(ASTRewrite astRewrite,
            MethodInvocation methodInvocation, Expression superInvocationNode) {
        astRewrite.replace(methodInvocation, superInvocationNode, null /*
                                                                        * edit group
                                                                        */);
    }

    SuperMethodInvocation superMethodDelegatingMethodInvocation(
            MethodInvocation invocationToDelegate) {

        AST ast = invocationToDelegate.getAST();

        SuperMethodInvocation superMethodInvocationNode = ast.newSuperMethodInvocation();

        superMethodInvocationNode
                .setStructuralProperty(SuperMethodInvocation.NAME_PROPERTY, ASTNode
                        .copySubtree(
                                ast,
                                getStructuralProperty(MethodInvocation.NAME_PROPERTY,
                                        invocationToDelegate)));
        List<ASTNode> argumentsProperty = getStructuralProperty(
                MethodInvocation.ARGUMENTS_PROPERTY, invocationToDelegate);
        List<ASTNode> typeArgumentsProperty = getStructuralProperty(
                MethodInvocation.TYPE_ARGUMENTS_PROPERTY, invocationToDelegate);

        arguments(superMethodInvocationNode).addAll(ASTNode.copySubtrees(ast, argumentsProperty));
        arguments(superMethodInvocationNode).addAll(
                ASTNode.copySubtrees(ast, typeArgumentsProperty));

        return superMethodInvocationNode;
    }

    public Map<ICompilationUnit, ASTRewrite> calculateTextualChange() {
        Map<ICompilationUnit, ASTRewrite> result = new HashMap<ICompilationUnit, ASTRewrite>();

        // we don't yet do anything fancy here; only handle the simple case.
        if (_selectedNode instanceof MethodInvocation) {
            _selectedMethodInvocation = (MethodInvocation) _selectedNode;

            // create new SimpleName expression for our delegate identifier and
            // create the change
            // by setting it to be the expression of the method invocation.

            AST ast = _selectedMethodInvocation.getAST();

            ASTRewrite astRewrite = ASTRewrite.create(ast);

            if (_delegateIdentifier.equals("super")) {
                _newExpressionNode = null;

                _newSuperInvocationNode = superMethodDelegatingMethodInvocation(_selectedMethodInvocation);

                rewriteToDelegateMethodInvocationToSuperInvocation(astRewrite,
                        _selectedMethodInvocation, _newSuperInvocationNode);

            } else {
                if (!_delegateIdentifier.equals("")) {
                    _newExpressionNode = ast.newSimpleName(_delegateIdentifier);
                } else {
                    _newExpressionNode = null;
                }

                rewriteToDelegateMethodInvocationToIdentifier(astRewrite,
                        _selectedMethodInvocation, _newExpressionNode);
            }

            result.put(_iCompilationUnit, astRewrite);

        }

        return result;
    }

    public void performASTChange() {
        if (_delegateIdentifier.equals("super")) {

            // since we made copies of the arguments and name properties, we
            // have to
            // match the copies up with the old versions so that we can update
            // identifiers

            _project.recursivelyReplaceNodeWithCopy(_selectedMethodInvocation.getName(),
                    _newSuperInvocationNode.getName());

            List<Expression> oldArguments = arguments(_selectedMethodInvocation);
            List<Expression> newArguments = arguments(_newSuperInvocationNode);

            if (oldArguments.size() == newArguments.size()) {
                for (int i = 0; i < oldArguments.size(); i++) {
                    _project.recursivelyReplaceNodeWithCopy((Expression) oldArguments.get(i),
                            (Expression) newArguments.get(i));
                }

            } else {
                throw new RuntimeException("oldArguments.size != newArguments.size()");
            }

            // FIXME(dcc) Should use ASTNodeUtils.replaceNodeInParent()

            StructuralPropertyDescriptor location = _selectedMethodInvocation.getLocationInParent();

            // replace the selected method invocation with the new invocation
            if (location.isChildProperty()) {
                _selectedMethodInvocation.getParent().setStructuralProperty(location,
                        _newSuperInvocationNode);
            } else {
                List<ASTNode> parentList = getStructuralProperty(
                        (ChildListPropertyDescriptor) location,
                        _selectedMethodInvocation.getParent());

                parentList.set(parentList.indexOf(_selectedMethodInvocation),
                        _newSuperInvocationNode);
            }

        } else {

            if (_newExpressionNode != null) {
                if ((_newExpressionNode instanceof Name)) {

                    _newExpressionNodeReference = _project.getReferenceForNode(_newExpressionNode);
                } else
                    System.err.println("Unexpected new expression type "
                            + _newExpressionNode.getClass());

            }

            // Here is where we actually change the AST

            _selectedMethodInvocation.setExpression(_newExpressionNode);
        }

    }

    public void updateAfterReparse() {

        if (_newExpressionNodeReference != null)
            _newExpressionNode = (Expression) _newExpressionNodeReference.getNode();

        if (_newExpressionNode instanceof SimpleName) {

            SimpleName name = (SimpleName) _newExpressionNode;

            PMNameModel nameModel = _project.getNameModel();

            ASTNode declaringNode = _project.findDeclaringNodeForName(name);

            if (declaringNode != null) {
                SimpleName simpleNameForDeclaringNode = _project
                        .simpleNameForDeclaringNode(declaringNode);

                String identifier = nameModel.identifierForName(simpleNameForDeclaringNode);

                nameModel.setIdentifierForName(identifier, name);

            }

            // Now update use-def model

            MethodDeclaration methodDeclaration = null;

            ASTNode iterator = name.getParent();

            do {
                if (iterator instanceof MethodDeclaration) {
                    methodDeclaration = (MethodDeclaration) iterator;
                    break;
                } else
                    iterator = iterator.getParent();
            } while (iterator != null);

            PMRDefsAnalysis analysis = new PMRDefsAnalysis(methodDeclaration);

            PMUse use = analysis.useForSimpleName(name);

            PMUDModel udModel = _project.getUDModel();

            udModel.addUseToModel(use);

        } else if (_newExpressionNode == null) {
            // !!! should remove old expression info from name and use/def model
            // FIXME(dcc)

        }
    }

}
