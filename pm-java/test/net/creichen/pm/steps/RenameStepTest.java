/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm.steps;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collection;

import net.creichen.pm.api.PMCompilationUnit;
import net.creichen.pm.consistency.ConsistencyValidator;
import net.creichen.pm.consistency.inconsistencies.Inconsistency;
import net.creichen.pm.tests.PMTest;
import net.creichen.pm.utils.ASTQuery;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.junit.Test;

public class RenameStepTest extends PMTest {

    @Test
    public void testLocalWithMethodInvocation() throws JavaModelException {
        final String sourceS = "public class S {void sMethod() {}}";
        final String sourceT = "public class T {void m() {S s = new S(); s.sMethod();} }";

        /* ICompilationUnit compilationUnitS = */

        createCompilationUnit("", "S.java", sourceS);
        final ICompilationUnit compilationUnitT = createCompilationUnit("", "T.java", sourceT);

        final SimpleName firstSInT = ASTQuery.findSimpleNameByIdentifier("s", 0, "m", 0, "T", 0, getProject()
                .getCompilationUnit(compilationUnitT));

        final RenameStep step = new RenameStep(getProject(), firstSInT);

        step.setNewName("sInstance");

        step.applyAllAtOnce();

        assertTrue(compilationUnitSourceMatchesSource(
                "public class T {void m() {S sInstance = new S(); sInstance.sMethod();} }",
                compilationUnitT.getSource()));

        final IProblem[] problemsT = getProject().getCompilationUnit(compilationUnitT).getProblems();

        assertEquals(0, problemsT.length);
    }

    @Test
    public void testRenameClassFollowedByRenameConstructor() {

        final String source = "class S {S() {}}";

        final ICompilationUnit compilationUnitS = createCompilationUnit("", "S.java", source);

        final PMCompilationUnit pmCompilationUnitS = getProject().getPMCompilationUnitForICompilationUnit(
                compilationUnitS);

        final TypeDeclaration classNode = ASTQuery.findClassByName("S", 0, pmCompilationUnitS.getCompilationUnit());

        final SimpleName className = classNode.getName();

        final RenameStep renameClassStep = new RenameStep(getProject(), className);

        renameClassStep.setNewName("T");

        renameClassStep.applyAllAtOnce();

        final String expectedNewSourceAfterRenameClass = "class T {T() {}}";

        assertTrue(compilationUnitSourceMatchesSource(expectedNewSourceAfterRenameClass, pmCompilationUnitS.getSource()));

        final MethodDeclaration methodDeclaration = ASTQuery.findMethodByName("T", 0, "T", 0,
                pmCompilationUnitS.getCompilationUnit());

        final SimpleName methodName = methodDeclaration.getName();

        final RenameStep renameConstructorStep = new RenameStep(getProject(), methodName);

        renameConstructorStep.setNewName("S");

        renameConstructorStep.applyAllAtOnce();

        final String expectedNewSourceAfterRenameConstructor = "class S {S() {}}";

        assertTrue(compilationUnitSourceMatchesSource(expectedNewSourceAfterRenameConstructor,
                pmCompilationUnitS.getSource()));

    }

    @Test
    public void testRenameClassToNewName() throws JavaModelException {
        final String sourceS = "public class S {void sMethod() {}}";

        final ICompilationUnit compilationUnitS = createCompilationUnit("", "S.java", sourceS);

        final PMCompilationUnit pmCompilationUnitS = getProject().getPMCompilationUnitForICompilationUnit(
                compilationUnitS);

        final TypeDeclaration classS = ASTQuery.findClassByName("S", 0,
                getProject().getCompilationUnit(compilationUnitS));

        final SimpleName className = classS.getName();

        final RenameStep step = new RenameStep(getProject(), className);

        step.setNewName("T");

        step.applyAllAtOnce();

        final ICompilationUnit compilationUnitT = pmCompilationUnitS.getICompilationUnit();

        assertTrue(compilationUnitSourceMatchesSource("public class T {void sMethod() {}}",
                compilationUnitT.getSource()));

        final IProblem[] problems = getProject().getCompilationUnit(compilationUnitT).getProblems();

        assertEquals(0, problems.length);
    }

    @Test
    public void testRenameClassViaExtendsClause() {
        final String sourceA = "public class A {public A() {} public static class InnerA extends A {} }";

        final ICompilationUnit compilationUnitA = createCompilationUnit("", "A.java", sourceA);

        final PMCompilationUnit pmCompilationUnitA = getProject().getPMCompilationUnitForICompilationUnit(
                compilationUnitA);

        final TypeDeclaration outerClassDeclaration = ASTQuery.findClassByName("A", 0,
                pmCompilationUnitA.getCompilationUnit());

        final TypeDeclaration innerAClassDeclaration = (TypeDeclaration) outerClassDeclaration.bodyDeclarations()
                .get(1);

        final SimpleName extendsClassName = (SimpleName) ((SimpleType) innerAClassDeclaration.getSuperclassType())
                .getName();

        final RenameStep renameStep = new RenameStep(getProject(), extendsClassName);

        renameStep.setNewName("x__5");

        renameStep.applyAllAtOnce();

        final String expectedNewSourceA = "public class x__5 {public x__5() {} public static class InnerA extends x__5 {} }";

        assertTrue(compilationUnitSourceMatchesSource(expectedNewSourceA, pmCompilationUnitA.getSource()));
    }

