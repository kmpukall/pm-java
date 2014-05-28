/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm.steps;

import static org.junit.Assert.*;

import java.util.Set;

import net.creichen.pm.PMASTQuery;
import net.creichen.pm.PMCompilationUnit;
import net.creichen.pm.PMProject;
import net.creichen.pm.PMWorkspace;
import net.creichen.pm.inconsistencies.PMInconsistency;
import net.creichen.pm.steps.PMRenameStep;
import net.creichen.pm.tests.PMTest;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.SimpleName;
import org.junit.Test;

public class PMRenameStepTest extends PMTest {

	@Test
	public void testLocalWithMethodInvocation() throws JavaModelException {
		String sourceS = "public class S {void sMethod() {}}";
		String sourceT = "public class T {void m() {S s = new S(); s.sMethod();} }";

		/* ICompilationUnit compilationUnitS = */

		createNewCompilationUnit("", "S.java", sourceS);
		ICompilationUnit compilationUnitT = createNewCompilationUnit("",
				"T.java", sourceT);

		PMProject pmProject = PMWorkspace.sharedWorkspace()
				.projectForIJavaProject(_iJavaProject);

		SimpleName firstSInT = PMASTQuery
				.simpleNameWithIdentifierInMethodInClassInCompilationUnit(
						"s",
						0,
						"m",
						0,
						"T",
						0,
						(CompilationUnit) pmProject
								.findASTRootForICompilationUnit(compilationUnitT));

		PMRenameStep step = new PMRenameStep(pmProject, firstSInT);

		step.setNewName("sInstance");

		step.applyAllAtOnce();

		assertTrue(compilationUnitSourceMatchesSource(
				"public class T {void m() {S sInstance = new S(); sInstance.sMethod();} }",
				compilationUnitT.getSource()));

		IProblem[] problemsT = ((CompilationUnit) pmProject
				.findASTRootForICompilationUnit(compilationUnitT))
				.getProblems();

		assertEquals(0, problemsT.length);
	}

	@Test
	public void testRenameClassToNewName() throws JavaModelException {
		String sourceS = "public class S {void sMethod() {}}";

		ICompilationUnit compilationUnitS = createNewCompilationUnit("",
				"S.java", sourceS);

		PMProject pmProject = PMWorkspace.sharedWorkspace()
				.projectForIJavaProject(_iJavaProject);

		PMCompilationUnit pmCompilationUnitS = pmProject
				.getPMCompilationUnitForICompilationUnit(compilationUnitS);

		TypeDeclaration classS = PMASTQuery.classWithNameInCompilationUnit("S",
				0, (CompilationUnit) pmProject
						.findASTRootForICompilationUnit(compilationUnitS));

		SimpleName className = classS.getName();

		PMRenameStep step = new PMRenameStep(pmProject, className);

		step.setNewName("T");

		step.applyAllAtOnce();

		ICompilationUnit compilationUnitT = pmCompilationUnitS
				.getICompilationUnit();

		assertTrue(compilationUnitSourceMatchesSource(
				"public class T {void sMethod() {}}",
				compilationUnitT.getSource()));

		IProblem[] problems = ((CompilationUnit) pmProject
				.findASTRootForICompilationUnit(compilationUnitT))
				.getProblems();

		assertEquals(0, problems.length);
	}

	@Test
	public void testRenameLocalToShadowField() throws JavaModelException {
		String sourceS = "public class S {" + "String iVar;" + "void m() {"
				+ "String lVar;" + "iVar.length();" + "}" + "}";

		ICompilationUnit compilationUnitS = createNewCompilationUnit("",
				"S.java", sourceS);

		PMProject pmProject = PMWorkspace.sharedWorkspace()
				.projectForIJavaProject(_iJavaProject);

		PMCompilationUnit pmCompilationUnitS = pmProject
				.getPMCompilationUnitForICompilationUnit(compilationUnitS);

		SimpleName name = PMASTQuery
				.simpleNameWithIdentifierInMethodInClassInCompilationUnit(
						"lVar", 0, "m", 0, "S", 0,
						pmCompilationUnitS.getASTNode());

		PMRenameStep step = new PMRenameStep(pmProject, name);

		step.setNewName("iVar");

		step.applyAllAtOnce();

		String expectedNewSourceS = "public class S {" + "String iVar;"
				+ "void m() {" + "String iVar;" + "iVar.length();" + "}" + "}";

		assertTrue(compilationUnitSourceMatchesSource(expectedNewSourceS,
				compilationUnitS.getSource()));

		Set<PMInconsistency> inconsistencies = pmProject.allInconsistencies();

		// Inconsistencies are:
		// [Definition (null) should be used by iVar,
		// iVar was captured.,
		// Unexpected definition (iVar) used by iVar]

		assertEquals(3, inconsistencies.size());

	}

