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

import net.creichen.pm.PMTest;
import net.creichen.pm.Project;
import net.creichen.pm.Workspace;
import net.creichen.pm.analysis.ASTQuery;
import net.creichen.pm.api.PMCompilationUnit;
import net.creichen.pm.inconsistencies.Inconsistency;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.junit.Test;

public class DelegateStepTest extends PMTest {

    @Test
    public void testDelegateWithNameCapture() {

        final String source1 = "public class S1 { void m(){/*S1*/}}";
        final String source2 = "public class S2 {void m(){/*S2*/} void b(){S1 s1; m();} }";

        createNewCompilationUnit("", "S1.java", source1);
        final ICompilationUnit compilationUnit2 = createNewCompilationUnit("", "S2.java", source2);

        final Project pmProject = Workspace.sharedWorkspace().projectForIJavaProject(
                this.getIJavaProject());

        final PMCompilationUnit s2PMCompilationUnit = pmProject
                .getPMCompilationUnitForICompilationUnit(compilationUnit2);

        final MethodDeclaration methodB = ASTQuery.methodWithNameInClassInCompilationUnit("b", 0,
                "S2", 0, s2PMCompilationUnit.getASTNode());

        final ExpressionStatement methodInvocationStatement = (ExpressionStatement) methodB
                .getBody().statements().get(1);

        final MethodInvocation callToB = (MethodInvocation) methodInvocationStatement
                .getExpression();

        final DelegateStep step = new DelegateStep(pmProject, callToB);

        step.setDelegateIdentifier("s1");

        step.applyAllAtOnce();

        assertTrue(compilationUnitSourceMatchesSource(
                "public class S2 {void m(){/*S2*/} void b(){S1 s1; s1.m();} }",
                s2PMCompilationUnit.getSource()));

        final Set<Inconsistency> inconsistencies = pmProject.allInconsistencies();

        assertEquals(1, inconsistencies.size());

    }

}
