/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm.steps;

import static net.creichen.pm.tests.Matchers.hasNoInconsistencies;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.util.List;

import net.creichen.pm.api.PMCompilationUnit;
import net.creichen.pm.core.Project;
import net.creichen.pm.core.Workspace;
import net.creichen.pm.tests.PMTest;
import net.creichen.pm.utils.ASTQuery;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleName;
import org.junit.Ignore;
import org.junit.Test;

public class ExtractMethodStepTest extends PMTest {

    @Test
    @Ignore
    public void testExtractLocalVariableExpression() {
        final String source = "class S {String _s; void m(int i) {int j; System.out.println(_s + i + j);}}";
        final ICompilationUnit compilationUnitS = createNewCompilationUnit("", "S.java", source);
        final Project pmProject = Workspace.getInstance().getProject(getIJavaProject());
        final PMCompilationUnit pmCompilationUnitS = pmProject
                .getPMCompilationUnitForICompilationUnit(compilationUnitS);
        final MethodDeclaration methodDeclaration = ASTQuery.findMethodByName("m", 0, "S", 0,
                pmCompilationUnitS.getCompilationUnit());
        final Block bodyBlock = methodDeclaration.getBody();
        final ExpressionStatement printlnStatement = (ExpressionStatement) bodyBlock.statements().get(1);
        final MethodInvocation methodInvocation = (MethodInvocation) printlnStatement.getExpression();
        final Expression expression = (Expression) methodInvocation.arguments().get(0);

        final ExtractMethodStep step = new ExtractMethodStep(pmProject, expression);

        step.applyAllAtOnce();

        assertThat(pmProject, hasNoInconsistencies());
    }

    @Test
    public void testGetNamesToExtract() {
        final String source = "class S {String _s; void m(int i) {int j; System.out.println(_s + i + j);}}";
        final ICompilationUnit compilationUnitS = createNewCompilationUnit("", "S.java", source);
        final Project pmProject = Workspace.getInstance().getProject(getIJavaProject());
        final PMCompilationUnit pmCompilationUnitS = pmProject
                .getPMCompilationUnitForICompilationUnit(compilationUnitS);
        final MethodDeclaration methodDeclaration = ASTQuery.findMethodByName("m", 0, "S", 0,
                pmCompilationUnitS.getCompilationUnit());
        final Block bodyBlock = methodDeclaration.getBody();
        final ExpressionStatement printlnStatement = (ExpressionStatement) bodyBlock.statements().get(1);
        final MethodInvocation methodInvocation = (MethodInvocation) printlnStatement.getExpression();
        final Expression expression = (Expression) methodInvocation.arguments().get(0);

        final ExtractMethodStep step = new ExtractMethodStep(pmProject, expression);

        final List<SimpleName> namesToExtract = step.getNamesToExtract();
        assertEquals(2, namesToExtract.size());
        assertEquals(
                ASTQuery.findSimpleNameByIdentifier("i", 0, "m", 0, "S", 0, pmCompilationUnitS.getCompilationUnit()),
                namesToExtract.get(0));
        assertEquals(
                ASTQuery.findSimpleNameByIdentifier("j", 1, "m", 0, "S", 0, pmCompilationUnitS.getCompilationUnit()),
                namesToExtract.get(1));

    }

}
