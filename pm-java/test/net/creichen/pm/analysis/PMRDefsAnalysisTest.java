/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm.analysis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import net.creichen.pm.PMASTQuery;
import net.creichen.pm.tests.PMTest;

import org.eclipse.jdt.core.dom.*;
import org.junit.Test;

public class PMRDefsAnalysisTest extends PMTest {
    Set<ASTNode> definingNodesFromDefinitions(final Set<PMDef> definitions) {
        final Set<ASTNode> definingNodes = new HashSet<ASTNode>();

        for (final PMDef definition : definitions) {
            definingNodes.add(definition.getDefiningNode());
        }

        return definingNodes;
    }

    @Test
    public void testArrrayAccessDefinition() {
        final String source = "public class S {int[] y;void m(){y[5] = 6;}}";

        final CompilationUnit compilationUnit = parseCompilationUnitFromSource(source, "S.java");

        final MethodDeclaration methodDeclaration = PMASTQuery
                .methodWithNameInClassInCompilationUnit("m", 0, "S", 0, compilationUnit);

        final PMRDefsAnalysis rdefs = new PMRDefsAnalysis(methodDeclaration);

        rdefs.toString();
        // Just want to make sure it doesn't blow up
    }

    @Test
    public void testDefiningInitializer() {
        final String source = "public class S {void m(){int x = 0;x = x + 1;}}";

        final CompilationUnit compilationUnit = parseCompilationUnitFromSource(source, "S.java");

        final MethodDeclaration methodDeclaration = PMASTQuery
                .methodWithNameInClassInCompilationUnit("m", 0, "S", 0, compilationUnit);

        final PMRDefsAnalysis rdefs = new PMRDefsAnalysis(methodDeclaration);

        final SimpleName firstX = PMASTQuery
                .simpleNameWithIdentifierInMethodInClassInCompilationUnit("x", 0, "m", 0, "S", 0,
                        compilationUnit);

        assertEquals(null, rdefs.useForSimpleName(firstX)); // The first x is
                                                            // not a use

        final SimpleName secondX = PMASTQuery
                .simpleNameWithIdentifierInMethodInClassInCompilationUnit("x", 1, "m", 0, "S", 0,
                        compilationUnit);

        assertEquals(null, rdefs.useForSimpleName(secondX)); // The second x is
                                                             // not a use

        final SimpleName thirdX = PMASTQuery
                .simpleNameWithIdentifierInMethodInClassInCompilationUnit("x", 2, "m", 0, "S", 0,
                        compilationUnit);

        final PMUse thirdXUse = rdefs.useForSimpleName(thirdX);

        assertTrue(thirdXUse != null);

        assertEquals(1, thirdXUse.getReachingDefinitions().size());

        final PMDef xInitializerDef = (PMDef) thirdXUse.getReachingDefinitions().toArray()[0];

        final VariableDeclaration xDeclaration = PMASTQuery
                .localWithNameInMethodInClassInCompilationUnit("x", 0, "m", 0, "S", 0,
                        compilationUnit);

        assertEquals(xDeclaration, xInitializerDef.getDefiningNode());
    }

    @Test
    public void testDefinitionInPostIncrement() {
        final String source = "public class S {void m(){int y = 1; y++; int x = y;}}";

        final CompilationUnit compilationUnit = parseCompilationUnitFromSource(source, "S.java");

        final MethodDeclaration methodDeclaration = PMASTQuery
                .methodWithNameInClassInCompilationUnit("m", 0, "S", 0, compilationUnit);

        final PMRDefsAnalysis rdefs = new PMRDefsAnalysis(methodDeclaration);

        final SimpleName thirdY = PMASTQuery
                .simpleNameWithIdentifierInMethodInClassInCompilationUnit("y", 2, "m", 0, "S", 0,
                        compilationUnit);
        final PMUse thirdYUse = rdefs.useForSimpleName(thirdY);

        assertTrue(thirdYUse != null);

        assertEquals(1, thirdYUse.getReachingDefinitions().size());

        final PMDef defForThirdYUse = (PMDef) thirdYUse.getReachingDefinitions().toArray()[0];

        final PostfixExpression yPlusPlus = (PostfixExpression) PMASTQuery
                .simpleNameWithIdentifierInMethodInClassInCompilationUnit("y", 1, "m", 0, "S", 0,
                        compilationUnit).getParent();

        assertEquals(yPlusPlus, defForThirdYUse.getDefiningNode());
    }

