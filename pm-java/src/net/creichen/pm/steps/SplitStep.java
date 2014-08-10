/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm.steps;

import static net.creichen.pm.utils.APIWrapperUtil.getStructuralProperty;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.creichen.pm.analysis.ASTQuery;
import net.creichen.pm.analysis.Def;
import net.creichen.pm.analysis.ReachingDefsAnalysis;
import net.creichen.pm.analysis.Use;
import net.creichen.pm.api.NodeReference;
import net.creichen.pm.data.NodeReferenceStore;
import net.creichen.pm.models.DefUseModel;
import net.creichen.pm.models.NameModel;
import net.creichen.pm.models.Project;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.ChildListPropertyDescriptor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

public class SplitStep extends Step {

    private final ICompilationUnit iCompilationUnit;

    private final ExpressionStatement assignmentStatement;

    // These keep state between text and ast change
    private VariableDeclarationStatement replacementDeclarationStatement;

    private Expression initializer;
    private Expression initializerCopy;

    // This keeps state between reparses
    private NodeReference replacementDeclarationReference;

    public SplitStep(final Project project, final ExpressionStatement assignmentStatement) {
        super(project);

        this.assignmentStatement = assignmentStatement;

        final CompilationUnit containingCompilationUnit = (CompilationUnit) this.assignmentStatement.getRoot();

        this.iCompilationUnit = (ICompilationUnit) containingCompilationUnit.getJavaElement();

    }

    @Override
    public Map<ICompilationUnit, ASTRewrite> calculateTextualChange() {
        final ASTRewrite astRewrite = ASTRewrite.create(this.assignmentStatement.getAST());

        final Assignment assignmentExpression = (Assignment) this.assignmentStatement.getExpression();

        rewriteToReplaceAssignmentStatementWithDeclaration(astRewrite, assignmentExpression);

        final Map<ICompilationUnit, ASTRewrite> result = new HashMap<ICompilationUnit, ASTRewrite>();

        result.put(this.iCompilationUnit, astRewrite);

        return result;
    }

    @Override
    public void cleanup() {

    }

    private MethodDeclaration findContainingMethodDeclaration(final ASTNode node) {
        MethodDeclaration containingMethodDeclaration = null;

        ASTNode iterator = node;
        while ((iterator = iterator.getParent()) != null) {

            if (iterator instanceof MethodDeclaration) {
                containingMethodDeclaration = (MethodDeclaration) iterator;
            }
        }

        return containingMethodDeclaration;
    }

    public VariableDeclarationStatement getReplacementDeclarationStatement() {
        return (VariableDeclarationStatement) this.replacementDeclarationReference.getNode();
    }

    @Override
    public void performASTChange() {

        final Assignment oldAssignmentExpression = (Assignment) this.assignmentStatement.getExpression();

        final MethodDeclaration containingMethodDeclaration = findContainingMethodDeclaration(oldAssignmentExpression);

        final ReachingDefsAnalysis reachingDefsAnalysis = new ReachingDefsAnalysis(containingMethodDeclaration);

        final Def definitionForAssignment = reachingDefsAnalysis.getDefinitionForDefiningNode(oldAssignmentExpression);

        final Set<SimpleName> uses = new HashSet<SimpleName>();

        for (final Use use : definitionForAssignment.getUses()) {
            final SimpleName usingSimpleName = use.getSimpleName();

            uses.add(usingSimpleName);
        }

        final VariableDeclarationFragment newVariableDeclarationFragment = (VariableDeclarationFragment) this.replacementDeclarationStatement
                .fragments().get(0);

        final NodeReference identifierForOldAssignment = NodeReferenceStore.getInstance().getReferenceForNode(
                oldAssignmentExpression);

        getProject().recursivelyReplaceNodeWithCopy(this.initializer, this.initializerCopy);

        // !!!_project.removeNode(oldAssignmentExpression);
        // !!!_project.addNode(_replacementDeclarationStatement);

        final NodeReference identifierForNewVariableDeclaration = NodeReferenceStore.getInstance().getReferenceForNode(
                newVariableDeclarationFragment);

        final SimpleName oldLHS = (SimpleName) oldAssignmentExpression.getLeftHandSide();
        final SimpleName newLHS = newVariableDeclarationFragment.getName();

        // Need to update UDModel to replace assignment definition with variable
        // declaration fragment definition

        final DefUseModel udModel = getProject().getUDModel();

        // for each use of the assignment, replace the use of the assignment
        // with the use of the declaration

        for (final NodeReference useIdentifier : new HashSet<NodeReference>(
                udModel.usesForDefinition(identifierForOldAssignment))) {
            udModel.removeDefinitionIdentifierForName(identifierForOldAssignment, useIdentifier);
            udModel.addDefinitionIdentifierForName(identifierForNewVariableDeclaration, useIdentifier);
        }

        udModel.deleteDefinition(identifierForOldAssignment);

        final NameModel nameModel = getProject().getNameModel();

        nameModel.removeIdentifierForName(oldLHS);

        final String freshIdentifier = UUID.randomUUID().toString();

        nameModel.setIdentifierForName(freshIdentifier, newLHS);

        for (final SimpleName use : uses) {

            nameModel.setIdentifierForName(freshIdentifier, use);
        }

        final StructuralPropertyDescriptor location = this.assignmentStatement.getLocationInParent();

        final List<ASTNode> parentList = getStructuralProperty((ChildListPropertyDescriptor) location,
                this.assignmentStatement.getParent());

        parentList.set(parentList.indexOf(this.assignmentStatement), this.replacementDeclarationStatement);

        this.replacementDeclarationReference = NodeReferenceStore.getInstance().getReferenceForNode(
                this.replacementDeclarationStatement);

    }

    private void rewriteToReplaceAssignmentStatementWithDeclaration(final ASTRewrite rewrite,
            final Assignment assignment) {

        final SimpleName lhs = (SimpleName) assignment.getLeftHandSide();

        final AST ast = assignment.getAST();

        this.initializer = assignment.getRightHandSide();
        this.initializerCopy = (Expression) ASTNode.copySubtree(ast, this.initializer);

        final VariableDeclarationFragment fragment = ast.newVariableDeclarationFragment();
        fragment.setInitializer(this.initializerCopy);

        fragment.setName(ast.newSimpleName(lhs.getIdentifier()));

        this.replacementDeclarationStatement = ast.newVariableDeclarationStatement(fragment);

        final VariableDeclaration originalVariableDeclaration = ASTQuery.localVariableDeclarationForSimpleName(lhs);

        Type type = null;

        // AAAAAAGH
        // It's hard to get a type from a variable declaration
        // This an instance where strong typing really gets in the way

        if (originalVariableDeclaration instanceof VariableDeclarationFragment) {
            final ASTNode parent = originalVariableDeclaration.getParent();

            if (parent instanceof VariableDeclarationStatement) {
                type = ((VariableDeclarationStatement) parent).getType();
            } else if (parent instanceof VariableDeclarationExpression) {
                type = ((VariableDeclarationExpression) parent).getType();
            }
        } else if (originalVariableDeclaration instanceof SingleVariableDeclaration) {
            type = ((SingleVariableDeclaration) originalVariableDeclaration).getType();
        }

        this.replacementDeclarationStatement.setType((Type) ASTNode.copySubtree(ast, type));

        rewrite.replace(this.assignmentStatement, this.replacementDeclarationStatement, null /*
                                                                                              * edit group
                                                                                              */);
    }

    @Override
    public void updateAfterReparse() {

    }

}
