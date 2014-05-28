/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm.refactorings;

import static org.junit.Assert.*;
import net.creichen.pm.PMASTQuery;
import net.creichen.pm.PMProject;
import net.creichen.pm.PMWorkspace;
import net.creichen.pm.refactorings.PMMoveFieldRefactoring;
import net.creichen.pm.tests.PMTest;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.junit.Test;

public class PMMoveFieldRefactoringTest extends PMTest {

	@Test
	public void testMoveField() throws JavaModelException {
		ICompilationUnit iCompilationUnitS = createNewCompilationUnit(
				"",
				"S.java",
				"public class S { int _y; void m() {int x; _y = 7; x = 5; System.out.println(x);} }");
		ICompilationUnit iCompilationUnitT = createNewCompilationUnit("",
				"T.java", "public class T {  }");

		PMProject project = PMWorkspace.sharedWorkspace()
				.projectForIJavaProject(_iJavaProject);

		CompilationUnit compilationUnitS = (CompilationUnit) project
				.findASTRootForICompilationUnit(iCompilationUnitS);
		CompilationUnit compilationUnitT = (CompilationUnit) project
				.findASTRootForICompilationUnit(iCompilationUnitT);

		FieldDeclaration yField = (FieldDeclaration) PMASTQuery
				.fieldWithNameInClassInCompilationUnit("_y", 0, "S", 0,
						compilationUnitS).getParent();

		TypeDeclaration classT = PMASTQuery.classWithNameInCompilationUnit("T",
				0, compilationUnitT);

		PMMoveFieldRefactoring refactoring = new PMMoveFieldRefactoring(
				project, yField, classT);

		refactoring.apply();

		assertTrue(compilationUnitSourceMatchesSource(
				"public class S {void m() {int x; _y = 7; x = 5; System.out.println(x);} }",
				iCompilationUnitS.getSource()));

		assertTrue(compilationUnitSourceMatchesSource(
				"public class T { int _y;  }", iCompilationUnitT.getSource()));
	}
}