    @Test
    public void testDefinitionsInIfThenBody() {
        final String source = "public class S {void m(){int y = 1; if (true) y = 5; System.out.println(y);}}";

        final CompilationUnit compilationUnit = parseCompilationUnitFromSource(source, "S.java");

        final MethodDeclaration methodDeclaration = PMASTQuery
                .methodWithNameInClassInCompilationUnit("m", 0, "S", 0, compilationUnit);

        final PMRDefsAnalysis rdefs = new PMRDefsAnalysis(methodDeclaration);

        final SimpleName thirdY = PMASTQuery
                .simpleNameWithIdentifierInMethodInClassInCompilationUnit("y", 2, "m", 0, "S", 0,
                        compilationUnit);
        final PMUse thirdYUse = rdefs.useForSimpleName(thirdY);

        assertTrue(thirdYUse != null);

        assertEquals(2, thirdYUse.getReachingDefinitions().size());

        final Object definitions[] = thirdYUse.getReachingDefinitions().toArray();

        final PMDef definition1 = (PMDef) definitions[0];
        final PMDef definition2 = (PMDef) definitions[1];

        final VariableDeclaration yEquals1 = PMASTQuery
                .localWithNameInMethodInClassInCompilationUnit("y", 0, "m", 0, "S", 0,
                        compilationUnit);
        final Assignment yEquals5 = PMASTQuery.assignmentInMethodInClassInCompilationUnit(0, "m",
                0, "S", 0, compilationUnit);

        assertTrue(definition1.getDefiningNode() == yEquals1
                && definition2.getDefiningNode() == yEquals5
                || definition1.getDefiningNode() == yEquals5
                && definition2.getDefiningNode() == yEquals1);

    }

    @Test
    public void testDefinitionsInIfThenElseBody() {
        final String source = "public class S {void m(){int y = 1; if (true) y = 5; else y = 6; System.out.println(y);}}";

        final CompilationUnit compilationUnit = parseCompilationUnitFromSource(source, "S.java");

        final MethodDeclaration methodDeclaration = PMASTQuery
                .methodWithNameInClassInCompilationUnit("m", 0, "S", 0, compilationUnit);

        final PMRDefsAnalysis rdefs = new PMRDefsAnalysis(methodDeclaration);

        final SimpleName fourthY = PMASTQuery
                .simpleNameWithIdentifierInMethodInClassInCompilationUnit("y", 3, "m", 0, "S", 0,
                        compilationUnit);
        final PMUse fourthYUse = rdefs.useForSimpleName(fourthY);

        assertTrue(fourthYUse != null);

        assertEquals(2, fourthYUse.getReachingDefinitions().size());

        final Object definitions[] = fourthYUse.getReachingDefinitions().toArray();

        final PMDef definition1 = (PMDef) definitions[0];
        final PMDef definition2 = (PMDef) definitions[1];

        final Assignment yEquals5 = PMASTQuery.assignmentInMethodInClassInCompilationUnit(0, "m",
                0, "S", 0, compilationUnit);
        final Assignment yEquals6 = PMASTQuery.assignmentInMethodInClassInCompilationUnit(1, "m",
                0, "S", 0, compilationUnit);

        assertTrue(definition1.getDefiningNode() == yEquals5
                && definition2.getDefiningNode() == yEquals6
                || definition1.getDefiningNode() == yEquals6
                && definition2.getDefiningNode() == yEquals5);

    }

