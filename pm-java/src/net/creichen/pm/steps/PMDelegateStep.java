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
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

public class PMDelegateStep extends PMStep {

    private final ASTNode selectedNode;

    private String delegateIdentifier;

    // iVars to hold state between textual change and ast change

    private SuperMethodInvocation newSuperInvocationNode; // for delegation to super,
    private MethodInvocation selectedMethodInvocation;

    private Expression newExpressionNode;

    // ivar to hold state across reparse

    private PMNodeReference newExpressionNodeReference;

    private final ICompilationUnit iCompilationUnit;

    public PMDelegateStep(final PMProject project, final ASTNode selectedNode) {
        super(project);

        this.selectedNode = selectedNode;

        final CompilationUnit containingCompilationUnit = (CompilationUnit) this.selectedNode
                .getRoot();

        this.iCompilationUnit = (ICompilationUnit) containingCompilationUnit.getJavaElement();

    }

    @Override
    public Map<ICompilationUnit, ASTRewrite> calculateTextualChange() {
        final Map<ICompilationUnit, ASTRewrite> result = new HashMap<ICompilationUnit, ASTRewrite>();

        // we don't yet do anything fancy here; only handle the simple case.
        if (this.selectedNode instanceof MethodInvocation) {
            this.selectedMethodInvocation = (MethodInvocation) this.selectedNode;

            // create new SimpleName expression for our delegate identifier and
            // create the change
            // by setting it to be the expression of the method invocation.

            final AST ast = this.selectedMethodInvocation.getAST();

            final ASTRewrite astRewrite = ASTRewrite.create(ast);

            if (this.delegateIdentifier.equals("super")) {
                this.newExpressionNode = null;

                this.newSuperInvocationNode = superMethodDelegatingMethodInvocation(this.selectedMethodInvocation);

                rewriteToDelegateMethodInvocationToSuperInvocation(astRewrite,
                        this.selectedMethodInvocation, this.newSuperInvocationNode);

            } else {
                if (!this.delegateIdentifier.equals("")) {
                    this.newExpressionNode = ast.newSimpleName(this.delegateIdentifier);
                } else {
                    this.newExpressionNode = null;
                }

                rewriteToDelegateMethodInvocationToIdentifier(astRewrite,
                        this.selectedMethodInvocation, this.newExpressionNode);
            }

            result.put(this.iCompilationUnit, astRewrite);

        }

        return result;
    }

    public String getDelegateIdentifier() {
        return this.delegateIdentifier;
    }

    @Override
    public void performASTChange() {
        if (this.delegateIdentifier.equals("super")) {

            // since we made copies of the arguments and name properties, we
            // have to
            // match the copies up with the old versions so that we can update
            // identifiers

            this.getProject().recursivelyReplaceNodeWithCopy(this.selectedMethodInvocation.getName(),
                    this.newSuperInvocationNode.getName());

            final List<Expression> oldArguments = arguments(this.selectedMethodInvocation);
            final List<Expression> newArguments = arguments(this.newSuperInvocationNode);

            if (oldArguments.size() == newArguments.size()) {
                for (int i = 0; i < oldArguments.size(); i++) {
                    this.getProject().recursivelyReplaceNodeWithCopy(oldArguments.get(i),
                            newArguments.get(i));
                }

            } else {
                throw new RuntimeException("oldArguments.size != newArguments.size()");
            }

            // FIXME(dcc) Should use ASTNodeUtils.replaceNodeInParent()

            final StructuralPropertyDescriptor location = this.selectedMethodInvocation
                    .getLocationInParent();

            // replace the selected method invocation with the new invocation
            if (location.isChildProperty()) {
                this.selectedMethodInvocation.getParent().setStructuralProperty(location,
                        this.newSuperInvocationNode);
            } else {
                final List<ASTNode> parentList = getStructuralProperty(
                        (ChildListPropertyDescriptor) location,
                        this.selectedMethodInvocation.getParent());

                parentList.set(parentList.indexOf(this.selectedMethodInvocation),
                        this.newSuperInvocationNode);
            }

        } else {

            if (this.newExpressionNode != null) {
                if ((this.newExpressionNode instanceof Name)) {

                    this.newExpressionNodeReference = this.getProject()
                            .getReferenceForNode(this.newExpressionNode);
                } else {
                    System.err.println("Unexpected new expression type "
                            + this.newExpressionNode.getClass());
                }

            }

            // Here is where we actually change the AST

            this.selectedMethodInvocation.setExpression(this.newExpressionNode);
        }

    }