	@Test
	public void testRenameClassWithConstructor() {
		// We expect all constructors to be renamed as well

		String source = "public class A {protected int x; public A(int y) {x=y;} public A() {x = 0; A a = new A(); a = new A(x);} public static class InnerA extends A { } }";

		ICompilationUnit compilationUnitS = createNewCompilationUnit("",
				"S.java", source);

		PMProject pmProject = PMWorkspace.sharedWorkspace()
				.projectForIJavaProject(_iJavaProject);

		PMCompilationUnit pmCompilationUnitS = pmProject
				.getPMCompilationUnitForICompilationUnit(compilationUnitS);

		TypeDeclaration classNode = PMASTQuery.classWithNameInCompilationUnit(
				"A", 0, pmCompilationUnitS.getASTNode());

		SimpleName className = classNode.getName();

		PMRenameStep renameStep = new PMRenameStep(pmProject, className);

		renameStep.setNewName("T");

		renameStep.applyAllAtOnce();

		String expectedNewSource = "public class T {protected int x; public T(int y) {x=y;} public T() {x = 0; T a = new T(); a = new T(x);} public static class InnerA extends T { } }";

		assertTrue(compilationUnitSourceMatchesSource(expectedNewSource,
				pmCompilationUnitS.getSource()));

	}

	@Test
	public void testRenameConstructor() {
		// We expect the class and all other constructors and calls of that
		// construtor will also be renamed

		String source = "public class A {protected int x; public A(int y) {x=y;} public A() {x = 0; A a = new A(); a = new A(x);} public static class InnerA extends A { } }";

		ICompilationUnit compilationUnitS = createNewCompilationUnit("",
				"A.java", source);

		PMProject pmProject = PMWorkspace.sharedWorkspace()
				.projectForIJavaProject(_iJavaProject);

		PMCompilationUnit pmCompilationUnitS = pmProject
				.getPMCompilationUnitForICompilationUnit(compilationUnitS);

		MethodDeclaration methodDeclaration = PMASTQuery
				.methodWithNameInClassInCompilationUnit("A", 0, "A", 0,
						pmCompilationUnitS.getASTNode());

		SimpleName methodName = methodDeclaration.getName();

		PMRenameStep renameStep = new PMRenameStep(pmProject, methodName);

		renameStep.setNewName("x__5");

		renameStep.applyAllAtOnce();

		String expectedNewSource = "public class x__5 {protected int x; public x__5(int y) {x=y;} public x__5() {x = 0; x__5 a = new x__5(); a = new x__5(x);} public static class InnerA extends x__5 { } }";

		assertTrue(compilationUnitSourceMatchesSource(expectedNewSource,
				pmCompilationUnitS.getSource()));

	}

	@Test
	public void testRenameConstructorFollowedByRenameClass() {

		String source = "class S {S() {}}";

		ICompilationUnit compilationUnitS = createNewCompilationUnit("",
				"S.java", source);

		PMProject pmProject = PMWorkspace.sharedWorkspace()
				.projectForIJavaProject(_iJavaProject);

		PMCompilationUnit pmCompilationUnitS = pmProject
				.getPMCompilationUnitForICompilationUnit(compilationUnitS);

		MethodDeclaration methodDeclaration = PMASTQuery
				.methodWithNameInClassInCompilationUnit("S", 0, "S", 0,
						pmCompilationUnitS.getASTNode());

		SimpleName methodName = methodDeclaration.getName();

		PMRenameStep renameConstructorStep = new PMRenameStep(pmProject,
				methodName);

		renameConstructorStep.setNewName("T");

		renameConstructorStep.applyAllAtOnce();

		String expectedNewSourceAfterRenameConstructor = "class T {T() {}}";

		assertTrue(compilationUnitSourceMatchesSource(
				expectedNewSourceAfterRenameConstructor,
				pmCompilationUnitS.getSource()));

		TypeDeclaration classNode = PMASTQuery.classWithNameInCompilationUnit(
				"T", 0, pmCompilationUnitS.getASTNode());

		SimpleName className = classNode.getName();

		PMRenameStep renameClassStep = new PMRenameStep(pmProject, className);

		renameClassStep.setNewName("S");

		renameClassStep.applyAllAtOnce();

		String expectedNewSourceAfterRenameClass = "class S {S() {}}";

		assertTrue(compilationUnitSourceMatchesSource(
				expectedNewSourceAfterRenameClass,
				pmCompilationUnitS.getSource()));

	}