    @Test
    public void testDefinitionsInNestedIfThenElse() {
        final String source = "public class S {void m(){int y = 1; if (true) { if (false) y = 2; else y = 3;} else {if (true) y = 4; else y = 5;} System.out.println(y);}}";

        final CompilationUnit compilationUnit = parseCompilationUnitFromSource(source, "S.java");

        final MethodDeclaration methodDeclaration = PMASTQuery
                .methodWithNameInClassInCompilationUnit("m", 0, "S", 0, compilationUnit);

        final PMRDefsAnalysis rdefs = new PMRDefsAnalysis(methodDeclaration);

        final SimpleName sixthY = PMASTQuery
                .simpleNameWithIdentifierInMethodInClassInCompilationUnit("y", 5, "m", 0, "S", 0,
                        compilationUnit);
        final PMUse sixthYUse = rdefs.useForSimpleName(sixthY);

        assertTrue(sixthYUse != null);

        assertEquals(4, sixthYUse.getReachingDefinitions().size());

        final Set<ASTNode> definingNodes = definingNodesFromDefinitions(sixthYUse
                .getReachingDefinitions());

        final Assignment yEquals2 = PMASTQuery.assignmentInMethodInClassInCompilationUnit(0, "m",
                0, "S", 0, compilationUnit);
        final Assignment yEquals3 = PMASTQuery.assignmentInMethodInClassInCompilationUnit(1, "m",
                0, "S", 0, compilationUnit);
        final Assignment yEquals4 = PMASTQuery.assignmentInMethodInClassInCompilationUnit(2, "m",
                0, "S", 0, compilationUnit);
        final Assignment yEquals5 = PMASTQuery.assignmentInMethodInClassInCompilationUnit(3, "m",
                0, "S", 0, compilationUnit);

        assertTrue(definingNodes.contains(yEquals2));
        assertTrue(definingNodes.contains(yEquals3));
        assertTrue(definingNodes.contains(yEquals4));
        assertTrue(definingNodes.contains(yEquals5));
    }

    @Test
    public void testDefinitionsInWhileLoop() {
        final String source = "public class S {void m(){int y = 1; while(y < 6) {y = y + 1;} System.out.println(y);}}";

        final CompilationUnit compilationUnit = parseCompilationUnitFromSource(source, "S.java");

        final MethodDeclaration methodDeclaration = PMASTQuery
                .methodWithNameInClassInCompilationUnit("m", 0, "S", 0, compilationUnit);

        final PMRDefsAnalysis rdefs = new PMRDefsAnalysis(methodDeclaration);

        final SimpleName fourthY = PMASTQuery
                .simpleNameWithIdentifierInMethodInClassInCompilationUnit("y", 3, "m", 0, "S", 0,
                        compilationUnit);
        final PMUse fourthYUse = rdefs.useForSimpleName(fourthY);

        assertTrue(fourthYUse != null);

        assertEquals(2, fourthYUse.getReachingDefinitions().size());

        final Set<ASTNode> fourthYUseDefiningNodes = definingNodesFromDefinitions(fourthYUse
                .getReachingDefinitions());

        final VariableDeclaration yEquals1 = PMASTQuery
                .localWithNameInMethodInClassInCompilationUnit("y", 0, "m", 0, "S", 0,
                        compilationUnit);

        final Assignment yEqualsYPlus1 = PMASTQuery.assignmentInMethodInClassInCompilationUnit(0,
                "m", 0, "S", 0, compilationUnit);

        assertTrue(fourthYUseDefiningNodes.contains(yEquals1));
        assertTrue(fourthYUseDefiningNodes.contains(yEqualsYPlus1));

        final SimpleName fifthY = PMASTQuery
                .simpleNameWithIdentifierInMethodInClassInCompilationUnit("y", 4, "m", 0, "S", 0,
                        compilationUnit);
        final PMUse fifthYUse = rdefs.useForSimpleName(fifthY);

        assertTrue(fifthYUse != null);
        assertEquals(2, fifthYUse.getReachingDefinitions().size());

        final Set<ASTNode> fifthYUseDefiningNodes = definingNodesFromDefinitions(fourthYUse
                .getReachingDefinitions());
        assertTrue(fifthYUseDefiningNodes.contains(yEquals1));
        assertTrue(fifthYUseDefiningNodes.contains(yEqualsYPlus1));

    }