    @Test
    public void testRenameClassWithConstructor() {
        // We expect all constructors to be renamed as well

        final String source = "public class A {protected int x; public A(int y) {x=y;} public A() {x = 0; A a = new A(); a = new A(x);} public static class InnerA extends A { } }";

        final ICompilationUnit compilationUnitS = createCompilationUnit("", "S.java", source);

        final PMCompilationUnit pmCompilationUnitS = getProject().getPMCompilationUnitForICompilationUnit(
                compilationUnitS);

        final TypeDeclaration classNode = ASTQuery.findClassByName("A", 0, pmCompilationUnitS.getCompilationUnit());

        final SimpleName className = classNode.getName();

        final RenameStep renameStep = new RenameStep(getProject(), className);

        renameStep.setNewName("T");

        renameStep.applyAllAtOnce();

        final String expectedNewSource = "public class T {protected int x; public T(int y) {x=y;} public T() {x = 0; T a = new T(); a = new T(x);} public static class InnerA extends T { } }";

        assertTrue(compilationUnitSourceMatchesSource(expectedNewSource, pmCompilationUnitS.getSource()));

    }

    @Test
    public void testRenameConstructor() {
        // We expect the class and all other constructors and calls of that
        // construtor will also be renamed

        final String source = "public class A {protected int x; public A(int y) {x=y;} public A() {x = 0; A a = new A(); a = new A(x);} public static class InnerA extends A { } }";

        final ICompilationUnit compilationUnitS = createCompilationUnit("", "A.java", source);

        final PMCompilationUnit pmCompilationUnitS = getProject().getPMCompilationUnitForICompilationUnit(
                compilationUnitS);

        final MethodDeclaration methodDeclaration = ASTQuery.findMethodByName("A", 0, "A", 0,
                pmCompilationUnitS.getCompilationUnit());

        final SimpleName methodName = methodDeclaration.getName();

        final RenameStep renameStep = new RenameStep(getProject(), methodName);

        renameStep.setNewName("x__5");

        renameStep.applyAllAtOnce();

        final String expectedNewSource = "public class x__5 {protected int x; public x__5(int y) {x=y;} public x__5() {x = 0; x__5 a = new x__5(); a = new x__5(x);} public static class InnerA extends x__5 { } }";

        assertTrue(compilationUnitSourceMatchesSource(expectedNewSource, pmCompilationUnitS.getSource()));

    }

    @Test
    public void testRenameConstructorFollowedByRenameClass() {

        final String source = "class S {S() {}}";

        final ICompilationUnit compilationUnitS = createCompilationUnit("", "S.java", source);

        final PMCompilationUnit pmCompilationUnitS = getProject().getPMCompilationUnitForICompilationUnit(
                compilationUnitS);

        final MethodDeclaration methodDeclaration = ASTQuery.findMethodByName("S", 0, "S", 0,
                pmCompilationUnitS.getCompilationUnit());

        final SimpleName methodName = methodDeclaration.getName();

        final RenameStep renameConstructorStep = new RenameStep(getProject(), methodName);

        renameConstructorStep.setNewName("T");

        renameConstructorStep.applyAllAtOnce();

        final String expectedNewSourceAfterRenameConstructor = "class T {T() {}}";

        assertTrue(compilationUnitSourceMatchesSource(expectedNewSourceAfterRenameConstructor,
                pmCompilationUnitS.getSource()));

        final TypeDeclaration classNode = ASTQuery.findClassByName("T", 0, pmCompilationUnitS.getCompilationUnit());

        final SimpleName className = classNode.getName();

        final RenameStep renameClassStep = new RenameStep(getProject(), className);

        renameClassStep.setNewName("S");

        renameClassStep.applyAllAtOnce();

        final String expectedNewSourceAfterRenameClass = "class S {S() {}}";

        assertTrue(compilationUnitSourceMatchesSource(expectedNewSourceAfterRenameClass, pmCompilationUnitS.getSource()));

    }

    @Test
    public void testRenameConstructorInClassInPackageWithSubclass() {

        final String sourceA = "package testpackage; public class A {public A() {System.out.println(6);}}";

        final ICompilationUnit compilationUnitA = createCompilationUnit("testpackage", "A.java", sourceA);

        final String sourceB = "public class B extends testpackage.A {}";

        final ICompilationUnit compilationUnitB = createCompilationUnit("", "B.java", sourceB);

        final PMCompilationUnit pmCompilationUnitA = getProject().getPMCompilationUnitForICompilationUnit(
                compilationUnitA);

        final PMCompilationUnit pmCompilationUnitB = getProject().getPMCompilationUnitForICompilationUnit(
                compilationUnitB);

        final MethodDeclaration methodDeclaration = ASTQuery.findMethodByName("A", 0, "A", 0,
                pmCompilationUnitA.getCompilationUnit());

        final SimpleName methodName = methodDeclaration.getName();

        final RenameStep renameStep = new RenameStep(getProject(), methodName);

        renameStep.setNewName("x__5");

        renameStep.applyAllAtOnce();

        final String expectedNewSourceA = "package testpackage; public class x__5 {public x__5() {System.out.println(6);}}";

        assertTrue(compilationUnitSourceMatchesSource(expectedNewSourceA, pmCompilationUnitA.getSource()));

        final String expectedNewSourceB = "public class B extends testpackage.x__5 {}";

        assertTrue(compilationUnitSourceMatchesSource(expectedNewSourceB, pmCompilationUnitB.getSource()));
    }

