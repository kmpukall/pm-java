/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm.steps;

import static net.creichen.pm.tests.Matchers.hasNoInconsistencies;
import static net.creichen.pm.utils.ASTQuery.findClassByName;
import static net.creichen.pm.utils.ASTQuery.findSimpleNames;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.util.List;

import net.creichen.pm.api.PMCompilationUnit;
import net.creichen.pm.tests.PMTest;
import net.creichen.pm.utils.ASTQuery;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.junit.Ignore;
import org.junit.Test;

public class ExtractMethodStepTest extends PMTest {

    @Test
    @Ignore
    public void testExtractLocalVariableExpression() throws JavaModelException {
        final String source = "class S {String _s; void m(int i) {int j; System.out.println(_s + i + j);}}";
        final ICompilationUnit compilationUnitS = createCompilationUnit("", "S.java", source);
        final PMCompilationUnit pmCompilationUnitS = getProject().getPMCompilationUnit(compilationUnitS);
        final TypeDeclaration type = findClassByName("S", pmCompilationUnitS);
        final MethodDeclaration methodDeclaration = ASTQuery.findMethodByName("m", type);
        final Block bodyBlock = methodDeclaration.getBody();
        final ExpressionStatement printlnStatement = (ExpressionStatement) bodyBlock.statements().get(1);
        final MethodInvocation methodInvocation = (MethodInvocation) printlnStatement.getExpression();
        final Expression expression = (Expression) methodInvocation.arguments().get(0);

        final ExtractMethodStep step = new ExtractMethodStep(getProject(), expression);

        step.apply();

        assertThat(
                compilationUnitS.getSource(),
                is(equalTo("class S {String _s; void m(int i) {int j; System.out.println(extractedMethod(i, j));}\r\nfinal String extractedMethod(int i, int j) {\r\n\treturn _s + i + j;\r\n}}")));
        assertThat(getProject(), hasNoInconsistencies());
    }

    @Test
    public void testGetNamesToExtract() {
        final String source = "class S {String _s; void m(int i) {int j; System.out.println(_s + i + j);}}";
        final ICompilationUnit compilationUnitS = createCompilationUnit("", "S.java", source);
        final PMCompilationUnit pmCompilationUnitS = getProject().getPMCompilationUnit(compilationUnitS);
        final TypeDeclaration type = findClassByName("S", pmCompilationUnitS);
        final MethodDeclaration methodDeclaration = ASTQuery.findMethodByName("m", type);
        final Block bodyBlock = methodDeclaration.getBody();
        final ExpressionStatement printlnStatement = (ExpressionStatement) bodyBlock.statements().get(1);
        final MethodInvocation methodInvocation = (MethodInvocation) printlnStatement.getExpression();
        final Expression expression = (Expression) methodInvocation.arguments().get(0);

        final ExtractMethodStep step = new ExtractMethodStep(getProject(), expression);

        final List<SimpleName> namesToExtract = step.getNamesToExtract();
        assertEquals(2, namesToExtract.size());
        assertEquals(findSimpleNames("i", methodDeclaration.getBody()).get(0), namesToExtract.get(0));
        assertEquals(findSimpleNames("j", methodDeclaration.getBody()).get(1), namesToExtract.get(1));

    }
}
