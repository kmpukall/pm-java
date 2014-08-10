/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm.analysis;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import net.creichen.pm.models.Def;
import net.creichen.pm.models.Use;
import net.creichen.pm.tests.PMTest;
import net.creichen.pm.utils.ASTQuery;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.junit.Ignore;
import org.junit.Test;

public class ReachingDefsAnalysisTest extends PMTest {
    private ReachingDefsAnalysis rdefs;
    private CompilationUnit compilationUnit;

    private Set<ASTNode> definingNodesFromDefinitions(final Set<Def> definitions) {
        final Set<ASTNode> definingNodes = new HashSet<ASTNode>();

        for (final Def definition : definitions) {
            definingNodes.add(definition.getDefiningNode());
        }

        return definingNodes;
    }

    @Test
    public void testArrrayAccessDefinition() {
        analyze("public class S {int[] y;void m(){y[5] = 6;}}");

        this.rdefs.toString();
        // Just want to make sure it doesn't blow up
    }

    @Test
    public void testDefiningInitializer1() {
        analyze("public class S {void m(){int x = 0;x = x + 1;}}");

        // The first x is not a use
        final SimpleName firstX = ASTQuery.findSimpleNames("x", this.compilationUnit).get(0);
        assertThat(this.rdefs.getUse(firstX), is(nullValue()));

    }

    @Test
    public void testDefiningInitializer2() {
        analyze("public class S {void m(){int x = 0;x = x + 1;}}");

        // The second x is not a use either
        final SimpleName secondX = ASTQuery.findSimpleNames("x", this.compilationUnit).get(1);
        assertThat(this.rdefs.getUse(secondX), is(nullValue()));
    }

    @Test
    public void testDefiningInitializer3() {
        analyze("public class S {void m(){int x = 0;x = x + 1;}}");

        // the third occurrence of x is a use and has one reaching definition
        final SimpleName thirdX = ASTQuery.findSimpleNames("x", this.compilationUnit).get(2);

        final Use use = this.rdefs.getUse(thirdX);
        assertThat(use, is(not(nullValue())));
        assertThat(use.getReachingDefinitions().size(), is(1));

        final Def definition = use.getReachingDefinitions().iterator().next();
        final VariableDeclaration declaration = ASTQuery.findLocalByName("x", 0, "m", 0, "S", 0, this.compilationUnit);
        assertEquals(declaration, definition.getDefiningNode());
    }

    @Test
    public void testDefinitionInPostIncrement() {
        analyze("public class S {void m(){int y = 1; y++; int x = y;}}");

        final SimpleName thirdY = ASTQuery.findSimpleNames("y", this.compilationUnit).get(2);

        final Use use = this.rdefs.getUse(thirdY);
        assertThat(use, is(not(nullValue())));
        assertEquals(1, use.getReachingDefinitions().size());

        final Def def = use.getReachingDefinitions().iterator().next();
        final PostfixExpression increment = (PostfixExpression) ASTQuery.findSimpleNames("y", this.compilationUnit)
                .get(1).getParent();
        assertEquals(increment, def.getDefiningNode());
    }

    @Test
    public void testDefinitionsInIfThenBody() {
        analyze("public class S {void m(){int y = 1; if (true) y = 5; System.out.println(y);}}");

        final SimpleName thirdY = ASTQuery.findSimpleNames("y", this.compilationUnit).get(2);
        final Use use = this.rdefs.getUse(thirdY);

        assertTrue(use != null);
        assertEquals(2, use.getReachingDefinitions().size());

        Iterator<Def> iterator = use.getReachingDefinitions().iterator();

        final Def definition1 = iterator.next();
        final Def definition2 = iterator.next();

        final VariableDeclaration yEquals1 = ASTQuery.findLocalByName("y", 0, "m", 0, "S", 0, this.compilationUnit);
        final Assignment yEquals5 = ASTQuery.assignmentInMethodInClassInCompilationUnit(0, "m", 0, "S", 0,
                this.compilationUnit);

        assertTrue(definition1.getDefiningNode() == yEquals1 && definition2.getDefiningNode() == yEquals5
                || definition1.getDefiningNode() == yEquals5 && definition2.getDefiningNode() == yEquals1);

    }

