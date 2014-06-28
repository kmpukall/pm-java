/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm.steps;

import static net.creichen.pm.utils.APIWrapperUtil.getStructuralProperty;

import java.util.*;

import net.creichen.pm.PMASTNodeUtil;
import net.creichen.pm.PMNodeReference;
import net.creichen.pm.PMProject;
import net.creichen.pm.analysis.Def;
import net.creichen.pm.analysis.RDefsAnalysis;
import net.creichen.pm.analysis.PMUse;
import net.creichen.pm.models.NameModel;
import net.creichen.pm.models.DefUseModel;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

public class SplitStep extends PMStep {

    private final ICompilationUnit iCompilationUnit;

    private final ExpressionStatement assignmentStatement;

    // These keep state between text and ast change
    private VariableDeclarationStatement replacementDeclarationStatement;

    private Expression initializer;
    private Expression initializerCopy;

    // This keeps state between reparses
    private PMNodeReference replacementDeclarationReference;

    public SplitStep(final PMProject project, final ExpressionStatement assignmentStatement) {
        super(project);

        this.assignmentStatement = assignmentStatement;

        final CompilationUnit containingCompilationUnit = (CompilationUnit) this.assignmentStatement
                .getRoot();

        this.iCompilationUnit = (ICompilationUnit) containingCompilationUnit.getJavaElement();

    }

    @Override
    public Map<ICompilationUnit, ASTRewrite> calculateTextualChange() {
        final ASTRewrite astRewrite = ASTRewrite.create(this.assignmentStatement.getAST());

        final Assignment assignmentExpression = (Assignment) this.assignmentStatement
                .getExpression();

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

        final Assignment oldAssignmentExpression = (Assignment) this.assignmentStatement
                .getExpression();

        final MethodDeclaration containingMethodDeclaration = findContainingMethodDeclaration(oldAssignmentExpression);

        final RDefsAnalysis reachingDefsAnalysis = new RDefsAnalysis(
                containingMethodDeclaration);

        final Def definitionForAssignment = reachingDefsAnalysis
                .getDefinitionForDefiningNode(oldAssignmentExpression);

        final Set<SimpleName> uses = new HashSet<SimpleName>();

        for (final PMUse use : definitionForAssignment.getUses()) {
            final SimpleName usingSimpleName = use.getSimpleName();

            uses.add(usingSimpleName);
        }

        final VariableDeclarationFragment newVariableDeclarationFragment = (VariableDeclarationFragment) this.replacementDeclarationStatement
                .fragments().get(0);

        final PMNodeReference identifierForOldAssignment = getProject().getReferenceForNode(
                oldAssignmentExpression);

        getProject().recursivelyReplaceNodeWithCopy(this.initializer, this.initializerCopy);

        // !!!_project.removeNode(oldAssignmentExpression);
        // !!!_project.addNode(_replacementDeclarationStatement);

        final PMNodeReference identifierForNewVariableDeclaration = getProject()
                .getReferenceForNode(newVariableDeclarationFragment);

        final SimpleName oldLHS = (SimpleName) oldAssignmentExpression.getLeftHandSide();
        final SimpleName newLHS = newVariableDeclarationFragment.getName();

        // Need to update UDModel to replace assignment definition with variable
        // declaration fragment definition

        final DefUseModel udModel = getProject().getUDModel();

        // for each use of the assignment, replace the use of the assignment
        // with the use of the declaration

        for (final PMNodeReference useIdentifier : new HashSet<PMNodeReference>(
                udModel.usesForDefinition(identifierForOldAssignment))) {
            udModel.removeDefinitionIdentifierForName(identifierForOldAssignment, useIdentifier);
            udModel.addDefinitionIdentifierForName(identifierForNewVariableDeclaration,
                    useIdentifier);
        }

        udModel.deleteDefinition(identifierForOldAssignment);

        final NameModel nameModel = getProject().getNameModel();

        nameModel.removeIdentifierForName(oldLHS);

        final String freshIdentifier = UUID.randomUUID().toString();

        nameModel.setIdentifierForName(freshIdentifier, newLHS);

        for (final SimpleName use : uses) {

            nameModel.setIdentifierForName(freshIdentifier, use);
        }

        final StructuralPropertyDescriptor location = this.assignmentStatement
                .getLocationInParent();

        final List<ASTNode> parentList = getStructuralProperty(
                (ChildListPropertyDescriptor) location, this.assignmentStatement.getParent());

        parentList.set(parentList.indexOf(this.assignmentStatement),
                this.replacementDeclarationStatement);

        this.replacementDeclarationReference = getProject().getReferenceForNode(
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

        final VariableDeclaration originalVariableDeclaration = PMASTNodeUtil
                .localVariableDeclarationForSimpleName(lhs);

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
                                                                                              * edit
                                                                                              * group
                                                                                              */);
    }

    @Override
    public void updateAfterReparse() {

    }

}
