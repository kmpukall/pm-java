/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm.refactorings;

import static org.junit.Assert.assertTrue;
import net.creichen.pm.PMASTQuery;
import net.creichen.pm.PMProject;
import net.creichen.pm.PMWorkspace;
import net.creichen.pm.tests.PMTest;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.junit.Test;

public class PMSplitTemporaryRefactoringTest extends PMTest {

    @Test
    public void testSimplestCase() throws JavaModelException {
        final ICompilationUnit iCompilationUnit = createNewCompilationUnit("", "S.java",
                "public class S { void m() {int x; x = 7; x = 5; System.out.println(x);} }");

        final PMProject project = PMWorkspace.sharedWorkspace().projectForIJavaProject(
                this.getIJavaProject());

        final Assignment secondAssignment = PMASTQuery.assignmentInMethodInClassInCompilationUnit(
                1, "m", 0, "S", 0,
                (CompilationUnit) project.findASTRootForICompilationUnit(iCompilationUnit));

        final ExpressionStatement assignmentStatement = (ExpressionStatement) secondAssignment
                .getParent();

        final PMSplitTemporaryRefactoring splitTemporaryRefactoring = new PMSplitTemporaryRefactoring(
                project, assignmentStatement, "y");

        splitTemporaryRefactoring.apply();

        // Since this is a refactoring, all we care about is a source test

        assertTrue(compilationUnitSourceMatchesSource(
                "public class S { void m() {int x; x = 7; int y = 5; System.out.println(y);} }",
                iCompilationUnit.getSource()));

    }
}