    @Test
    public void testFieldAccessDefinition() {
        final String source = "public class S {int y;void m(S x){x.y = 1; }}";

        final CompilationUnit compilationUnit = parseCompilationUnitFromSource(source, "S.java");

        final MethodDeclaration methodDeclaration = PMASTQuery
                .methodWithNameInClassInCompilationUnit("m", 0, "S", 0, compilationUnit);

        final PMRDefsAnalysis rdefs = new PMRDefsAnalysis(methodDeclaration);
        rdefs.toString();

    }

    /* @Test */public void testFindDefinitions() {
        final String source = "class S {void m(int x){int y = x - 1; x++; --x; y += (x = y);}}";

        final CompilationUnit compilationUnit = parseCompilationUnitFromSource(source);

        final MethodDeclaration methodDeclaration = PMASTQuery
                .methodWithNameInClassInCompilationUnit("m", 0, "S", 0, compilationUnit);

        final PMRDefsAnalysis rdefs = new PMRDefsAnalysis(methodDeclaration);

        final ArrayList<PMDef> definitions = rdefs.getDefinitions();

        assertEquals(6, definitions.size());
    }

    @Test
    public void testMethodParameterWithoutUse() {
        final String source = "public class S {void m(String object){ }}";

        final CompilationUnit compilationUnit = parseCompilationUnitFromSource(source, "S.java");

        final MethodDeclaration methodDeclaration = PMASTQuery
                .methodWithNameInClassInCompilationUnit("m", 0, "S", 0, compilationUnit);

        final PMRDefsAnalysis rdefs = new PMRDefsAnalysis(methodDeclaration);

        rdefs.toString();

        // we really just want to make sure this doesn't blow up.
    }

    @Test
    public void testMethodParameterWithUse() {
        final String source = "public class S {void m(String x){System.out.println(x); }}";

        final CompilationUnit compilationUnit = parseCompilationUnitFromSource(source, "S.java");

        final MethodDeclaration methodDeclaration = PMASTQuery
                .methodWithNameInClassInCompilationUnit("m", 0, "S", 0, compilationUnit);

        final PMRDefsAnalysis rdefs = new PMRDefsAnalysis(methodDeclaration);

        final SimpleName firstX = PMASTQuery
                .simpleNameWithIdentifierInMethodInClassInCompilationUnit("x", 0, "m", 0, "S", 0,
                        compilationUnit);
        assertTrue(firstX != null);

        final PMUse firstXUse = rdefs.useForSimpleName(firstX);

        assertEquals(1, firstXUse.getReachingDefinitions().size());

        assertEquals(null, firstXUse.getReachingDefinitions().toArray()[0]); // null
                                                                             // means
                                                                             // uninitialized
    }

    @Test
    public void testNonDefiningPrefixExpression() {
        final String source = "public class S {bool y;void m(){if (!y);}}";

        final CompilationUnit compilationUnit = parseCompilationUnitFromSource(source, "S.java");

        final MethodDeclaration methodDeclaration = PMASTQuery
                .methodWithNameInClassInCompilationUnit("m", 0, "S", 0, compilationUnit);

        final PMRDefsAnalysis rdefs = new PMRDefsAnalysis(methodDeclaration);

        rdefs.toString();
        // Just want to make sure it doesn't blow up
    }

    @Test
    public void testStraightlineCodeReachingDefsNoUses() {
        final String source = "public class S {void m(){int x;x = 1;int y; x = 2; y = 3; x = 5;}}";

        final CompilationUnit compilationUnit = parseCompilationUnitFromSource(source, "S.java");

        final MethodDeclaration methodDeclaration = PMASTQuery
                .methodWithNameInClassInCompilationUnit("m", 0, "S", 0, compilationUnit);

        final PMRDefsAnalysis rdefs = new PMRDefsAnalysis(methodDeclaration);

        assertEquals(0, rdefs.getUses().size());

        // insert tests here!!!

        // need to figure out exactly what it means to be a definition
    }