    @Test
    public void testDefinitionsInIfThenElseBody() {
        analyze("public class S {void m(){int y = 1; if (true) y = 5; else y = 6; System.out.println(y);}}");

        final SimpleName fourthY = ASTQuery.findSimpleNameByIdentifier("y", 3, "m", 0, "S", 0, this.compilationUnit);
        final Use fourthYUse = this.rdefs.getUse(fourthY);

        assertTrue(fourthYUse != null);

        assertEquals(2, fourthYUse.getReachingDefinitions().size());

        final Object[] definitions = fourthYUse.getReachingDefinitions().toArray();

        final Def definition1 = (Def) definitions[0];
        final Def definition2 = (Def) definitions[1];

        final Assignment yEquals5 = ASTQuery.assignmentInMethodInClassInCompilationUnit(0, "m", 0, "S", 0,
                this.compilationUnit);
        final Assignment yEquals6 = ASTQuery.assignmentInMethodInClassInCompilationUnit(1, "m", 0, "S", 0,
                this.compilationUnit);

        assertTrue(definition1.getDefiningNode() == yEquals5 && definition2.getDefiningNode() == yEquals6
                || definition1.getDefiningNode() == yEquals6 && definition2.getDefiningNode() == yEquals5);

    }

    @Test
    public void testDefinitionsInNestedIfThenElse() {
        analyze("public class S {void m(){int y = 1; if (true) { if (false) y = 2; else y = 3;} else {if (true) y = 4; else y = 5;} System.out.println(y);}}");

        final SimpleName sixthY = ASTQuery.findSimpleNameByIdentifier("y", 5, "m", 0, "S", 0, this.compilationUnit);
        final Use sixthYUse = this.rdefs.getUse(sixthY);

        assertTrue(sixthYUse != null);

        assertEquals(4, sixthYUse.getReachingDefinitions().size());

        final Set<ASTNode> definingNodes = definingNodesFromDefinitions(sixthYUse.getReachingDefinitions());

        final Assignment yEquals2 = ASTQuery.assignmentInMethodInClassInCompilationUnit(0, "m", 0, "S", 0,
                this.compilationUnit);
        final Assignment yEquals3 = ASTQuery.assignmentInMethodInClassInCompilationUnit(1, "m", 0, "S", 0,
                this.compilationUnit);
        final Assignment yEquals4 = ASTQuery.assignmentInMethodInClassInCompilationUnit(2, "m", 0, "S", 0,
                this.compilationUnit);
        final Assignment yEquals5 = ASTQuery.assignmentInMethodInClassInCompilationUnit(3, "m", 0, "S", 0,
                this.compilationUnit);

        assertTrue(definingNodes.contains(yEquals2));
        assertTrue(definingNodes.contains(yEquals3));
        assertTrue(definingNodes.contains(yEquals4));
        assertTrue(definingNodes.contains(yEquals5));
    }

    @Test
    public void testDefinitionsInWhileLoop() {
        analyze("public class S {void m(){int y = 1; while(y < 6) {y = y + 1;} System.out.println(y);}}");

        final SimpleName fourthY = ASTQuery.findSimpleNameByIdentifier("y", 3, "m", 0, "S", 0, this.compilationUnit);
        final Use fourthYUse = this.rdefs.getUse(fourthY);

        assertTrue(fourthYUse != null);

        assertEquals(2, fourthYUse.getReachingDefinitions().size());

        final Set<ASTNode> fourthYUseDefiningNodes = definingNodesFromDefinitions(fourthYUse.getReachingDefinitions());

        final VariableDeclaration yEquals1 = ASTQuery.findLocalByName("y", 0, "m", 0, "S", 0, this.compilationUnit);

        final Assignment yEqualsYPlus1 = ASTQuery.assignmentInMethodInClassInCompilationUnit(0, "m", 0, "S", 0,
                this.compilationUnit);

        assertTrue(fourthYUseDefiningNodes.contains(yEquals1));
        assertTrue(fourthYUseDefiningNodes.contains(yEqualsYPlus1));

        final SimpleName fifthY = ASTQuery.findSimpleNameByIdentifier("y", 4, "m", 0, "S", 0, this.compilationUnit);
        final Use fifthYUse = this.rdefs.getUse(fifthY);

        assertTrue(fifthYUse != null);
        assertEquals(2, fifthYUse.getReachingDefinitions().size());

        final Set<ASTNode> fifthYUseDefiningNodes = definingNodesFromDefinitions(fourthYUse.getReachingDefinitions());
        assertTrue(fifthYUseDefiningNodes.contains(yEquals1));
        assertTrue(fifthYUseDefiningNodes.contains(yEqualsYPlus1));

    }

