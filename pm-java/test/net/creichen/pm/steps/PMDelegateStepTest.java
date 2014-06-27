/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm.steps;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Set;

import net.creichen.pm.PMASTQuery;
import net.creichen.pm.PMCompilationUnit;
import net.creichen.pm.PMProject;
import net.creichen.pm.PMWorkspace;
import net.creichen.pm.inconsistencies.PMInconsistency;
import net.creichen.pm.tests.PMTest;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.junit.Test;

public class PMDelegateStepTest extends PMTest {

    @Test
    public void testDelegateWithNameCapture() {

        final String source1 = "public class S1 { void m(){/*S1*/}}";
        final String source2 = "public class S2 {void m(){/*S2*/} void b(){S1 s1; m();} }";

        createNewCompilationUnit("", "S1.java", source1);
        final ICompilationUnit compilationUnit2 = createNewCompilationUnit("", "S2.java", source2);

        final PMProject pmProject = PMWorkspace.sharedWorkspace().projectForIJavaProject(
                this.iJavaProject);

        final PMCompilationUnit s2PMCompilationUnit = pmProject
                .getPMCompilationUnitForICompilationUnit(compilationUnit2);

        final MethodDeclaration methodB = PMASTQuery.methodWithNameInClassInCompilationUnit("b", 0,
                "S2", 0, s2PMCompilationUnit.getASTNode());

        final ExpressionStatement methodInvocationStatement = (ExpressionStatement) methodB
                .getBody().statements().get(1);

        final MethodInvocation callToB = (MethodInvocation) methodInvocationStatement
                .getExpression();

        final PMDelegateStep step = new PMDelegateStep(pmProject, callToB);

        step.setDelegateIdentifier("s1");

        step.applyAllAtOnce();

        assertTrue(compilationUnitSourceMatchesSource(
                "public class S2 {void m(){/*S2*/} void b(){S1 s1; s1.m();} }",
                s2PMCompilationUnit.getSource()));

        final Set<PMInconsistency> inconsistencies = pmProject.allInconsistencies();

        assertEquals(1, inconsistencies.size());

    }

}