    @Test
    public void testStraightlineCodeUses() {
        final String source = "public class S {void m(){int x;x = 1;int y; y = x;}}";

        final CompilationUnit compilationUnit = parseCompilationUnitFromSource(source, "S.java");

        final MethodDeclaration methodDeclaration = PMASTQuery
                .methodWithNameInClassInCompilationUnit("m", 0, "S", 0, compilationUnit);

        final PMRDefsAnalysis rdefs = new PMRDefsAnalysis(methodDeclaration);

        final SimpleName firstX = PMASTQuery
                .simpleNameWithIdentifierInMethodInClassInCompilationUnit("x", 0, "m", 0, "S", 0,
                        compilationUnit);

        assertEquals(null, rdefs.useForSimpleName(firstX));

        final SimpleName secondX = PMASTQuery
                .simpleNameWithIdentifierInMethodInClassInCompilationUnit("x", 1, "m", 0, "S", 0,
                        compilationUnit);

        assertEquals(null, rdefs.useForSimpleName(secondX));

        final SimpleName thirdX = PMASTQuery
                .simpleNameWithIdentifierInMethodInClassInCompilationUnit("x", 2, "m", 0, "S", 0,
                        compilationUnit);

        final PMUse thirdXUse = rdefs.useForSimpleName(thirdX);

        assertTrue(thirdXUse != null);

        assertEquals(1, thirdXUse.getReachingDefinitions().size());

        final Assignment xAssignment = PMASTQuery.assignmentInMethodInClassInCompilationUnit(0,
                "m", 0, "S", 0, compilationUnit);

        final PMDef xAssignmentDef = (PMDef) thirdXUse.getReachingDefinitions().toArray()[0];

        assertTrue(xAssignmentDef != null);
        assertEquals(xAssignment, xAssignmentDef.getDefiningNode());
    }

    @Test
    public void testUseInExpressionStatement() {

        // Here we test the use of x in method invocation in an expression
        // statement.

        final String source = "public class S {void m(){String x;x = \"foo\";System.out.println(x);}}";

        final CompilationUnit compilationUnit = parseCompilationUnitFromSource(source, "S.java");

        final MethodDeclaration methodDeclaration = PMASTQuery
                .methodWithNameInClassInCompilationUnit("m", 0, "S", 0, compilationUnit);

        final PMRDefsAnalysis rdefs = new PMRDefsAnalysis(methodDeclaration);

        final SimpleName thirdX = PMASTQuery
                .simpleNameWithIdentifierInMethodInClassInCompilationUnit("x", 2, "m", 0, "S", 0,
                        compilationUnit);

        final PMUse thirdXUse = rdefs.useForSimpleName(thirdX);

        assertTrue(thirdXUse != null);

        assertEquals(1, thirdXUse.getReachingDefinitions().size());

        final Assignment xAssignment = PMASTQuery.assignmentInMethodInClassInCompilationUnit(0,
                "m", 0, "S", 0, compilationUnit);

        final PMDef xAssignmentDef = (PMDef) thirdXUse.getReachingDefinitions().toArray()[0];

        assertEquals(xAssignment, xAssignmentDef.getDefiningNode());
    }

    @Test
    public void testUseInInitializer() {
        final String source = "public class S {void m(){int y = 1;int x = y;}}";

        final CompilationUnit compilationUnit = parseCompilationUnitFromSource(source, "S.java");

        final MethodDeclaration methodDeclaration = PMASTQuery
                .methodWithNameInClassInCompilationUnit("m", 0, "S", 0, compilationUnit);

        final PMRDefsAnalysis rdefs = new PMRDefsAnalysis(methodDeclaration);

        final SimpleName firstY = PMASTQuery
                .simpleNameWithIdentifierInMethodInClassInCompilationUnit("y", 0, "m", 0, "S", 0,
                        compilationUnit);
        assertEquals(null, rdefs.useForSimpleName(firstY)); // The first y is
                                                            // not a use

        final SimpleName firstX = PMASTQuery
                .simpleNameWithIdentifierInMethodInClassInCompilationUnit("x", 0, "m", 0, "S", 0,
                        compilationUnit);
        assertEquals(null, rdefs.useForSimpleName(firstX)); // The first x is
                                                            // not a use

        final SimpleName secondY = PMASTQuery
                .simpleNameWithIdentifierInMethodInClassInCompilationUnit("y", 1, "m", 0, "S", 0,
                        compilationUnit);
        final PMUse secondYUse = rdefs.useForSimpleName(secondY);

        assertTrue(secondYUse != null);

        assertEquals(1, secondYUse.getReachingDefinitions().size());

        final PMDef defForSecondYUse = (PMDef) secondYUse.getReachingDefinitions().toArray()[0];

        final VariableDeclaration yDeclaration = PMASTQuery
                .localWithNameInMethodInClassInCompilationUnit("y", 0, "m", 0, "S", 0,
                        compilationUnit);

        assertEquals(yDeclaration, defForSecondYUse.getDefiningNode());
    }