    @Test
    public void testFieldAccessDefinition() {
        analyze("public class S {int y;void m(S x){x.y = 1; }}");

        this.rdefs.toString();

    }

    @Test
    @Ignore
    // TODO: fix this test
    public void testFindDefinitions() {
        analyze("class S {void m(int x){int y = x - 1; x++; --x; y += (x = y);}}");

        final List<Def> definitions = this.rdefs.getDefinitions();

        assertEquals(6, definitions.size());
    }

    @Test
    public void testMethodParameterWithoutUse() {
        analyze("public class S {void m(String object){ }}");

        this.rdefs.toString();

        // we really just want to make sure this doesn't blow up.
    }

    @Test
    public void testMethodParameterWithUse() {
        analyze("public class S {void m(String x){System.out.println(x); }}");

        final SimpleName firstX = ASTQuery.findSimpleNameByIdentifier("x", 0, "m", 0, "S", 0, this.compilationUnit);
        assertTrue(firstX != null);

        final Use firstXUse = this.rdefs.getUse(firstX);

        assertEquals(1, firstXUse.getReachingDefinitions().size());

        assertEquals(null, firstXUse.getReachingDefinitions().toArray()[0]); // null
        // means
        // uninitialized
    }

    @Test
    public void testNonDefiningPrefixExpression() {
        analyze("public class S {bool y;void m(){if (!y);}}");

        this.rdefs.toString();
        // Just want to make sure it doesn't blow up
    }

    @Test
    public void testStraightlineCodeReachingDefsNoUses() {
        analyze("public class S {void m(){int x;x = 1;int y; x = 2; y = 3; x = 5;}}");

        assertEquals(0, this.rdefs.getUses().size());

        // TODO: insert tests here!!!

        // need to figure out exactly what it means to be a definition
    }

    @Test
    public void testStraightlineCodeUses() {
        analyze("public class S {void m(){int x;x = 1;int y; y = x;}}");

        final SimpleName firstX = ASTQuery.findSimpleNameByIdentifier("x", 0, "m", 0, "S", 0, this.compilationUnit);

        assertEquals(null, this.rdefs.getUse(firstX));

        final SimpleName secondX = ASTQuery.findSimpleNameByIdentifier("x", 1, "m", 0, "S", 0, this.compilationUnit);

        assertEquals(null, this.rdefs.getUse(secondX));

        final SimpleName thirdX = ASTQuery.findSimpleNameByIdentifier("x", 2, "m", 0, "S", 0, this.compilationUnit);

        final Use thirdXUse = this.rdefs.getUse(thirdX);

        assertTrue(thirdXUse != null);

        assertEquals(1, thirdXUse.getReachingDefinitions().size());

        final Assignment xAssignment = ASTQuery.assignmentInMethodInClassInCompilationUnit(0, "m", 0, "S", 0,
                this.compilationUnit);

        final Def xAssignmentDef = (Def) thirdXUse.getReachingDefinitions().toArray()[0];

        assertTrue(xAssignmentDef != null);
        assertEquals(xAssignment, xAssignmentDef.getDefiningNode());
    }

