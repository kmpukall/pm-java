/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm.steps;

import static net.creichen.pm.utils.APIWrapperUtil.getStructuralProperty;
import static net.creichen.pm.utils.ASTQuery.findParent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.creichen.pm.analysis.ReachingDefsAnalysis;
import net.creichen.pm.api.Node;
import net.creichen.pm.api.PMCompilationUnit;
import net.creichen.pm.core.Project;
import net.creichen.pm.data.NodeStore;
import net.creichen.pm.models.defuse.Def;
import net.creichen.pm.models.defuse.DefUseModel;
import net.creichen.pm.models.defuse.Use;
import net.creichen.pm.models.name.NameModel;
import net.creichen.pm.utils.ASTQuery;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.ChildListPropertyDescriptor;
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

    private final PMCompilationUnit compilationUnit;

    private final ExpressionStatement assignmentStatement;

    // These keep state between text and ast change
    private VariableDeclarationStatement replacementDeclarationStatement;

    private Expression initializer;
    private Expression initializerCopy;

    // This keeps state between reparses
    private Node replacementDeclarationReference;

    public SplitStep(final Project project, final ExpressionStatement assignmentStatement) {
        super(project);
        this.assignmentStatement = assignmentStatement;
        this.compilationUnit = project.findPMCompilationUnitForNode(assignmentStatement);
    }

    @Override
    public Map<PMCompilationUnit, ASTRewrite> calculateTextualChange() {
        final ASTRewrite astRewrite = ASTRewrite.create(this.assignmentStatement.getAST());

        final Assignment assignmentExpression = (Assignment) this.assignmentStatement.getExpression();

        rewriteToReplaceAssignmentStatementWithDeclaration(astRewrite, assignmentExpression);

        final Map<PMCompilationUnit, ASTRewrite> result = new HashMap<PMCompilationUnit, ASTRewrite>();

        result.put(this.compilationUnit, astRewrite);

        return result;
    }

    public VariableDeclarationStatement getReplacementDeclarationStatement() {
        return (VariableDeclarationStatement) this.replacementDeclarationReference.getNode();
    }

    @Override
    public void performASTChange() {
        final Assignment oldAssignmentExpression = (Assignment) this.assignmentStatement.getExpression();
        final MethodDeclaration containingMethodDeclaration = findParent(oldAssignmentExpression,
                MethodDeclaration.class);

        final ReachingDefsAnalysis reachingDefsAnalysis = new ReachingDefsAnalysis(containingMethodDeclaration);
        final Def definitionForAssignment = reachingDefsAnalysis.getDefinitionForDefiningNode(oldAssignmentExpression);
        final Set<SimpleName> uses = new HashSet<SimpleName>();
        for (final Use use : definitionForAssignment.getUses()) {
            uses.add(use.getSimpleName());
        }
        final VariableDeclarationFragment newVariableDeclarationFragment = (VariableDeclarationFragment) this.replacementDeclarationStatement
                .fragments().get(0);
        final Node identifierForOldAssignment = NodeStore.getInstance().getReference(oldAssignmentExpression);
        getProject().recursivelyReplaceNodeWithCopy(this.initializer, this.initializerCopy);
        final Node identifierForNewVariableDeclaration = NodeStore.getInstance().getReference(
                newVariableDeclarationFragment);

        final SimpleName oldLHS = (SimpleName) oldAssignmentExpression.getLeftHandSide();
        final SimpleName newLHS = newVariableDeclarationFragment.getName();

        // Need to update UDModel to replace assignment definition with variable
        // declaration fragment definition
        final DefUseModel udModel = getProject().getUDModel();

        // for each use of the assignment, replace the use of the assignment
        // with the use of the declaration
        for (final Node useIdentifier : udModel.getUsesByDefinition(identifierForOldAssignment)) {
            udModel.removeMapping(identifierForOldAssignment, useIdentifier);
            udModel.addMapping(identifierForNewVariableDeclaration, useIdentifier);
        }

        udModel.deleteDefinition(identifierForOldAssignment);

        final NameModel nameModel = getProject().getNameModel();

        nameModel.removeIdentifier(oldLHS);

        final String freshIdentifier = UUID.randomUUID().toString();

        nameModel.setIdentifier(freshIdentifier, newLHS);

        for (final SimpleName use : uses) {

            nameModel.setIdentifier(freshIdentifier, use);
        }

        final StructuralPropertyDescriptor location = this.assignmentStatement.getLocationInParent();

        final List<ASTNode> parentList = getStructuralProperty((ChildListPropertyDescriptor) location,
                this.assignmentStatement.getParent());

        parentList.set(parentList.indexOf(this.assignmentStatement), this.replacementDeclarationStatement);

        this.replacementDeclarationReference = NodeStore.getInstance().getReference(
                this.replacementDeclarationStatement);

    }

    @Override
    public void updateAfterReparse() {

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

}
