/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm.steps;

import static net.creichen.pm.tests.Matchers.hasPMSource;
import static net.creichen.pm.utils.ASTQuery.findClassByName;
import static net.creichen.pm.utils.ASTQuery.findMethodByName;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import net.creichen.pm.api.PMCompilationUnit;
import net.creichen.pm.consistency.ConsistencyValidator;
import net.creichen.pm.tests.PMTest;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.junit.Test;

public class DelegateStepTest extends PMTest {

    @Test
    public void testDelegateWithNameCapture() {

        final String source1 = "public class S1 { void m(){/*S1*/}}";
        final String source2 = "public class S2 {void m(){/*S2*/} void b(){S1 s1; m();} }";

        createCompilationUnit("", "S1.java", source1);
        final ICompilationUnit compilationUnit2 = createCompilationUnit("", "S2.java", source2);

        final PMCompilationUnit s2PMCompilationUnit = getProject().getPMCompilationUnit(compilationUnit2);

        final TypeDeclaration type = findClassByName("S2", s2PMCompilationUnit);
        final MethodDeclaration methodB = findMethodByName("b", type);

        final ExpressionStatement methodInvocationStatement = (ExpressionStatement) methodB.getBody().statements()
                .get(1);

        final MethodInvocation callToB = (MethodInvocation) methodInvocationStatement.getExpression();

        final DelegateStep step = new DelegateStep(getProject(), callToB);

        step.setDelegateIdentifier("s1");

        step.apply();

        assertThat(s2PMCompilationUnit, hasPMSource("public class S2 {void m(){/*S2*/} void b(){S1 s1; s1.m();} }"));
        assertThat(ConsistencyValidator.getInstance().getInconsistencies(), hasSize(1));

    }

}
