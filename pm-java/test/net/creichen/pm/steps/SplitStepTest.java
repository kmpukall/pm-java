/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm.steps;

import static net.creichen.pm.utils.ASTQuery.findAssignments;
import static net.creichen.pm.utils.ASTQuery.findClassByName;
import static net.creichen.pm.utils.ASTQuery.findMethodByName;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.junit.Test;

public class SplitStepTest extends PMTest {

    private ICompilationUnit iCompilationUnit;
    private ExpressionStatement assignmentStatement;

    public void setUpTest() {
        this.iCompilationUnit = createCompilationUnit("", "S.java",
                "public class S { void m() {int x; x = 7; x = 5; System.out.println(x);} }");
        TypeDeclaration type = findClassByName("S", getProject().getCompilationUnit(this.iCompilationUnit));
        MethodDeclaration method = findMethodByName("m", type);
        final Assignment secondAssignment = findAssignments(method).get(1);
        this.assignmentStatement = (ExpressionStatement) secondAssignment.getParent();
    }

    @Test
    public void itShouldModifyTheSourceCorrectly() throws JavaModelException {
        setUpTest();

        new SplitStep(getProject(), this.assignmentStatement).apply();

        assertTrue(matchesSource("public class S { void m() {int x; x = 7; int x = 5; System.out.println(x);} }",
                this.iCompilationUnit.getSource()));
    }

    // We have five outputs that we care about: the source, the updated name
    // model,
    // the DU/UD model, replacement declaration statement node, and the
    // inconsistencies

    @Test
    public void itShouldModifyTheNameModel() {
        setUpTest();

        final SplitStep step = new SplitStep(getProject(), this.assignmentStatement);
        step.apply();

        final NameModel nameModel = getProject().getNameModel();

        // First two occurrences of x should have same identifier
        // Second two occurrences of x should have same identifier (different
        // from first identifier)

        TypeDeclaration s = ASTQuery.findClassByName("S", getProject().getCompilationUnit(this.iCompilationUnit));
        MethodDeclaration m = ASTQuery.findMethodByName("m", s);
        final SimpleName firstX = ASTQuery.findSimpleNameByIdentifier("x", 0, "m", 0, "S", 0, getProject()
                .getCompilationUnit(this.iCompilationUnit));
        final SimpleName secondX = ASTQuery.findSimpleNameByIdentifier("x", 1, "m", 0, "S", 0, getProject()
                .getCompilationUnit(this.iCompilationUnit));
        final SimpleName thirdX = ASTQuery.findSimpleNameByIdentifier("x", 2, "m", 0, "S", 0, getProject()
                .getCompilationUnit(this.iCompilationUnit));
        final SimpleName fourthX = ASTQuery.findSimpleNameByIdentifier("x", 3, "m", 0, "S", 0, getProject()
                .getCompilationUnit(this.iCompilationUnit));

        assertNotNull(nameModel.getIdentifierForName(firstX));
        assertNotNull(nameModel.getIdentifierForName(secondX));
        assertNotNull(nameModel.getIdentifierForName(thirdX));
        assertNotNull(nameModel.getIdentifierForName(fourthX));

        assertEquals(nameModel.getIdentifierForName(firstX), nameModel.getIdentifierForName(secondX));
        assertEquals(nameModel.getIdentifierForName(thirdX), nameModel.getIdentifierForName(fourthX));
        assertFalse(nameModel.getIdentifierForName(firstX).equals(nameModel.getIdentifierForName(thirdX)));

        // now test reverse mapping interface

        final List<SimpleName> nodesRelatedToFirstDeclaration = nameModel.nameNodesRelatedToNameNode(firstX);

        assertEquals(2, nodesRelatedToFirstDeclaration.size());
        assertTrue(nodesRelatedToFirstDeclaration.contains(firstX));
        assertTrue(nodesRelatedToFirstDeclaration.contains(secondX));

        final List<SimpleName> nodesRelatedToSecondDeclaration = nameModel.nameNodesRelatedToNameNode(thirdX);

        assertEquals(2, nodesRelatedToSecondDeclaration.size());
        assertTrue(nodesRelatedToSecondDeclaration.contains(thirdX));
        assertTrue(nodesRelatedToSecondDeclaration.contains(fourthX));
    }

    @Test
    public void defUseTest() {
        setUpTest();

        final SplitStep step = new SplitStep(getProject(), this.assignmentStatement);
        step.apply();

        // DU/UD test

        final DefUseModel udModel = getProject().getUDModel();

        // The use for the second declaration should be the fourthX
        final SimpleName thirdX = ASTQuery.findSimpleNameByIdentifier("x", 2, "m", 0, "S", 0, getProject()
                .getCompilationUnit(this.iCompilationUnit));
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