    @Test
    public void testUseInExpressionStatement() {

        // Here we test the use of x in method invocation in an expression
        // statement.

        analyze("public class S {void m(){String x;x = \"foo\";System.out.println(x);}}");

        final SimpleName thirdX = ASTQuery.findSimpleNameByIdentifier("x", 2, "m", 0, "S", 0, this.compilationUnit);

        final Use thirdXUse = this.rdefs.getUse(thirdX);

        assertTrue(thirdXUse != null);

        assertEquals(1, thirdXUse.getReachingDefinitions().size());

        final Assignment xAssignment = ASTQuery.assignmentInMethodInClassInCompilationUnit(0, "m", 0, "S", 0,
                this.compilationUnit);

        final Def xAssignmentDef = (Def) thirdXUse.getReachingDefinitions().toArray()[0];

        assertEquals(xAssignment, xAssignmentDef.getDefiningNode());
    }

    @Test
    public void testUseInInitializer() {
        analyze("public class S {void m(){int y = 1;int x = y;}}");

        final SimpleName firstY = ASTQuery.findSimpleNameByIdentifier("y", 0, "m", 0, "S", 0, this.compilationUnit);
        assertEquals(null, this.rdefs.getUse(firstY)); // The first y is
        // not a use

        final SimpleName firstX = ASTQuery.findSimpleNameByIdentifier("x", 0, "m", 0, "S", 0, this.compilationUnit);
        assertEquals(null, this.rdefs.getUse(firstX)); // The first x is
        // not a use

        final SimpleName secondY = ASTQuery.findSimpleNameByIdentifier("y", 1, "m", 0, "S", 0, this.compilationUnit);
        final Use secondYUse = this.rdefs.getUse(secondY);

        assertTrue(secondYUse != null);

        assertEquals(1, secondYUse.getReachingDefinitions().size());

        final Def defForSecondYUse = (Def) secondYUse.getReachingDefinitions().toArray()[0];

        final VariableDeclaration yDeclaration = ASTQuery.findLocalByName("y", 0, "m", 0, "S", 0, this.compilationUnit);

        assertEquals(yDeclaration, defForSecondYUse.getDefiningNode());
    }

    @Test
    public void testUseInPostIncrement() {
        analyze("public class S {void m(){int y = 1;int x = y++;}}");

        final SimpleName secondY = ASTQuery.findSimpleNameByIdentifier("y", 1, "m", 0, "S", 0, this.compilationUnit);
        final Use secondYUse = this.rdefs.getUse(secondY);

        assertTrue(secondYUse != null);

        assertEquals(1, secondYUse.getReachingDefinitions().size());

        final Def defForSecondYUse = (Def) secondYUse.getReachingDefinitions().toArray()[0];

        final VariableDeclaration yDeclaration = ASTQuery.findLocalByName("y", 0, "m", 0, "S", 0, this.compilationUnit);

        assertEquals(yDeclaration, defForSecondYUse.getDefiningNode());
    }

    @Test
    public void testUseOfInstanceVariableBeforeDefinition() {
        analyze("public class S {int y; void m(){System.out.println(y); y = 5;}}");

        final SimpleName secondY = ASTQuery.findSimpleNameByIdentifier("y", 0, "m", 0, "S", 0, this.compilationUnit);
        assertTrue(secondY != null);

        final Use secondYUse = this.rdefs.getUse(secondY);

        assertEquals(1, secondYUse.getReachingDefinitions().size());

        assertEquals(null, secondYUse.getReachingDefinitions().toArray()[0]);
    }

    @Test
    public void testUseOfInstanceVariableWithoutDefinition() {
        analyze("public class S {int y; void m(){System.out.println(y);}}");

        final SimpleName secondY = ASTQuery.findSimpleNameByIdentifier("y", 0, "m", 0, "S", 0, this.compilationUnit);
        assertTrue(secondY != null);

        final Use secondYUse = this.rdefs.getUse(secondY);

        assertEquals(1, secondYUse.getReachingDefinitions().size());

        assertEquals(null, secondYUse.getReachingDefinitions().toArray()[0]);
    }

    private void analyze(final String source) {
        this.compilationUnit = parseCompilationUnitFromSource(source, "S.java");
        final MethodDeclaration methodDeclaration = ASTQuery.findMethodByName("m", 0, "S", 0, this.compilationUnit);
        this.rdefs = new ReachingDefsAnalysis(methodDeclaration);
    }

}
