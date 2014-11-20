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

import net.creichen.pm.analysis.reachingdefs.ReachingDefsAnalysis;
import net.creichen.pm.api.Node;
import net.creichen.pm.api.PMCompilationUnit;
import net.creichen.pm.core.PMException;
import net.creichen.pm.core.Project;
import net.creichen.pm.data.NodeStore;
import net.creichen.pm.models.defuse.DefUseModel;
import net.creichen.pm.models.defuse.Use;
import net.creichen.pm.models.name.NameModel;
import net.creichen.pm.utils.APIWrapperUtil;
import net.creichen.pm.utils.ASTQuery;

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

public class DelegateStep extends AbstractStep {

    private final ASTNode selectedNode;

    private String delegateIdentifier;

    // iVars to hold state between textual change and ast change

    private SuperMethodInvocation newSuperInvocationNode; // for delegation to super,
    private MethodInvocation selectedMethodInvocation;

    private Expression newExpressionNode;

    // ivar to hold state across reparse

    private Node newExpressionNodeReference;

    private PMCompilationUnit pmCompilationUnit;

    public DelegateStep(final Project project, final ASTNode selectedNode) {
        super(project);

        this.selectedNode = selectedNode;

        final CompilationUnit containingCompilationUnit = (CompilationUnit) this.selectedNode.getRoot();
        ICompilationUnit iCompilationUnit = (ICompilationUnit) containingCompilationUnit.getJavaElement();
        this.pmCompilationUnit = project.getPMCompilationUnit(iCompilationUnit);

    }

    @Override
    public Map<PMCompilationUnit, ASTRewrite> calculateTextualChange() {
        final Map<PMCompilationUnit, ASTRewrite> result = new HashMap<PMCompilationUnit, ASTRewrite>();

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

                rewriteToDelegateMethodInvocationToSuperInvocation(astRewrite, this.selectedMethodInvocation,
                        this.newSuperInvocationNode);

            } else {
                if (!this.delegateIdentifier.equals("")) {
                    this.newExpressionNode = ast.newSimpleName(this.delegateIdentifier);
                } else {
                    this.newExpressionNode = null;
                }

                rewriteToDelegateMethodInvocationToIdentifier(astRewrite, this.selectedMethodInvocation,
                        this.newExpressionNode);
            }

            result.put(this.pmCompilationUnit, astRewrite);

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

            getProject().recursivelyReplaceNodeWithCopy(this.selectedMethodInvocation.getName(),
                    this.newSuperInvocationNode.getName());

            final List<Expression> oldArguments = arguments(this.selectedMethodInvocation);
            final List<Expression> newArguments = arguments(this.newSuperInvocationNode);

            if (oldArguments.size() == newArguments.size()) {
                for (int i = 0; i < oldArguments.size(); i++) {
                    getProject().recursivelyReplaceNodeWithCopy(oldArguments.get(i), newArguments.get(i));
                }

            } else {
                throw new PMException("oldArguments.size != newArguments.size()");
            }

            // FIXME(dcc) Should use ASTNodeUtils.replaceNodeInParent()

            final StructuralPropertyDescriptor location = this.selectedMethodInvocation.getLocationInParent();

            // replace the selected method invocation with the new invocation
            if (location.isChildProperty()) {
                this.selectedMethodInvocation.getParent().setStructuralProperty(location, this.newSuperInvocationNode);
            } else {
                final List<ASTNode> parentList = getStructuralProperty((ChildListPropertyDescriptor) location,
                        this.selectedMethodInvocation.getParent());

                parentList.set(parentList.indexOf(this.selectedMethodInvocation), this.newSuperInvocationNode);
            }

        } else {

            if (this.newExpressionNode != null) {
                if (this.newExpressionNode instanceof Name) {

                    this.newExpressionNodeReference = NodeStore.getInstance().getReference(this.newExpressionNode);
                } else {
                    System.err.println("Unexpected new expression type " + this.newExpressionNode.getClass());
                }

            }

            // Here is where we actually change the AST

            this.selectedMethodInvocation.setExpression(this.newExpressionNode);
        }

    }

    private void rewriteToDelegateMethodInvocationToIdentifier(final ASTRewrite astRewrite,
            final MethodInvocation methodInvocation, final Expression identifierNode) {
        astRewrite
                .set(methodInvocation, MethodInvocation.EXPRESSION_PROPERTY, identifierNode, null /* textEditGroup */);
    }

    private void rewriteToDelegateMethodInvocationToSuperInvocation(final ASTRewrite astRewrite,
            final MethodInvocation methodInvocation, final Expression superInvocationNode) {
        astRewrite.replace(methodInvocation, superInvocationNode, null /*
                                                                       * edit group
                                                                       */);
    }

    public void setDelegateIdentifier(final String delegateIdentifier) {
        this.delegateIdentifier = delegateIdentifier;
    }

    private SuperMethodInvocation superMethodDelegatingMethodInvocation(final MethodInvocation invocationToDelegate) {

        final AST ast = invocationToDelegate.getAST();

        final SuperMethodInvocation superMethodInvocationNode = ast.newSuperMethodInvocation();

        superMethodInvocationNode.setStructuralProperty(SuperMethodInvocation.NAME_PROPERTY,
                ASTNode.copySubtree(ast, getStructuralProperty(MethodInvocation.NAME_PROPERTY, invocationToDelegate)));
        final List<ASTNode> argumentsProperty = getStructuralProperty(MethodInvocation.ARGUMENTS_PROPERTY,
                invocationToDelegate);
        final List<ASTNode> typeArgumentsProperty = getStructuralProperty(MethodInvocation.TYPE_ARGUMENTS_PROPERTY,
                invocationToDelegate);

        List<Expression> arguments = APIWrapperUtil.toExpressionList(ASTNode.copySubtrees(ast, argumentsProperty));
        arguments(superMethodInvocationNode).addAll(arguments);
        List<Expression> typeArguments = APIWrapperUtil.toExpressionList(ASTNode.copySubtrees(ast,
                typeArgumentsProperty));
        arguments(superMethodInvocationNode).addAll(typeArguments);

        return superMethodInvocationNode;
    }

    @Override
    public void updateAfterReparse() {
        if (this.newExpressionNodeReference != null) {
            this.newExpressionNode = (Expression) this.newExpressionNodeReference.getNode();
        }
        if (this.newExpressionNode instanceof SimpleName) {
            final SimpleName name = (SimpleName) this.newExpressionNode;
            final NameModel nameModel = getProject().getNameModel();
            final ASTNode declaringNode = getProject().findDeclaringNodeForName(name);
            if (declaringNode != null) {
                final SimpleName simpleNameForDeclaringNode = ASTQuery.resolveSimpleName(declaringNode);
                final String identifier = nameModel.getIdentifier(simpleNameForDeclaringNode);
                nameModel.setIdentifier(identifier, name);
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
            final ReachingDefsAnalysis analysis = new ReachingDefsAnalysis(methodDeclaration);
            final Use use = analysis.getUse(name);
            final DefUseModel udModel = getProject().getUDModel();
            udModel.addUse(use);
        }
        // !!! should remove old expression info from name and use/def model
        // FIXME(dcc)
    }

}
