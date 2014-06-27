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

import net.creichen.pm.PMASTQuery;
import net.creichen.pm.PMNodeReference;
import net.creichen.pm.PMProject;
import net.creichen.pm.PMWorkspace;
import net.creichen.pm.models.PMNameModel;
import net.creichen.pm.models.PMUDModel;
import net.creichen.pm.tests.PMTest;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.*;
import org.junit.Test;

public class PMSplitStepTest extends PMTest {

    @Test
    public void testSimplestCase() throws JavaModelException {
        final ICompilationUnit iCompilationUnit = createNewCompilationUnit("", "S.java",
                "public class S { void m() {int x; x = 7; x = 5; System.out.println(x);} }");

        final PMProject project = PMWorkspace.sharedWorkspace().projectForIJavaProject(
                this.getIJavaProject());

        final Assignment secondAssignment = PMASTQuery.assignmentInMethodInClassInCompilationUnit(
                1, "m", 0, "S", 0,
                (CompilationUnit) project.findASTRootForICompilationUnit(iCompilationUnit));

        final ExpressionStatement assignmentStatement = (ExpressionStatement) secondAssignment
                .getParent();

        final PMSplitStep step = new PMSplitStep(project, assignmentStatement);

        step.applyAllAtOnce();

        // We have five outputs that we care about: the source, the updated name
        // model,
        // the DU/UD model, replacement declaration statement node, and the
        // inconsistencies

        // Source test
        assertTrue(compilationUnitSourceMatchesSource(
                "public class S { void m() {int x; x = 7; int x = 5; System.out.println(x);} }",
                iCompilationUnit.getSource()));

        // Name model test

        final PMNameModel nameModel = project.getNameModel();

        // First two occurrences of x should have same identifier
        // Second two occurrences of x should have same identifier (different
        // from first identifier)

        final SimpleName firstX = PMASTQuery
                .simpleNameWithIdentifierInMethodInClassInCompilationUnit("x", 0, "m", 0, "S", 0,
                        (CompilationUnit) project.findASTRootForICompilationUnit(iCompilationUnit));
        final SimpleName secondX = PMASTQuery
                .simpleNameWithIdentifierInMethodInClassInCompilationUnit("x", 1, "m", 0, "S", 0,
                        (CompilationUnit) project.findASTRootForICompilationUnit(iCompilationUnit));

        assertNotNull(nameModel.identifierForName(firstX));
        assertNotNull(nameModel.identifierForName(secondX));

        assertEquals(nameModel.identifierForName(firstX), nameModel.identifierForName(secondX));

        final SimpleName thirdX = PMASTQuery
                .simpleNameWithIdentifierInMethodInClassInCompilationUnit("x", 2, "m", 0, "S", 0,
                        (CompilationUnit) project.findASTRootForICompilationUnit(iCompilationUnit));
        final SimpleName fourthX = PMASTQuery
                .simpleNameWithIdentifierInMethodInClassInCompilationUnit("x", 3, "m", 0, "S", 0,
                        (CompilationUnit) project.findASTRootForICompilationUnit(iCompilationUnit));

        assertNotNull(nameModel.identifierForName(thirdX));
        assertNotNull(nameModel.identifierForName(fourthX));

        assertEquals(nameModel.identifierForName(thirdX), nameModel.identifierForName(fourthX));

        assertTrue(!nameModel.identifierForName(firstX).equals(nameModel.identifierForName(thirdX)));

        // now test reverse mapping interface

        final List<SimpleName> nodesRelatedToFirstDeclaration = nameModel
                .nameNodesRelatedToNameNode(firstX);

        assertEquals(2, nodesRelatedToFirstDeclaration.size());
        assertTrue(nodesRelatedToFirstDeclaration.contains(firstX));
        assertTrue(nodesRelatedToFirstDeclaration.contains(secondX));

        final List<SimpleName> nodesRelatedToSecondDeclaration = nameModel
                .nameNodesRelatedToNameNode(thirdX);

        assertEquals(2, nodesRelatedToSecondDeclaration.size());
        assertTrue(nodesRelatedToSecondDeclaration.contains(thirdX));
        assertTrue(nodesRelatedToSecondDeclaration.contains(fourthX));

        // DU/UD test

        final PMUDModel udModel = project.getUDModel();

        // The use for the second declaration should be the fourthX

        final VariableDeclarationFragment secondXDeclaration = (VariableDeclarationFragment) thirdX
                .getParent();

        final Set<PMNodeReference> usesOfSecondDeclaration = udModel.usesForDefinition(project
                .getReferenceForNode(secondXDeclaration));

        assertEquals(1, usesOfSecondDeclaration.size());

        // Replacement Declaration Node test
        // Make sure the node we report back is exactly equal to the one in the
        // AST

        final VariableDeclarationStatement expectedReplacementDeclaration = (VariableDeclarationStatement) secondXDeclaration
                .getParent();

        assertTrue(expectedReplacementDeclaration == step.getReplacementDeclarationStatement());
    }

}
