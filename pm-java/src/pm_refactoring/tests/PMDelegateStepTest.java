/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

*******************************************************************************/

package pm_refactoring.tests;

import static org.junit.Assert.*;

import java.util.Set;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.junit.Test;

import pm_refactoring.PMASTQuery;
import pm_refactoring.PMCompilationUnit;
import pm_refactoring.PMProject;
import pm_refactoring.PMWorkspace;
import pm_refactoring.inconsistencies.PMInconsistency;
import pm_refactoring.steps.PMDelegateStep;

public class PMDelegateStepTest extends PMTest {

	@Test public void testDelegateWithNameCapture() {
		
		String source1 = "public class S1 { void m(){/*S1*/}}";
		String source2 = "public class S2 {void m(){/*S2*/} void b(){S1 s1; m();} }";
		
		createNewCompilationUnit("", "S1.java", source1);
		ICompilationUnit compilationUnit2 = createNewCompilationUnit("", "S2.java", source2);
		
		PMProject pmProject = PMWorkspace.sharedWorkspace().projectForIJavaProject(_iJavaProject);
		
		PMCompilationUnit s2PMCompilationUnit = pmProject.getPMCompilationUnitForICompilationUnit(compilationUnit2);
		
		MethodDeclaration methodB = PMASTQuery.methodWithNameInClassInCompilationUnit("b", 0, "S2", 0, s2PMCompilationUnit.getASTNode());
		
		ExpressionStatement methodInvocationStatement = (ExpressionStatement)methodB.getBody().statements().get(1);
	
		MethodInvocation callToB = (MethodInvocation)methodInvocationStatement.getExpression();
		
		PMDelegateStep step = new PMDelegateStep(pmProject, callToB);
		
		step.setDelegateIdentifier("s1");
		
		step.applyAllAtOnce();
		
		assertTrue(compilationUnitSourceMatchesSource("public class S2 {void m(){/*S2*/} void b(){S1 s1; s1.m();} }", s2PMCompilationUnit.getSource()));		

		
		Set<PMInconsistency> inconsistencies = pmProject.allInconsistencies();
		
		assertEquals(1, inconsistencies.size());
		
	}
	
}