    @Test
    public void testUseInPostIncrement() {
        final String source = "public class S {void m(){int y = 1;int x = y++;}}";

        final CompilationUnit compilationUnit = parseCompilationUnitFromSource(source, "S.java");

        final MethodDeclaration methodDeclaration = PMASTQuery
                .methodWithNameInClassInCompilationUnit("m", 0, "S", 0, compilationUnit);

        final PMRDefsAnalysis rdefs = new PMRDefsAnalysis(methodDeclaration);

        final SimpleName secondY = PMASTQuery
                .simpleNameWithIdentifierInMethodInClassInCompilationUnit("y", 1, "m", 0, "S", 0,
                        compilationUnit);
        final PMUse secondYUse = rdefs.useForSimpleName(secondY);

        assertTrue(secondYUse != null);

        assertEquals(1, secondYUse.getReachingDefinitions().size());

        final PMDef defForSecondYUse = (PMDef) secondYUse.getReachingDefinitions().toArray()[0];

        final VariableDeclaration yDeclaration = PMASTQuery
                .localWithNameInMethodInClassInCompilationUnit("y", 0, "m", 0, "S", 0,
                        compilationUnit);

        assertEquals(yDeclaration, defForSecondYUse.getDefiningNode());
    }

    @Test
    public void testUseOfInstanceVariableBeforeDefinition() {
        final String source = "public class S {int y; void m(){System.out.println(y); y = 5;}}";

        final CompilationUnit compilationUnit = parseCompilationUnitFromSource(source, "S.java");

        final MethodDeclaration methodDeclaration = PMASTQuery
                .methodWithNameInClassInCompilationUnit("m", 0, "S", 0, compilationUnit);

        final PMRDefsAnalysis rdefs = new PMRDefsAnalysis(methodDeclaration);

        final SimpleName secondY = PMASTQuery
                .simpleNameWithIdentifierInMethodInClassInCompilationUnit("y", 0, "m", 0, "S", 0,
                        compilationUnit);
        assertTrue(secondY != null);

        final PMUse secondYUse = rdefs.useForSimpleName(secondY);

        assertEquals(1, secondYUse.getReachingDefinitions().size());

        assertEquals(null, secondYUse.getReachingDefinitions().toArray()[0]);
    }

    @Test
    public void testUseOfInstanceVariableWithoutDefinition() {
        final String source = "public class S {int y; void m(){System.out.println(y);}}";

        final CompilationUnit compilationUnit = parseCompilationUnitFromSource(source, "S.java");

        final MethodDeclaration methodDeclaration = PMASTQuery
                .methodWithNameInClassInCompilationUnit("m", 0, "S", 0, compilationUnit);

        final PMRDefsAnalysis rdefs = new PMRDefsAnalysis(methodDeclaration);

        final SimpleName secondY = PMASTQuery
                .simpleNameWithIdentifierInMethodInClassInCompilationUnit("y", 0, "m", 0, "S", 0,
                        compilationUnit);
        assertTrue(secondY != null);

        final PMUse secondYUse = rdefs.useForSimpleName(secondY);

        assertEquals(1, secondYUse.getReachingDefinitions().size());

        assertEquals(null, secondYUse.getReachingDefinitions().toArray()[0]);
    }

}
