/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm.steps;

import static org.junit.Assert.assertEquals;

import java.util.List;

import net.creichen.pm.PMASTQuery;
import net.creichen.pm.PMCompilationUnit;
import net.creichen.pm.PMProject;
import net.creichen.pm.PMWorkspace;
import net.creichen.pm.tests.PMTest;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.*;
import org.junit.Test;

public class PMExtractMethodStepTest extends PMTest {

    @Test
    public void testExtractLocalVariableExpression() {
        final String source = "class S {String _s; void m(int i) {int j; System.out.println(_s + i + j);}}";

        final ICompilationUnit compilationUnitS = createNewCompilationUnit("", "S.java", source);

        final PMProject pmProject = PMWorkspace.sharedWorkspace().projectForIJavaProject(
                this.iJavaProject);

        final PMCompilationUnit pmCompilationUnitS = pmProject
                .getPMCompilationUnitForICompilationUnit(compilationUnitS);

        final MethodDeclaration methodDeclaration = PMASTQuery
                .methodWithNameInClassInCompilationUnit("m", 0, "S", 0,
                        pmCompilationUnitS.getASTNode());

        final Block bodyBlock = methodDeclaration.getBody();

        final ExpressionStatement printlnStatement = (ExpressionStatement) bodyBlock.statements()
                .get(1);

        final MethodInvocation methodInvocation = (MethodInvocation) printlnStatement
                .getExpression();

        final Expression expression = (Expression) methodInvocation.arguments().get(0);

        final PMExtractMethodStep step = new PMExtractMethodStep(pmProject, expression);

        step.applyAllAtOnce();

        // assertEquals(new HashSet(), pmProject.allInconsistencies());
    }

    @Test
    public void testGetNamesToExtract() {

        final String source = "class S {String _s; void m(int i) {int j; System.out.println(_s + i + j);}}";

        final ICompilationUnit compilationUnitS = createNewCompilationUnit("", "S.java", source);

        final PMProject pmProject = PMWorkspace.sharedWorkspace().projectForIJavaProject(
                this.iJavaProject);

        final PMCompilationUnit pmCompilationUnitS = pmProject
                .getPMCompilationUnitForICompilationUnit(compilationUnitS);

        final MethodDeclaration methodDeclaration = PMASTQuery
                .methodWithNameInClassInCompilationUnit("m", 0, "S", 0,
                        pmCompilationUnitS.getASTNode());

        final Block bodyBlock = methodDeclaration.getBody();

        final ExpressionStatement printlnStatement = (ExpressionStatement) bodyBlock.statements()
                .get(1);

        final MethodInvocation methodInvocation = (MethodInvocation) printlnStatement
                .getExpression();

        final Expression expression = (Expression) methodInvocation.arguments().get(0);

        final PMExtractMethodStep step = new PMExtractMethodStep(pmProject, expression);

        final List<SimpleName> namesToExtract = step.getNamesToExtract();

        assertEquals(2, namesToExtract.size());

        assertEquals(PMASTQuery.simpleNameWithIdentifierInMethodInClassInCompilationUnit("i", 0,
                "m", 0, "S", 0, pmCompilationUnitS.getASTNode()), namesToExtract.get(0));

        assertEquals(PMASTQuery.simpleNameWithIdentifierInMethodInClassInCompilationUnit("j", 1,
                "m", 0, "S", 0, pmCompilationUnitS.getASTNode()), namesToExtract.get(1));

    }

}