    void rewriteToDelegateMethodInvocationToIdentifier(final ASTRewrite astRewrite,
            final MethodInvocation methodInvocation, final Expression identifierNode) {
        astRewrite
                .set(methodInvocation, MethodInvocation.EXPRESSION_PROPERTY, identifierNode, null /* textEditGroup */);
    }

    void rewriteToDelegateMethodInvocationToSuperInvocation(final ASTRewrite astRewrite,
            final MethodInvocation methodInvocation, final Expression superInvocationNode) {
        astRewrite.replace(methodInvocation, superInvocationNode, null /*
                                                                        * edit group
                                                                        */);
    }

    public void setDelegateIdentifier(final String delegateIdentifier) {
        this.delegateIdentifier = delegateIdentifier;
    }

    SuperMethodInvocation superMethodDelegatingMethodInvocation(
            final MethodInvocation invocationToDelegate) {

        final AST ast = invocationToDelegate.getAST();

        final SuperMethodInvocation superMethodInvocationNode = ast.newSuperMethodInvocation();

        superMethodInvocationNode
                .setStructuralProperty(SuperMethodInvocation.NAME_PROPERTY, ASTNode
                        .copySubtree(
                                ast,
                                getStructuralProperty(MethodInvocation.NAME_PROPERTY,
                                        invocationToDelegate)));
        final List<ASTNode> argumentsProperty = getStructuralProperty(
                MethodInvocation.ARGUMENTS_PROPERTY, invocationToDelegate);
        final List<ASTNode> typeArgumentsProperty = getStructuralProperty(
                MethodInvocation.TYPE_ARGUMENTS_PROPERTY, invocationToDelegate);

        arguments(superMethodInvocationNode).addAll(ASTNode.copySubtrees(ast, argumentsProperty));
        arguments(superMethodInvocationNode).addAll(
                ASTNode.copySubtrees(ast, typeArgumentsProperty));

        return superMethodInvocationNode;
    }

    @Override
    public void updateAfterReparse() {

        if (this.newExpressionNodeReference != null) {
            this.newExpressionNode = (Expression) this.newExpressionNodeReference.getNode();
        }

        if (this.newExpressionNode instanceof SimpleName) {

            final SimpleName name = (SimpleName) this.newExpressionNode;

            final PMNameModel nameModel = this.getProject().getNameModel();

            final ASTNode declaringNode = this.getProject().findDeclaringNodeForName(name);

            if (declaringNode != null) {
                final SimpleName simpleNameForDeclaringNode = this.getProject()
                        .simpleNameForDeclaringNode(declaringNode);

                final String identifier = nameModel.identifierForName(simpleNameForDeclaringNode);

                nameModel.setIdentifierForName(identifier, name);

            }

            // Now update use-def model

            MethodDeclaration methodDeclaration = null;

            ASTNode iterator = name.getParent();

            do {
                if (iterator instanceof MethodDeclaration) {
                    methodDeclaration = (MethodDeclaration) iterator;
                    break;
                } else {
                    iterator = iterator.getParent();
                }
            } while (iterator != null);

            final PMRDefsAnalysis analysis = new PMRDefsAnalysis(methodDeclaration);

            final PMUse use = analysis.useForSimpleName(name);

            final PMUDModel udModel = this.getProject().getUDModel();

            udModel.addUseToModel(use);

        }
        // !!! should remove old expression info from name and use/def model
        // FIXME(dcc)
    }

}