    @Test
    public void testRenameConstructorInComplicatedClass() {
        final String sourceA = "package testpackage;" + "public class A {" + "protected int x;"
                + "protected int unused;" + "public A(int y){x = y;}" + "public void f() { int z = x + 2; x = z; }"
                + "public void g() { nop(); this.nop(); x *= 3; A instance = new A(7); }" + "public void nop() {}"
                + "public int getX() { return x;}" + "protected int log_count = 0;"
                + "protected void do_log(){++log_count;}" + "}";

        final String sourceB = "package testpackage;" + "public class B extends A {" + "int b = 7;"
                + "public B(int y) {super(y);}"
                + "@Override public void f() { do_log(); nop(); this.other_nop(); x -= b;}"
                + "protected void other_nop() {this.nop(); this.another_nop();}" + "protected void another_nop(){}"
                + "}";

        final ICompilationUnit compilationUnitA = createCompilationUnit("testpackage", "A.java", sourceA);
        final ICompilationUnit compilationUnitB = createCompilationUnit("testpackage", "B.java", sourceB);

        final PMCompilationUnit pmCompilationUnitA = getProject().getPMCompilationUnitForICompilationUnit(
                compilationUnitA);
        final PMCompilationUnit pmCompilationUnitB = getProject().getPMCompilationUnitForICompilationUnit(
                compilationUnitB);

        final MethodDeclaration methodDeclaration = ASTQuery.findMethodByName("A", 0, "A", 0,
                pmCompilationUnitA.getCompilationUnit());

        final SimpleName methodName = methodDeclaration.getName();

        final RenameStep renameStep = new RenameStep(getProject(), methodName);

        renameStep.setNewName("x__5");

        renameStep.applyAllAtOnce();

        final String expectedTransformedSourceA = "package testpackage;" + "public class x__5 {" + "protected int x;"
                + "protected int unused;" + "public x__5(int y){x = y;}" + "public void f() { int z = x + 2; x = z; }"
                + "public void g() { nop(); this.nop(); x *= 3; x__5 instance = new x__5(7); }"
                + "public void nop() {}" + "public int getX() { return x;}" + "protected int log_count = 0;"
                + "protected void do_log(){++log_count;}" + "}";

        final String expectedTransformedSourceB = "package testpackage;" + "public class B extends x__5 {"
                + "int b = 7;" + "public B(int y) {super(y);}"
                + "@Override public void f() { do_log(); nop(); this.other_nop(); x -= b;}"
                + "protected void other_nop() {this.nop(); this.another_nop();}" + "protected void another_nop(){}"
                + "}";

        assertTrue(compilationUnitSourceMatchesSource(expectedTransformedSourceA, pmCompilationUnitA.getSource()));

        assertTrue(compilationUnitSourceMatchesSource(expectedTransformedSourceB, pmCompilationUnitB.getSource()));
    }

    @Test
    public void testRenameLocalToShadowField() throws JavaModelException {
        final String sourceS = "public class S {" + "String iVar;" + "void m() {" + "String lVar;" + "iVar.length();"
                + "}" + "}";

        final ICompilationUnit compilationUnitS = createCompilationUnit("", "S.java", sourceS);

        final PMCompilationUnit pmCompilationUnitS = getProject().getPMCompilationUnitForICompilationUnit(
                compilationUnitS);

        final SimpleName name = ASTQuery.findSimpleNameByIdentifier("lVar", 0, "m", 0, "S", 0,
                pmCompilationUnitS.getCompilationUnit());

        final RenameStep step = new RenameStep(getProject(), name);

        step.setNewName("iVar");

        step.applyAllAtOnce();

        final String expectedNewSourceS = "public class S {" + "String iVar;" + "void m() {" + "String iVar;"
                + "iVar.length();" + "}" + "}";

        assertTrue(compilationUnitSourceMatchesSource(expectedNewSourceS, compilationUnitS.getSource()));

        final Collection<Inconsistency> inconsistencies = ConsistencyValidator.getInstance().getInconsistencies();

        // Inconsistencies are:
        // [Definition (null) should be used by iVar,
        // iVar was captured.,
        // Unexpected definition (iVar) used by iVar]

        assertEquals(3, inconsistencies.size());

    }

    // Should also test via static class name like Foo.doSomething() and via
    // this expression (Foo.this) and qualified name (Foo.something) in inner
    // class to get to outer class
}
