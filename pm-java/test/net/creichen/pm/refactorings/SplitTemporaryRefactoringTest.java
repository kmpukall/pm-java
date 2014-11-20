/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm.refactorings;

import static net.creichen.pm.tests.Matchers.hasSource;
import static net.creichen.pm.utils.ASTQuery.findAssignments;
import static net.creichen.pm.utils.ASTQuery.findClassByName;
import static net.creichen.pm.utils.ASTQuery.findMethodByName;
import static org.junit.Assert.assertThat;

import java.util.List;

import net.creichen.pm.tests.PMTest;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.junit.Test;

public class SplitTemporaryRefactoringTest extends PMTest {

    @Test
    public void testSimplestCase() throws JavaModelException {
        final ICompilationUnit iCompilationUnit = createCompilationUnit("", "S.java",
                "public class S { void m() {int x; x = 7; x = 5; System.out.println(x);} }");

        TypeDeclaration type = findClassByName("S", getProject().getPMCompilationUnit(iCompilationUnit));
        MethodDeclaration method = findMethodByName("m", type);
        List<Assignment> assignments = findAssignments(method);
        final Assignment secondAssignment = assignments.get(1);

        final ExpressionStatement assignmentStatement = (ExpressionStatement) secondAssignment.getParent();

        final SplitTemporaryRefactoring splitTemporaryRefactoring = new SplitTemporaryRefactoring(getProject(),
                assignmentStatement, "y");

        splitTemporaryRefactoring.apply();

        // Since this is a refactoring, all we care about is a source test

        assertThat(iCompilationUnit,
                hasSource("public class S { void m() {int x; x = 7; int y = 5; System.out.println(y);} }"));

    }
}
