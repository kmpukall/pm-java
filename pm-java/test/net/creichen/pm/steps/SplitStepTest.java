/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm.steps;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Set;

import net.creichen.pm.api.NodeReference;
import net.creichen.pm.data.NodeReferenceStore;
import net.creichen.pm.models.DefUseModel;
import net.creichen.pm.models.NameModel;
import net.creichen.pm.tests.PMTest;
import net.creichen.pm.utils.ASTQuery;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.junit.Test;

public class SplitStepTest extends PMTest {

    @Test
    public void testSimplestCase() throws JavaModelException {
        final ICompilationUnit iCompilationUnit = createCompilationUnit("", "S.java",
                "public class S { void m() {int x; x = 7; x = 5; System.out.println(x);} }");

        final Assignment secondAssignment = ASTQuery.findAssignmentInMethod(1, "m", 0, "S", 0,
                getProject().getCompilationUnit(iCompilationUnit));

        final ExpressionStatement assignmentStatement = (ExpressionStatement) secondAssignment.getParent();

        final SplitStep step = new SplitStep(getProject(), assignmentStatement);

        step.applyAllAtOnce();

        // We have five outputs that we care about: the source, the updated name
        // model,
        // the DU/UD model, replacement declaration statement node, and the
        // inconsistencies

        // Source test
        assertTrue(matchesSource(
                "public class S { void m() {int x; x = 7; int x = 5; System.out.println(x);} }",
                iCompilationUnit.getSource()));

        // Name model test

        final NameModel nameModel = getProject().getNameModel();

        // First two occurrences of x should have same identifier
        // Second two occurrences of x should have same identifier (different
        // from first identifier)

        final SimpleName firstX = ASTQuery.findSimpleNameByIdentifier("x", 0, "m", 0, "S", 0, getProject()
                .getCompilationUnit(iCompilationUnit));
        final SimpleName secondX = ASTQuery.findSimpleNameByIdentifier("x", 1, "m", 0, "S", 0, getProject()
                .getCompilationUnit(iCompilationUnit));

        assertNotNull(nameModel.getIdentifierForName(firstX));
        assertNotNull(nameModel.getIdentifierForName(secondX));

        assertEquals(nameModel.getIdentifierForName(firstX), nameModel.getIdentifierForName(secondX));

        final SimpleName thirdX = ASTQuery.findSimpleNameByIdentifier("x", 2, "m", 0, "S", 0, getProject()
                .getCompilationUnit(iCompilationUnit));
        final SimpleName fourthX = ASTQuery.findSimpleNameByIdentifier("x", 3, "m", 0, "S", 0, getProject()
                .getCompilationUnit(iCompilationUnit));

        assertNotNull(nameModel.getIdentifierForName(thirdX));
        assertNotNull(nameModel.getIdentifierForName(fourthX));

        assertEquals(nameModel.getIdentifierForName(thirdX), nameModel.getIdentifierForName(fourthX));

        assertTrue(!nameModel.getIdentifierForName(firstX).equals(nameModel.getIdentifierForName(thirdX)));

        // now test reverse mapping interface

        final List<SimpleName> nodesRelatedToFirstDeclaration = nameModel.nameNodesRelatedToNameNode(firstX);

        assertEquals(2, nodesRelatedToFirstDeclaration.size());
        assertTrue(nodesRelatedToFirstDeclaration.contains(firstX));
        assertTrue(nodesRelatedToFirstDeclaration.contains(secondX));

        final List<SimpleName> nodesRelatedToSecondDeclaration = nameModel.nameNodesRelatedToNameNode(thirdX);

        assertEquals(2, nodesRelatedToSecondDeclaration.size());
        assertTrue(nodesRelatedToSecondDeclaration.contains(thirdX));
        assertTrue(nodesRelatedToSecondDeclaration.contains(fourthX));

        // DU/UD test

        final DefUseModel udModel = getProject().getUDModel();

        // The use for the second declaration should be the fourthX

        final VariableDeclarationFragment secondXDeclaration = (VariableDeclarationFragment) thirdX.getParent();

        final Set<NodeReference> usesOfSecondDeclaration = udModel.usesForDefinition(NodeReferenceStore.getInstance()
                .getReference(secondXDeclaration));

        assertEquals(1, usesOfSecondDeclaration.size());

        // Replacement Declaration Node test
        // Make sure the node we report back is exactly equal to the one in the
        // AST

        final VariableDeclarationStatement expectedReplacementDeclaration = (VariableDeclarationStatement) secondXDeclaration
                .getParent();

        assertTrue(expectedReplacementDeclaration == step.getReplacementDeclarationStatement());
    }

}