	@Test
	public void testRenameClassFollowedByRenameConstructor() {

		String source = "class S {S() {}}";

		ICompilationUnit compilationUnitS = createNewCompilationUnit("",
				"S.java", source);

		PMProject pmProject = PMWorkspace.sharedWorkspace()
				.projectForIJavaProject(_iJavaProject);

		PMCompilationUnit pmCompilationUnitS = pmProject
				.getPMCompilationUnitForICompilationUnit(compilationUnitS);

		TypeDeclaration classNode = PMASTQuery.classWithNameInCompilationUnit(
				"S", 0, pmCompilationUnitS.getASTNode());

		SimpleName className = classNode.getName();

		PMRenameStep renameClassStep = new PMRenameStep(pmProject, className);

		renameClassStep.setNewName("T");

		renameClassStep.applyAllAtOnce();

		String expectedNewSourceAfterRenameClass = "class T {T() {}}";

		assertTrue(compilationUnitSourceMatchesSource(
				expectedNewSourceAfterRenameClass,
				pmCompilationUnitS.getSource()));

		MethodDeclaration methodDeclaration = PMASTQuery
				.methodWithNameInClassInCompilationUnit("T", 0, "T", 0,
						pmCompilationUnitS.getASTNode());

		SimpleName methodName = methodDeclaration.getName();

		PMRenameStep renameConstructorStep = new PMRenameStep(pmProject,
				methodName);

		renameConstructorStep.setNewName("S");

		renameConstructorStep.applyAllAtOnce();

		String expectedNewSourceAfterRenameConstructor = "class S {S() {}}";

		assertTrue(compilationUnitSourceMatchesSource(
				expectedNewSourceAfterRenameConstructor,
				pmCompilationUnitS.getSource()));

	}

	@Test
	public void testRenameConstructorInComplicatedClass() {
		String sourceA = "package testpackage;"
				+ "public class A {"
				+ "protected int x;"
				+ "protected int unused;"
				+ "public A(int y){x = y;}"
				+ "public void f() { int z = x + 2; x = z; }"
				+ "public void g() { nop(); this.nop(); x *= 3; A instance = new A(7); }"
				+ "public void nop() {}" + "public int getX() { return x;}"
				+ "protected int log_count = 0;"
				+ "protected void do_log(){++log_count;}" + "}";

		String sourceB = "package testpackage;"
				+ "public class B extends A {"
				+ "int b = 7;"
				+ "public B(int y) {super(y);}"
				+ "@Override public void f() { do_log(); nop(); this.other_nop(); x -= b;}"
				+ "protected void other_nop() {this.nop(); this.another_nop();}"
				+ "protected void another_nop(){}" + "}";

		ICompilationUnit compilationUnitA = createNewCompilationUnit(
				"testpackage", "A.java", sourceA);
		ICompilationUnit compilationUnitB = createNewCompilationUnit(
				"testpackage", "B.java", sourceB);

		PMProject pmProject = PMWorkspace.sharedWorkspace()
				.projectForIJavaProject(_iJavaProject);

		PMCompilationUnit pmCompilationUnitA = pmProject
				.getPMCompilationUnitForICompilationUnit(compilationUnitA);
		PMCompilationUnit pmCompilationUnitB = pmProject
				.getPMCompilationUnitForICompilationUnit(compilationUnitB);

		MethodDeclaration methodDeclaration = PMASTQuery
				.methodWithNameInClassInCompilationUnit("A", 0, "A", 0,
						pmCompilationUnitA.getASTNode());

		SimpleName methodName = methodDeclaration.getName();

		PMRenameStep renameStep = new PMRenameStep(pmProject, methodName);

		renameStep.setNewName("x__5");

		renameStep.applyAllAtOnce();

		String expectedTransformedSourceA = "package testpackage;"
				+ "public class x__5 {"
				+ "protected int x;"
				+ "protected int unused;"
				+ "public x__5(int y){x = y;}"
				+ "public void f() { int z = x + 2; x = z; }"
				+ "public void g() { nop(); this.nop(); x *= 3; x__5 instance = new x__5(7); }"
				+ "public void nop() {}" + "public int getX() { return x;}"
				+ "protected int log_count = 0;"
				+ "protected void do_log(){++log_count;}" + "}";

		String expectedTransformedSourceB = "package testpackage;"
				+ "public class B extends x__5 {"
				+ "int b = 7;"
				+ "public B(int y) {super(y);}"
				+ "@Override public void f() { do_log(); nop(); this.other_nop(); x -= b;}"
				+ "protected void other_nop() {this.nop(); this.another_nop();}"
				+ "protected void another_nop(){}" + "}";

		assertTrue(compilationUnitSourceMatchesSource(
				expectedTransformedSourceA, pmCompilationUnitA.getSource()));

		assertTrue(compilationUnitSourceMatchesSource(
				expectedTransformedSourceB, pmCompilationUnitB.getSource()));
	}

