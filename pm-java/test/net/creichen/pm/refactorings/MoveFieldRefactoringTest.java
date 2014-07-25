/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm.refactorings;

import static org.junit.Assert.assertTrue;
import net.creichen.pm.ASTQuery;
import net.creichen.pm.PMWorkspace;
import net.creichen.pm.api.PMProject;
import net.creichen.pm.tests.PMTest;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.junit.Test;

public class MoveFieldRefactoringTest extends PMTest {

    @Test
    public void testMoveField() throws JavaModelException {
        final ICompilationUnit iCompilationUnitS = createNewCompilationUnit("", "S.java",
                "public class S { int _y; void m() {int x; _y = 7; x = 5; System.out.println(x);} }");
        final ICompilationUnit iCompilationUnitT = createNewCompilationUnit("", "T.java",
                "public class T {  }");

        final PMProject project = PMWorkspace.sharedWorkspace().projectForIJavaProject(
                this.getIJavaProject());

        final CompilationUnit compilationUnitS = (CompilationUnit) project
                .findASTRootForICompilationUnit(iCompilationUnitS);
        final CompilationUnit compilationUnitT = (CompilationUnit) project
                .findASTRootForICompilationUnit(iCompilationUnitT);

        final FieldDeclaration yField = (FieldDeclaration) ASTQuery
                .fieldWithNameInClassInCompilationUnit("_y", 0, "S", 0, compilationUnitS)
                .getParent();

        final TypeDeclaration classT = ASTQuery.classWithNameInCompilationUnit("T", 0,
                compilationUnitT);

        final MoveFieldRefactoring refactoring = new MoveFieldRefactoring(project, yField,
                classT);

        refactoring.apply();

        assertTrue(compilationUnitSourceMatchesSource(
                "public class S {void m() {int x; _y = 7; x = 5; System.out.println(x);} }",
                iCompilationUnitS.getSource()));

        assertTrue(compilationUnitSourceMatchesSource("public class T { int _y;  }",
                iCompilationUnitT.getSource()));
    }
}
