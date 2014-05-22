/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

*******************************************************************************/

package pm_refactoring.tests;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.junit.Test;

import pm_refactoring.PMASTQuery;
import pm_refactoring.analysis.PMDef;
import pm_refactoring.analysis.PMRDefsAnalysis;
import pm_refactoring.analysis.PMUse;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class PMRDefsAnalysisTest extends PMTest {
	/*@Test*/ public void testFindDefinitions() {
		String source = "class S {void m(int x){int y = x - 1; x++; --x; y += (x = y);}}";
		
		CompilationUnit compilationUnit = parseCompilationUnitFromSource(source);
		
		MethodDeclaration methodDeclaration = (MethodDeclaration) PMASTQuery.methodWithNameInClassInCompilationUnit("m", 0, "S", 0, compilationUnit);
	
		PMRDefsAnalysis rdefs = new PMRDefsAnalysis(methodDeclaration);
		
		ArrayList<PMDef> definitions = rdefs.getDefinitions();
		
		assertEquals(6, definitions.size());
	}
	
	@Test public void testStraightlineCodeReachingDefsNoUses() {
		String source = "public class S {void m(){int x;x = 1;int y; x = 2; y = 3; x = 5;}}";
		
		CompilationUnit compilationUnit = parseCompilationUnitFromSource(source, "S.java");
		
		MethodDeclaration methodDeclaration = (MethodDeclaration) PMASTQuery.methodWithNameInClassInCompilationUnit("m", 0, "S", 0, compilationUnit);
	
		PMRDefsAnalysis rdefs = new PMRDefsAnalysis(methodDeclaration);
		
		assertEquals(0, rdefs.getUses().size());
		
		//insert tests here!!!
		
		//need to figure out exactly what it means to be a definition
	}
	
	@Test public void testStraightlineCodeUses() {
		String source = "public class S {void m(){int x;x = 1;int y; y = x;}}";
		
		CompilationUnit compilationUnit = parseCompilationUnitFromSource(source, "S.java");
		
		MethodDeclaration methodDeclaration = (MethodDeclaration) PMASTQuery.methodWithNameInClassInCompilationUnit("m", 0, "S", 0, compilationUnit);
	
		PMRDefsAnalysis rdefs = new PMRDefsAnalysis(methodDeclaration);
		
		SimpleName firstX = PMASTQuery.simpleNameWithIdentifierInMethodInClassInCompilationUnit("x", 0, "m", 0, "S", 0, compilationUnit);
		
		assertEquals(null, rdefs.useForSimpleName(firstX));
		
		SimpleName secondX = PMASTQuery.simpleNameWithIdentifierInMethodInClassInCompilationUnit("x", 1, "m", 0, "S", 0, compilationUnit);
		
		assertEquals(null, rdefs.useForSimpleName(secondX));
		
		SimpleName thirdX = PMASTQuery.simpleNameWithIdentifierInMethodInClassInCompilationUnit("x", 2, "m", 0, "S", 0, compilationUnit);
		
		
		PMUse thirdXUse = rdefs.useForSimpleName(thirdX);
		
		assertTrue(thirdXUse != null);
		
		assertEquals(1, thirdXUse.getReachingDefinitions().size());
		
		Assignment xAssignment = PMASTQuery.assignmentInMethodInClassInCompilationUnit(0, "m", 0, "S", 0, compilationUnit);
		
		PMDef xAssignmentDef = (PMDef)thirdXUse.getReachingDefinitions().toArray()[0];
		
		assertTrue(xAssignmentDef != null);
		assertEquals(xAssignment, xAssignmentDef.getDefiningNode());		
	}
	
	
	@Test public void testUseInExpressionStatement() {
		
		//Here we test the use of x in method invocation in an expression statement.
		
		String source = "public class S {void m(){String x;x = \"foo\";System.out.println(x);}}";
		
		CompilationUnit compilationUnit = parseCompilationUnitFromSource(source, "S.java");
		
		MethodDeclaration methodDeclaration = (MethodDeclaration) PMASTQuery.methodWithNameInClassInCompilationUnit("m", 0, "S", 0, compilationUnit);
	
		PMRDefsAnalysis rdefs = new PMRDefsAnalysis(methodDeclaration);
		
		
		SimpleName thirdX = PMASTQuery.simpleNameWithIdentifierInMethodInClassInCompilationUnit("x", 2, "m", 0, "S", 0, compilationUnit);
		
		
		PMUse thirdXUse = rdefs.useForSimpleName(thirdX);
		
		assertTrue(thirdXUse != null);
		
		assertEquals(1, thirdXUse.getReachingDefinitions().size());
		
		Assignment xAssignment = PMASTQuery.assignmentInMethodInClassInCompilationUnit(0, "m", 0, "S", 0, compilationUnit);
		
		PMDef xAssignmentDef = (PMDef)thirdXUse.getReachingDefinitions().toArray()[0];
		
		assertEquals(xAssignment, xAssignmentDef.getDefiningNode());		
	}
	
	@Test public void testDefiningInitializer() {
		String source = "public class S {void m(){int x = 0;x = x + 1;}}";
		
		CompilationUnit compilationUnit = parseCompilationUnitFromSource(source, "S.java");
		
		MethodDeclaration methodDeclaration = (MethodDeclaration) PMASTQuery.methodWithNameInClassInCompilationUnit("m", 0, "S", 0, compilationUnit);
	
		PMRDefsAnalysis rdefs = new PMRDefsAnalysis(methodDeclaration);
		
		SimpleName firstX = PMASTQuery.simpleNameWithIdentifierInMethodInClassInCompilationUnit("x", 0, "m", 0, "S", 0, compilationUnit);

		assertEquals(null, rdefs.useForSimpleName(firstX)); //The first x is not a use
		
		SimpleName secondX = PMASTQuery.simpleNameWithIdentifierInMethodInClassInCompilationUnit("x", 1, "m", 0, "S", 0, compilationUnit);
		
		assertEquals(null, rdefs.useForSimpleName(secondX)); //The second x is not a use
		
		SimpleName thirdX = PMASTQuery.simpleNameWithIdentifierInMethodInClassInCompilationUnit("x", 2, "m", 0, "S", 0, compilationUnit);

		PMUse thirdXUse = rdefs.useForSimpleName(thirdX);
		
		assertTrue(thirdXUse != null);
		
		assertEquals(1, thirdXUse.getReachingDefinitions().size());
		
		PMDef xInitializerDef = (PMDef)thirdXUse.getReachingDefinitions().toArray()[0];
		
		VariableDeclaration xDeclaration = PMASTQuery.localWithNameInMethodInClassInCompilationUnit("x", 0, "m", 0, "S", 0, compilationUnit);
		
		
		assertEquals(xDeclaration, xInitializerDef.getDefiningNode());
	}
	
	@Test public void testUseInInitializer() {
		String source = "public class S {void m(){int y = 1;int x = y;}}";
		
		CompilationUnit compilationUnit = parseCompilationUnitFromSource(source, "S.java");
		
		MethodDeclaration methodDeclaration = (MethodDeclaration) PMASTQuery.methodWithNameInClassInCompilationUnit("m", 0, "S", 0, compilationUnit);
	
		PMRDefsAnalysis rdefs = new PMRDefsAnalysis(methodDeclaration);

		SimpleName firstY = PMASTQuery.simpleNameWithIdentifierInMethodInClassInCompilationUnit("y", 0, "m", 0, "S", 0, compilationUnit);
		assertEquals(null, rdefs.useForSimpleName(firstY)); //The first y is not a use
		
		SimpleName firstX = PMASTQuery.simpleNameWithIdentifierInMethodInClassInCompilationUnit("x", 0, "m", 0, "S", 0, compilationUnit);
		assertEquals(null, rdefs.useForSimpleName(firstX)); //The first x is not a use
		
		SimpleName secondY = PMASTQuery.simpleNameWithIdentifierInMethodInClassInCompilationUnit("y", 1, "m", 0, "S", 0, compilationUnit);
		PMUse secondYUse = rdefs.useForSimpleName(secondY);
		
		assertTrue(secondYUse != null);
		
		assertEquals(1, secondYUse.getReachingDefinitions().size());
		
		PMDef defForSecondYUse = (PMDef)secondYUse.getReachingDefinitions().toArray()[0];
		
		VariableDeclaration yDeclaration = PMASTQuery.localWithNameInMethodInClassInCompilationUnit("y", 0, "m", 0, "S", 0, compilationUnit);

		assertEquals(yDeclaration, defForSecondYUse.getDefiningNode());
	}
	
	@Test public void testUseInPostIncrement() {
		String source = "public class S {void m(){int y = 1;int x = y++;}}";
		
		CompilationUnit compilationUnit = parseCompilationUnitFromSource(source, "S.java");
		
		MethodDeclaration methodDeclaration = (MethodDeclaration) PMASTQuery.methodWithNameInClassInCompilationUnit("m", 0, "S", 0, compilationUnit);
		
		PMRDefsAnalysis rdefs = new PMRDefsAnalysis(methodDeclaration);
		
		SimpleName secondY = PMASTQuery.simpleNameWithIdentifierInMethodInClassInCompilationUnit("y", 1, "m", 0, "S", 0, compilationUnit);
		PMUse secondYUse = rdefs.useForSimpleName(secondY);
		
		assertTrue(secondYUse != null);
		
		assertEquals(1, secondYUse.getReachingDefinitions().size());
		
		PMDef defForSecondYUse = (PMDef)secondYUse.getReachingDefinitions().toArray()[0];
		
		VariableDeclaration yDeclaration = PMASTQuery.localWithNameInMethodInClassInCompilationUnit("y", 0, "m", 0, "S", 0, compilationUnit);

		assertEquals(yDeclaration, defForSecondYUse.getDefiningNode());
	}
	
	@Test public void testDefinitionInPostIncrement() {
		String source = "public class S {void m(){int y = 1; y++; int x = y;}}";
		
		CompilationUnit compilationUnit = parseCompilationUnitFromSource(source, "S.java");
		
		MethodDeclaration methodDeclaration = (MethodDeclaration) PMASTQuery.methodWithNameInClassInCompilationUnit("m", 0, "S", 0, compilationUnit);
		
		PMRDefsAnalysis rdefs = new PMRDefsAnalysis(methodDeclaration);
		
		SimpleName thirdY = PMASTQuery.simpleNameWithIdentifierInMethodInClassInCompilationUnit("y", 2, "m", 0, "S", 0, compilationUnit);
		PMUse thirdYUse = rdefs.useForSimpleName(thirdY);
		
		assertTrue(thirdYUse != null);
		
		assertEquals(1, thirdYUse.getReachingDefinitions().size());
		
		PMDef defForThirdYUse = (PMDef)thirdYUse.getReachingDefinitions().toArray()[0];
		
		PostfixExpression yPlusPlus = (PostfixExpression)PMASTQuery.simpleNameWithIdentifierInMethodInClassInCompilationUnit("y", 1, "m", 0, "S", 0, compilationUnit).getParent();
		
		assertEquals(yPlusPlus, defForThirdYUse.getDefiningNode());
	}
	
	
	@Test public void testDefinitionsInIfThenElseBody() {
		String source = "public class S {void m(){int y = 1; if (true) y = 5; else y = 6; System.out.println(y);}}";
		
		CompilationUnit compilationUnit = parseCompilationUnitFromSource(source, "S.java");
		
		MethodDeclaration methodDeclaration = (MethodDeclaration) PMASTQuery.methodWithNameInClassInCompilationUnit("m", 0, "S", 0, compilationUnit);
		
		PMRDefsAnalysis rdefs = new PMRDefsAnalysis(methodDeclaration);
	
		SimpleName fourthY = PMASTQuery.simpleNameWithIdentifierInMethodInClassInCompilationUnit("y", 3, "m", 0, "S", 0, compilationUnit);
		PMUse fourthYUse = rdefs.useForSimpleName(fourthY);
		
		assertTrue(fourthYUse != null);
		
		assertEquals(2, fourthYUse.getReachingDefinitions().size());
		
		Object definitions[] =  fourthYUse.getReachingDefinitions().toArray();
		
		PMDef definition1 = (PMDef) definitions[0];
		PMDef definition2 = (PMDef) definitions[1];
		
		Assignment yEquals5 = PMASTQuery.assignmentInMethodInClassInCompilationUnit(0, "m", 0, "S", 0, compilationUnit);
		Assignment yEquals6 = PMASTQuery.assignmentInMethodInClassInCompilationUnit(1, "m", 0, "S", 0, compilationUnit);
		
		assertTrue(definition1.getDefiningNode() == yEquals5 && definition2.getDefiningNode() == yEquals6 ||
				definition1.getDefiningNode() == yEquals6 && definition2.getDefiningNode() == yEquals5 );

		
	}
	
	@Test public void testDefinitionsInIfThenBody() {
		String source = "public class S {void m(){int y = 1; if (true) y = 5; System.out.println(y);}}";
		
		CompilationUnit compilationUnit = parseCompilationUnitFromSource(source, "S.java");
		
		MethodDeclaration methodDeclaration = (MethodDeclaration) PMASTQuery.methodWithNameInClassInCompilationUnit("m", 0, "S", 0, compilationUnit);
		
		PMRDefsAnalysis rdefs = new PMRDefsAnalysis(methodDeclaration);
	
		SimpleName thirdY = PMASTQuery.simpleNameWithIdentifierInMethodInClassInCompilationUnit("y", 2, "m", 0, "S", 0, compilationUnit);
		PMUse thirdYUse = rdefs.useForSimpleName(thirdY);
		
		assertTrue(thirdYUse != null);
		
		assertEquals(2, thirdYUse.getReachingDefinitions().size());
		
		
		Object definitions[] =  thirdYUse.getReachingDefinitions().toArray();
		
		PMDef definition1 = (PMDef) definitions[0];
		PMDef definition2 = (PMDef) definitions[1];
		
		VariableDeclaration yEquals1 = PMASTQuery.localWithNameInMethodInClassInCompilationUnit("y", 0, "m", 0, "S", 0, compilationUnit);
		Assignment yEquals5 = PMASTQuery.assignmentInMethodInClassInCompilationUnit(0, "m", 0, "S", 0, compilationUnit);
		
		assertTrue(definition1.getDefiningNode() == yEquals1 && definition2.getDefiningNode() == yEquals5 ||
				definition1.getDefiningNode() == yEquals5 && definition2.getDefiningNode() == yEquals1 );

		
	}
	
	
	Set<ASTNode> definingNodesFromDefinitions(Set<PMDef> definitions) {
		Set<ASTNode> definingNodes = new HashSet<ASTNode>();
		
		for (PMDef definition: definitions) {
			definingNodes.add(definition.getDefiningNode());
		}
		
		return definingNodes;
	}
	
	@Test public void testDefinitionsInNestedIfThenElse() {
		String source = "public class S {void m(){int y = 1; if (true) { if (false) y = 2; else y = 3;} else {if (true) y = 4; else y = 5;} System.out.println(y);}}";
		
		CompilationUnit compilationUnit = parseCompilationUnitFromSource(source, "S.java");
		
		MethodDeclaration methodDeclaration = (MethodDeclaration) PMASTQuery.methodWithNameInClassInCompilationUnit("m", 0, "S", 0, compilationUnit);
		
		PMRDefsAnalysis rdefs = new PMRDefsAnalysis(methodDeclaration);
	
		SimpleName sixthY = PMASTQuery.simpleNameWithIdentifierInMethodInClassInCompilationUnit("y", 5, "m", 0, "S", 0, compilationUnit);
		PMUse sixthYUse = rdefs.useForSimpleName(sixthY);
		
		assertTrue(sixthYUse != null);
		
		assertEquals(4, sixthYUse.getReachingDefinitions().size());
		
		Set<ASTNode> definingNodes = definingNodesFromDefinitions(sixthYUse.getReachingDefinitions());
		
		
		Assignment yEquals2 = PMASTQuery.assignmentInMethodInClassInCompilationUnit(0, "m", 0, "S", 0, compilationUnit);
		Assignment yEquals3 = PMASTQuery.assignmentInMethodInClassInCompilationUnit(1, "m", 0, "S", 0, compilationUnit);
		Assignment yEquals4 = PMASTQuery.assignmentInMethodInClassInCompilationUnit(2, "m", 0, "S", 0, compilationUnit);
		Assignment yEquals5 = PMASTQuery.assignmentInMethodInClassInCompilationUnit(3, "m", 0, "S", 0, compilationUnit);

		assertTrue(definingNodes.contains(yEquals2));
		assertTrue(definingNodes.contains(yEquals3));
		assertTrue(definingNodes.contains(yEquals4));
		assertTrue(definingNodes.contains(yEquals5));
	}
	
	
	@Test public void testDefinitionsInWhileLoop() {
		String source = "public class S {void m(){int y = 1; while(y < 6) {y = y + 1;} System.out.println(y);}}";
		
		CompilationUnit compilationUnit = parseCompilationUnitFromSource(source, "S.java");
		
		MethodDeclaration methodDeclaration = (MethodDeclaration) PMASTQuery.methodWithNameInClassInCompilationUnit("m", 0, "S", 0, compilationUnit);
		
		PMRDefsAnalysis rdefs = new PMRDefsAnalysis(methodDeclaration);
	
		
		SimpleName fourthY = PMASTQuery.simpleNameWithIdentifierInMethodInClassInCompilationUnit("y", 3, "m", 0, "S", 0, compilationUnit);
		PMUse fourthYUse = rdefs.useForSimpleName(fourthY);
		
		
		assertTrue(fourthYUse != null);
		
		assertEquals(2, fourthYUse.getReachingDefinitions().size());
		
		
		Set<ASTNode> fourthYUseDefiningNodes = definingNodesFromDefinitions(fourthYUse.getReachingDefinitions());
		
		VariableDeclaration yEquals1 = PMASTQuery.localWithNameInMethodInClassInCompilationUnit("y", 0, "m", 0, "S", 0, compilationUnit);

		Assignment yEqualsYPlus1 = PMASTQuery.assignmentInMethodInClassInCompilationUnit(0, "m", 0, "S", 0, compilationUnit);
		
		assertTrue(fourthYUseDefiningNodes.contains(yEquals1));
		assertTrue(fourthYUseDefiningNodes.contains(yEqualsYPlus1));
		
		SimpleName fifthY = PMASTQuery.simpleNameWithIdentifierInMethodInClassInCompilationUnit("y", 4, "m", 0, "S", 0, compilationUnit);
		PMUse fifthYUse = rdefs.useForSimpleName(fifthY);
			
		assertTrue(fifthYUse != null);
		assertEquals(2, fifthYUse.getReachingDefinitions().size());
		
		Set<ASTNode> fifthYUseDefiningNodes = definingNodesFromDefinitions(fourthYUse.getReachingDefinitions());
		assertTrue(fifthYUseDefiningNodes.contains(yEquals1));
		assertTrue(fifthYUseDefiningNodes.contains(yEqualsYPlus1));
		
	}
	
	@Test public void testUseOfInstanceVariableWithoutDefinition() {
		String source = "public class S {int y; void m(){System.out.println(y);}}";
		
		CompilationUnit compilationUnit = parseCompilationUnitFromSource(source, "S.java");
		
		MethodDeclaration methodDeclaration = (MethodDeclaration) PMASTQuery.methodWithNameInClassInCompilationUnit("m", 0, "S", 0, compilationUnit);
		
		PMRDefsAnalysis rdefs = new PMRDefsAnalysis(methodDeclaration);
		
		SimpleName secondY = PMASTQuery.simpleNameWithIdentifierInMethodInClassInCompilationUnit("y", 0, "m", 0, "S", 0, compilationUnit);
		assertTrue(secondY != null);
		
		PMUse secondYUse = rdefs.useForSimpleName(secondY);
	
		assertEquals(1, secondYUse.getReachingDefinitions().size());
		
		assertEquals(null, secondYUse.getReachingDefinitions().toArray()[0]);
	}
	
	@Test public void testUseOfInstanceVariableBeforeDefinition() {
		String source = "public class S {int y; void m(){System.out.println(y); y = 5;}}";
		
		CompilationUnit compilationUnit = parseCompilationUnitFromSource(source, "S.java");
		
		MethodDeclaration methodDeclaration = (MethodDeclaration) PMASTQuery.methodWithNameInClassInCompilationUnit("m", 0, "S", 0, compilationUnit);
		
		PMRDefsAnalysis rdefs = new PMRDefsAnalysis(methodDeclaration);
		
		SimpleName secondY = PMASTQuery.simpleNameWithIdentifierInMethodInClassInCompilationUnit("y", 0, "m", 0, "S", 0, compilationUnit);
		assertTrue(secondY != null);
		
		PMUse secondYUse = rdefs.useForSimpleName(secondY);
	
		assertEquals(1, secondYUse.getReachingDefinitions().size());
		
		assertEquals(null, secondYUse.getReachingDefinitions().toArray()[0]);
	}
	
	@Test public void testMethodParameterWithoutUse() {
		String source = "public class S {void m(String object){ }}";
		
		CompilationUnit compilationUnit = parseCompilationUnitFromSource(source, "S.java");
		
		MethodDeclaration methodDeclaration = (MethodDeclaration) PMASTQuery.methodWithNameInClassInCompilationUnit("m", 0, "S", 0, compilationUnit);
		
		PMRDefsAnalysis rdefs = new PMRDefsAnalysis(methodDeclaration);
		
		rdefs.toString();
		
		//we really just want to make sure this doesn't blow up.
	}
	
	@Test public void testMethodParameterWithUse() {
		String source = "public class S {void m(String x){System.out.println(x); }}";
		
		CompilationUnit compilationUnit = parseCompilationUnitFromSource(source, "S.java");
		
		MethodDeclaration methodDeclaration = (MethodDeclaration) PMASTQuery.methodWithNameInClassInCompilationUnit("m", 0, "S", 0, compilationUnit);
		
		PMRDefsAnalysis rdefs = new PMRDefsAnalysis(methodDeclaration);
		
		
		SimpleName firstX = PMASTQuery.simpleNameWithIdentifierInMethodInClassInCompilationUnit("x", 0, "m", 0, "S", 0, compilationUnit);
		assertTrue(firstX != null);
		
		PMUse firstXUse = rdefs.useForSimpleName(firstX);
		
		assertEquals(1, firstXUse.getReachingDefinitions().size());
		
		assertEquals(null, firstXUse.getReachingDefinitions().toArray()[0]); //null means uninitialized
	}
	
	@Test public void testFieldAccessDefinition() {
		String source = "public class S {int y;void m(S x){x.y = 1; }}";
		
		CompilationUnit compilationUnit = parseCompilationUnitFromSource(source, "S.java");
		
		MethodDeclaration methodDeclaration = (MethodDeclaration) PMASTQuery.methodWithNameInClassInCompilationUnit("m", 0, "S", 0, compilationUnit);
		
		PMRDefsAnalysis rdefs = new PMRDefsAnalysis(methodDeclaration);
		rdefs.toString();
		
	}
	
	@Test public void testNonDefiningPrefixExpression() {
		String source = "public class S {bool y;void m(){if (!y);}}";
		
		CompilationUnit compilationUnit = parseCompilationUnitFromSource(source, "S.java");
		
		MethodDeclaration methodDeclaration = (MethodDeclaration) PMASTQuery.methodWithNameInClassInCompilationUnit("m", 0, "S", 0, compilationUnit);
		
		PMRDefsAnalysis rdefs = new PMRDefsAnalysis(methodDeclaration);
		
		rdefs.toString();
		//Just want to make sure it doesn't blow up
	}
	
	@Test public void testArrrayAccessDefinition() {
		String source = "public class S {int[] y;void m(){y[5] = 6;}}";
		
		
		CompilationUnit compilationUnit = parseCompilationUnitFromSource(source, "S.java");
		
		MethodDeclaration methodDeclaration = (MethodDeclaration) PMASTQuery.methodWithNameInClassInCompilationUnit("m", 0, "S", 0, compilationUnit);
		
		PMRDefsAnalysis rdefs = new PMRDefsAnalysis(methodDeclaration);
		
		rdefs.toString();
		//Just want to make sure it doesn't blow up
	}
	
	
}
