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

import net.creichen.pm.PMASTNodeUtils;
import net.creichen.pm.PMNodeReference;
import net.creichen.pm.PMProject;
import net.creichen.pm.analysis.PMDef;
import net.creichen.pm.analysis.PMRDefsAnalysis;
import net.creichen.pm.analysis.PMUse;
import net.creichen.pm.models.PMNameModel;
import net.creichen.pm.models.PMUDModel;

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

public class PMSplitStep extends PMStep {

    ICompilationUnit _iCompilationUnit;

    ExpressionStatement _assignmentStatement;

    // These keep state between text and ast change
    VariableDeclarationStatement _replacementDeclarationStatement;

    Expression _initializer;
    Expression _initializerCopy;

    // This keeps state between reparses
    PMNodeReference _replacementDeclarationReference;

    public PMSplitStep(PMProject project, ExpressionStatement assignmentStatement) {
        super(project);

        _assignmentStatement = assignmentStatement;

        CompilationUnit containingCompilationUnit = (CompilationUnit) _assignmentStatement
                .getRoot();

        _iCompilationUnit = (ICompilationUnit) containingCompilationUnit.getJavaElement();

    }

    public VariableDeclarationStatement getReplacementDeclarationStatement() {
        return (VariableDeclarationStatement) _replacementDeclarationReference.getNode();
    }

    public void rewriteToReplaceAssignmentStatementWithDeclaration(ASTRewrite rewrite,
            Assignment assignment) {

        SimpleName lhs = (SimpleName) assignment.getLeftHandSide();

        AST ast = assignment.getAST();

        _initializer = assignment.getRightHandSide();
        _initializerCopy = (Expression) ASTNode.copySubtree(ast, _initializer);

        VariableDeclarationFragment fragment = ast.newVariableDeclarationFragment();
        fragment.setInitializer(_initializerCopy);

        fragment.setName(ast.newSimpleName(lhs.getIdentifier()));

        _replacementDeclarationStatement = ast.newVariableDeclarationStatement(fragment);

        VariableDeclaration originalVariableDeclaration = PMASTNodeUtils
                .localVariableDeclarationForSimpleName(lhs);

        Type type = null;

        // AAAAAAGH
        // It's hard to get a type from a variable declaration
        // This an instance where strong typing really gets in the way

        if (originalVariableDeclaration instanceof VariableDeclarationFragment) {
            ASTNode parent = originalVariableDeclaration.getParent();

            if (parent instanceof VariableDeclarationStatement) {
                type = ((VariableDeclarationStatement) parent).getType();
            } else if (parent instanceof VariableDeclarationExpression) {
                type = ((VariableDeclarationExpression) parent).getType();
            }
        } else if (originalVariableDeclaration instanceof SingleVariableDeclaration)
            type = ((SingleVariableDeclaration) originalVariableDeclaration).getType();

        _replacementDeclarationStatement.setType((Type) ASTNode.copySubtree(ast, type));

        rewrite.replace(_assignmentStatement, _replacementDeclarationStatement, null /* edit group */);
    }

    public void rewriteToRenameSimpleNameToIdentifier(ASTRewrite rewrite, SimpleName simpleName,
            String identifier) {
        rewrite.set(simpleName, SimpleName.IDENTIFIER_PROPERTY, identifier, null /* edit group */);
    }

    public MethodDeclaration findContainingMethodDeclaration(ASTNode node) {
        MethodDeclaration containingMethodDeclaration = null;

        ASTNode iterator = node;
        while ((iterator = iterator.getParent()) != null) {

            if (iterator instanceof MethodDeclaration) {
                containingMethodDeclaration = (MethodDeclaration) iterator;
            }
        }

        return containingMethodDeclaration;
    }

    public Map<ICompilationUnit, ASTRewrite> calculateTextualChange() {
        ASTRewrite astRewrite = ASTRewrite.create(_assignmentStatement.getAST());

        Assignment assignmentExpression = (Assignment) _assignmentStatement.getExpression();

        rewriteToReplaceAssignmentStatementWithDeclaration(astRewrite, assignmentExpression);

        Map<ICompilationUnit, ASTRewrite> result = new HashMap<ICompilationUnit, ASTRewrite>();

        result.put(_iCompilationUnit, astRewrite);

        return result;
    }

    public void performASTChange() {

        Assignment oldAssignmentExpression = (Assignment) _assignmentStatement.getExpression();

        MethodDeclaration containingMethodDeclaration = findContainingMethodDeclaration(oldAssignmentExpression);

        PMRDefsAnalysis reachingDefsAnalysis = new PMRDefsAnalysis(containingMethodDeclaration);

        PMDef definitionForAssignment = reachingDefsAnalysis
                .getDefinitionForDefiningNode(oldAssignmentExpression);

        Set<SimpleName> uses = new HashSet<SimpleName>();

        for (PMUse use : definitionForAssignment.getUses()) {
            SimpleName usingSimpleName = use.getSimpleName();

            uses.add(usingSimpleName);
        }

        VariableDeclarationFragment newVariableDeclarationFragment = (VariableDeclarationFragment) _replacementDeclarationStatement
                .fragments().get(0);
        ;

        PMNodeReference identifierForOldAssignment = _project
                .getReferenceForNode(oldAssignmentExpression);

        _project.recursivelyReplaceNodeWithCopy(_initializer, _initializerCopy);

        // !!!_project.removeNode(oldAssignmentExpression);
        // !!!_project.addNode(_replacementDeclarationStatement);

        PMNodeReference identifierForNewVariableDeclaration = _project
                .getReferenceForNode(newVariableDeclarationFragment);

        SimpleName oldLHS = (SimpleName) oldAssignmentExpression.getLeftHandSide();
        SimpleName newLHS = newVariableDeclarationFragment.getName();

        // Need to update UDModel to replace assignment definition with variable
        // declaration fragment definition

        PMUDModel udModel = _project.getUDModel();

        // for each use of the assignment, replace the use of the assignment
        // with the use of the declaration

        for (PMNodeReference useIdentifier : new HashSet<PMNodeReference>(
                udModel.usesForDefinition(identifierForOldAssignment))) {
            udModel.removeDefinitionIdentifierForName(identifierForOldAssignment, useIdentifier);
            udModel.addDefinitionIdentifierForName(identifierForNewVariableDeclaration,
                    useIdentifier);
        }

        udModel.deleteDefinition(identifierForOldAssignment);

        PMNameModel nameModel = _project.getNameModel();

        nameModel.removeIdentifierForName(oldLHS);

        String freshIdentifier = UUID.randomUUID().toString();

        nameModel.setIdentifierForName(freshIdentifier, newLHS);

        for (SimpleName use : uses) {

            nameModel.setIdentifierForName(freshIdentifier, use);
        }

        StructuralPropertyDescriptor location = _assignmentStatement.getLocationInParent();

        List<ASTNode> parentList = getStructuralProperty((ChildListPropertyDescriptor) location,
                _assignmentStatement.getParent());

        parentList.set(parentList.indexOf(_assignmentStatement), _replacementDeclarationStatement);

        _replacementDeclarationReference = _project
                .getReferenceForNode(_replacementDeclarationStatement);

    }

    public void updateAfterReparse() {

    }

    public void cleanup() {

    }

}
