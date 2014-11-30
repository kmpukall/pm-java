/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm.steps;

import static net.creichen.pm.tests.Matchers.hasSource;
import static net.creichen.pm.utils.ASTQuery.findAssignments;
import static net.creichen.pm.utils.ASTQuery.findClassByName;
import static net.creichen.pm.utils.ASTQuery.findMethodByName;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Set;

import net.creichen.pm.api.Node;
import net.creichen.pm.data.NodeStore;
import net.creichen.pm.models.defuse.DefUseModel;
import net.creichen.pm.models.name.NameModel;
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
    private SplitStep step;

    public void setUpTest() {
        this.iCompilationUnit = createCompilationUnit("", "S.java",
                "public class S { void m() {int x; x = 7; x = 5; System.out.println(x);} }");
        TypeDeclaration type = findClassByName("S", getProject().getPMCompilationUnit(this.iCompilationUnit));
        MethodDeclaration method = findMethodByName("m", type);
        final Assignment secondAssignment = findAssignments(method).get(1);
        ExpressionStatement assignmentStatement = (ExpressionStatement) secondAssignment.getParent();
        this.step = new SplitStep(getProject(), assignmentStatement);
    }

    @Test
    public void itShouldModifyTheSourceCorrectly() throws JavaModelException {
        setUpTest();

        this.step.apply();

        assertThat(this.iCompilationUnit,
                hasSource("public class S { void m() {int x; x = 7; int x = 5; System.out.println(x);} }"));
    }

    // We have five outputs that we care about: the source, the updated name
    // model,
    // the DU/UD model, replacement declaration statement node, and the
    // inconsistencies

    @Test
    public void itShouldModifyTheNameModel() {
        setUpTest();

        this.step.apply();

        final NameModel nameModel = getProject().getNameModel();

        // First two occurrences of x should have same identifier
        // Second two occurrences of x should have same identifier (different
        // from first identifier)

        TypeDeclaration s = ASTQuery.findClassByName("S", getProject().getPMCompilationUnit(this.iCompilationUnit));
        MethodDeclaration m = ASTQuery.findMethodByName("m", s);
        List<SimpleName> simpleNames = ASTQuery.findSimpleNames("x", m);
        final SimpleName firstX = simpleNames.get(0);
        final SimpleName secondX = simpleNames.get(1);
        final SimpleName thirdX = simpleNames.get(2);
        final SimpleName fourthX = simpleNames.get(3);

        assertNotNull(nameModel.getIdentifier(firstX));
        assertNotNull(nameModel.getIdentifier(secondX));
        assertNotNull(nameModel.getIdentifier(thirdX));
        assertNotNull(nameModel.getIdentifier(fourthX));

        assertEquals(nameModel.getIdentifier(firstX), nameModel.getIdentifier(secondX));
        assertEquals(nameModel.getIdentifier(thirdX), nameModel.getIdentifier(fourthX));
        assertFalse(nameModel.getIdentifier(firstX).equals(nameModel.getIdentifier(thirdX)));

        // now test reverse mapping interface

        final List<SimpleName> nodesRelatedToFirstDeclaration = nameModel.getRelatedNodes(firstX);

        assertEquals(2, nodesRelatedToFirstDeclaration.size());
        assertTrue(nodesRelatedToFirstDeclaration.contains(firstX));
        assertTrue(nodesRelatedToFirstDeclaration.contains(secondX));

        final List<SimpleName> nodesRelatedToSecondDeclaration = nameModel.getRelatedNodes(thirdX);

        assertEquals(2, nodesRelatedToSecondDeclaration.size());
        assertTrue(nodesRelatedToSecondDeclaration.contains(thirdX));
        assertTrue(nodesRelatedToSecondDeclaration.contains(fourthX));
    }

    @Test
    public void defUseTest() {
        setUpTest();

        this.step.apply();

        // DU/UD test

        final DefUseModel udModel = getProject().getUDModel();

        // The use for the second declaration should be the fourthX
        TypeDeclaration s = ASTQuery.findClassByName("S", getProject().getPMCompilationUnit(this.iCompilationUnit));
        MethodDeclaration m = ASTQuery.findMethodByName("m", s);
        List<SimpleName> simpleNames = ASTQuery.findSimpleNames("x", m);
        final SimpleName thirdX = simpleNames.get(2);
        final VariableDeclarationFragment secondXDeclaration = (VariableDeclarationFragment) thirdX.getParent();

        final Set<Node> usesOfSecondDeclaration = udModel.getUsesByDefinition(NodeStore.getInstance().getReference(
                secondXDeclaration));

        assertEquals(1, usesOfSecondDeclaration.size());

        // Replacement Declaration Node test
        // Make sure the node we report back is exactly equal to the one in the
        // AST

        final VariableDeclarationStatement expectedReplacementDeclaration = (VariableDeclarationStatement) secondXDeclaration
                .getParent();

        assertTrue(expectedReplacementDeclaration == this.step.getReplacementDeclarationStatement());
    }

}
