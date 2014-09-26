/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm.refactorings;

import static org.junit.Assert.assertTrue;
import net.creichen.pm.tests.PMTest;
import net.creichen.pm.utils.ASTQuery;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.junit.Test;

public class MoveFieldRefactoringTest extends PMTest {

    @Test
    public void testMoveField() throws JavaModelException {
        final ICompilationUnit s = createCompilationUnit("", "S.java",
                "public class S { int _y; void m() {int x; _y = 7; x = 5; System.out.println(x);} }");
        final ICompilationUnit t = createCompilationUnit("", "T.java", "public class T {  }");

        final CompilationUnit compilationUnitS = getProject().getCompilationUnit(s);
        final CompilationUnit compilationUnitT = getProject().getCompilationUnit(t);

        final FieldDeclaration yField = (FieldDeclaration) ASTQuery.findFieldByName("_y", 0, "S", 0, compilationUnitS)
                .getParent();

        final TypeDeclaration classT = ASTQuery.findClassByName("T", compilationUnitT);

        final MoveFieldRefactoring refactoring = new MoveFieldRefactoring(getProject(), yField, classT);

        refactoring.apply();

        assertTrue(matchesSource("public class S {void m() {int x; _y = 7; x = 5; System.out.println(x);} }",
                s.getSource()));

        assertTrue(matchesSource("public class T { int _y;  }", t.getSource()));
    }
}