	@Test
	public void testRenameConstructorInClassInPackageWithSubclass() {

		String sourceA = "package testpackage; public class A {public A() {System.out.println(6);}}";

		ICompilationUnit compilationUnitA = createNewCompilationUnit(
				"testpackage", "A.java", sourceA);

		String sourceB = "public class B extends testpackage.A {}";

		ICompilationUnit compilationUnitB = createNewCompilationUnit("",
				"B.java", sourceB);

		PMProject pmProject = PMWorkspace.sharedWorkspace()
				.projectForIJavaProject(_iJavaProject);

		PMCompilationUnit pmCompilationUnitA = pmProject
				.getPMCompilationUnitForICompilationUnit(compilationUnitA);

		PMCompilationUnit pmCompilationUnitB = pmProject
				.getPMCompilationUnitForICompilationUnit(compilationUnitB);

		MethodDeclaration methodDeclaration = PMASTQuery
				.methodWithNameInClassInCompilationUnit("A", 0, "A", 0,
						pmCompilationUnitA.getASTNode());

		SimpleName methodName = methodDeclaration.getName();

		PMRenameStep renameStep = new PMRenameStep(pmProject, methodName);

		renameStep.setNewName("x__5");

		renameStep.applyAllAtOnce();

		String expectedNewSourceA = "package testpackage; public class x__5 {public x__5() {System.out.println(6);}}";

		assertTrue(compilationUnitSourceMatchesSource(expectedNewSourceA,
				pmCompilationUnitA.getSource()));

		String expectedNewSourceB = "public class B extends testpackage.x__5 {}";

		assertTrue(compilationUnitSourceMatchesSource(expectedNewSourceB,
				pmCompilationUnitB.getSource()));
	}

	@Test
	public void testRenameClassViaExtendsClause() {
		String sourceA = "public class A {public A() {} public static class InnerA extends A {} }";

		ICompilationUnit compilationUnitA = createNewCompilationUnit("",
				"A.java", sourceA);

		PMProject pmProject = PMWorkspace.sharedWorkspace()
				.projectForIJavaProject(_iJavaProject);

		PMCompilationUnit pmCompilationUnitA = pmProject
				.getPMCompilationUnitForICompilationUnit(compilationUnitA);

		TypeDeclaration outerClassDeclaration = PMASTQuery
				.classWithNameInCompilationUnit("A", 0,
						pmCompilationUnitA.getASTNode());

		TypeDeclaration innerAClassDeclaration = (TypeDeclaration) outerClassDeclaration
				.bodyDeclarations().get(1);

		SimpleName extendsClassName = (SimpleName) ((SimpleType) innerAClassDeclaration
				.getSuperclassType()).getName();

		PMRenameStep renameStep = new PMRenameStep(pmProject, extendsClassName);

		renameStep.setNewName("x__5");

		renameStep.applyAllAtOnce();

		String expectedNewSourceA = "public class x__5 {public x__5() {} public static class InnerA extends x__5 {} }";

		assertTrue(compilationUnitSourceMatchesSource(expectedNewSourceA,
				pmCompilationUnitA.getSource()));
	}

	// Should also test via static class name like Foo.doSomething() and via
	// this expression (Foo.this) and qualified name (Foo.something) in inner
	// class to get to outer class
}
