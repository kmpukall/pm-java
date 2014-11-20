/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm.steps;

import static net.creichen.pm.tests.Matchers.hasNoProblems;
import static net.creichen.pm.tests.Matchers.hasPMSource;
import static net.creichen.pm.tests.Matchers.hasSource;
import static net.creichen.pm.utils.ASTQuery.findClassByName;
import static net.creichen.pm.utils.ASTQuery.findMethodByName;
import static net.creichen.pm.utils.ASTQuery.findSimpleName;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.Collection;

import net.creichen.pm.api.PMCompilationUnit;
import net.creichen.pm.consistency.ConsistencyValidator;
import net.creichen.pm.consistency.inconsistencies.Inconsistency;
import net.creichen.pm.consistency.inconsistencies.NameCapture;
import net.creichen.pm.tests.PMTest;
import net.creichen.pm.utils.ASTQuery;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.junit.Test;

public class RenameStepTest extends PMTest {

    @Test
    public void whenALocalWithMethodInvocationIsRenamed_then_theLocalReferenceIsRenamedToo() throws JavaModelException {
        final String sourceS = "public class S {void sMethod() {}}";
        final String sourceT = "public class T {void m() {S s = new S(); s.sMethod();} }";

        createCompilationUnit("", "S.java", sourceS);
        final ICompilationUnit compilationUnitT = createCompilationUnit("", "T.java", sourceT);

        TypeDeclaration t = findClassByName("T", getProject().getPMCompilationUnit(compilationUnitT));
        MethodDeclaration m = findMethodByName("m", t);
        final SimpleName firstSInT = findSimpleName("s", m);

        final RenameStep step = new RenameStep(getProject(), firstSInT);
        step.setNewName("sInstance");
        step.apply();

        assertThat(compilationUnitT,
                hasSource("public class T {void m() {S sInstance = new S(); sInstance.sMethod();} }"));
        assertThat(getProject().getPMCompilationUnit(compilationUnitT), hasNoProblems());
    }

    @Test
    public void whenTheClassIsRenamed_then_theConstructorIsRenamedToo() {
        final ICompilationUnit compilationUnitS = createCompilationUnit("", "S.java", "class S {S() {}}");
        final PMCompilationUnit pmCompilationUnitS = getProject().getPMCompilationUnit(compilationUnitS);
        final TypeDeclaration classNode = findClassByName("S", pmCompilationUnitS);

        final RenameStep renameClassStep = new RenameStep(getProject(), classNode.getName());
        renameClassStep.setNewName("T");
        renameClassStep.apply();

        assertThat(pmCompilationUnitS, hasPMSource("class T {T() {}}"));
        assertThat(pmCompilationUnitS, hasNoProblems());
    }

    @Test
    public void whenConstructorIsRenamed_then_theClassIsRenamedToo() {
        final ICompilationUnit compilationUnitS = createCompilationUnit("", "T.java", "class T {T() {}}");
        final PMCompilationUnit pmCompilationUnitS = getProject().getPMCompilationUnit(compilationUnitS);
        final TypeDeclaration type = findClassByName("T", pmCompilationUnitS);
        final MethodDeclaration methodDeclaration = findMethodByName("T", type);
        final SimpleName methodName = methodDeclaration.getName();

        final RenameStep renameConstructorStep = new RenameStep(getProject(), methodName);
        renameConstructorStep.setNewName("S");
        renameConstructorStep.apply();

        final String expectedNewSourceAfterRenameConstructor = "class S {S() {}}";
        assertThat(pmCompilationUnitS, hasPMSource(expectedNewSourceAfterRenameConstructor));
        assertThat(pmCompilationUnitS, hasNoProblems());
    }

    @Test
    public void testRenameClassToNewName() throws JavaModelException {
        final String sourceS = "public class S {void sMethod() {}}";
        final ICompilationUnit compilationUnitS = createCompilationUnit("", "S.java", sourceS);
        final PMCompilationUnit pmCompilationUnitS = getProject().getPMCompilationUnit(compilationUnitS);
        final TypeDeclaration classS = ASTQuery.findClassByName("S", pmCompilationUnitS);
        final SimpleName className = classS.getName();

        final RenameStep step = new RenameStep(getProject(), className);
        step.setNewName("T");
        step.apply();

        assertThat(pmCompilationUnitS, hasPMSource("public class T {void sMethod() {}}"));
        assertThat(pmCompilationUnitS, hasNoProblems());
    }

    @Test
    public void testRenameClassViaExtendsClause() {
        final String sourceA = "public class A {public A() {} public static class InnerA extends A {} }";

        final ICompilationUnit compilationUnitA = createCompilationUnit("", "A.java", sourceA);

        final PMCompilationUnit pmCompilationUnitA = getProject().getPMCompilationUnit(compilationUnitA);

        final TypeDeclaration outerClassDeclaration = ASTQuery.findClassByName("A", pmCompilationUnitA);

        final TypeDeclaration innerAClassDeclaration = (TypeDeclaration) outerClassDeclaration.bodyDeclarations()
                .get(1);

        final SimpleName extendsClassName = (SimpleName) ((SimpleType) innerAClassDeclaration.getSuperclassType())
                .getName();

        final RenameStep renameStep = new RenameStep(getProject(), extendsClassName);

        renameStep.setNewName("x__5");

        renameStep.apply();

        assertThat(pmCompilationUnitA,
                hasPMSource("public class x__5 {public x__5() {} public static class InnerA extends x__5 {} }"));
    }

    @Test
    public void testRenameClassWithConstructor() {
        // We expect all constructors to be renamed as well

        final String source = "public class A {protected int x; public A(int y) {x=y;} public A() {x = 0; A a = new A(); a = new A(x);} public static class InnerA extends A { } }";

        final ICompilationUnit compilationUnitS = createCompilationUnit("", "S.java", source);

        final PMCompilationUnit pmCompilationUnitS = getProject().getPMCompilationUnit(compilationUnitS);

        final TypeDeclaration classNode = ASTQuery.findClassByName("A", pmCompilationUnitS);

        final SimpleName className = classNode.getName();

        final RenameStep renameStep = new RenameStep(getProject(), className);

        renameStep.setNewName("T");

        renameStep.apply();

        final String expectedNewSource = "public class T {protected int x; public T(int y) {x=y;} public T() {x = 0; T a = new T(); a = new T(x);} public static class InnerA extends T { } }";

        assertThat(pmCompilationUnitS, hasPMSource(expectedNewSource));

    }

    @Test
    public void testRenameConstructor() {
        // We expect the class and all other constructors and calls of that
        // construtor will also be renamed

        final String source = "public class A {protected int x; public A(int y) {x=y;} public A() {x = 0; A a = new A(); a = new A(x);} public static class InnerA extends A { } }";

        final ICompilationUnit compilationUnitS = createCompilationUnit("", "A.java", source);

        final PMCompilationUnit pmCompilationUnitS = getProject().getPMCompilationUnit(compilationUnitS);

        final TypeDeclaration type = findClassByName("A", pmCompilationUnitS);
        final MethodDeclaration methodDeclaration = findMethodByName("A", type);

        final SimpleName methodName = methodDeclaration.getName();

        final RenameStep renameStep = new RenameStep(getProject(), methodName);

        renameStep.setNewName("x__5");

        renameStep.apply();

        final String expectedNewSource = "public class x__5 {protected int x; public x__5(int y) {x=y;} public x__5() {x = 0; x__5 a = new x__5(); a = new x__5(x);} public static class InnerA extends x__5 { } }";

        assertThat(pmCompilationUnitS, hasPMSource(expectedNewSource));

    }

    @Test
    public void testRenameConstructorFollowedByRenameClass() {

        final String source = "class S {S() {}}";

        final ICompilationUnit compilationUnitS = createCompilationUnit("", "S.java", source);

        final PMCompilationUnit pmCompilationUnitS = getProject().getPMCompilationUnit(compilationUnitS);

        final TypeDeclaration type = findClassByName("S", pmCompilationUnitS);
        final MethodDeclaration methodDeclaration = findMethodByName("S", type);

        final SimpleName methodName = methodDeclaration.getName();

        final RenameStep renameConstructorStep = new RenameStep(getProject(), methodName);

        renameConstructorStep.setNewName("T");

        renameConstructorStep.apply();

        final String expectedNewSourceAfterRenameConstructor = "class T {T() {}}";

        assertThat(pmCompilationUnitS, hasPMSource(expectedNewSourceAfterRenameConstructor));

        final TypeDeclaration classNode = ASTQuery.findClassByName("T", pmCompilationUnitS);

        final SimpleName className = classNode.getName();

        final RenameStep renameClassStep = new RenameStep(getProject(), className);

        renameClassStep.setNewName("S");

        renameClassStep.apply();

        final String expectedNewSourceAfterRenameClass = "class S {S() {}}";

        assertThat(pmCompilationUnitS, hasPMSource(expectedNewSourceAfterRenameClass));

    }

    @Test
    public void testRenameConstructorInClassInPackageWithSubclass() {

        final String sourceA = "package testpackage; public class A {public A() {System.out.println(6);}}";

        final ICompilationUnit compilationUnitA = createCompilationUnit("testpackage", "A.java", sourceA);

        final String sourceB = "public class B extends testpackage.A {}";

        final ICompilationUnit compilationUnitB = createCompilationUnit("", "B.java", sourceB);

        final PMCompilationUnit pmCompilationUnitA = getProject().getPMCompilationUnit(compilationUnitA);

        final PMCompilationUnit pmCompilationUnitB = getProject().getPMCompilationUnit(compilationUnitB);

        final TypeDeclaration type = findClassByName("A", pmCompilationUnitA);
        final MethodDeclaration methodDeclaration = findMethodByName("A", type);

        final SimpleName methodName = methodDeclaration.getName();

        final RenameStep renameStep = new RenameStep(getProject(), methodName);

        renameStep.setNewName("x__5");

        renameStep.apply();

        assertThat(pmCompilationUnitA,
                hasPMSource("package testpackage; public class x__5 {public x__5() {System.out.println(6);}}"));
        assertThat(pmCompilationUnitB, hasPMSource("public class B extends testpackage.x__5 {}"));
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

        final PMCompilationUnit pmCompilationUnitA = getProject().getPMCompilationUnit(compilationUnitA);
        final PMCompilationUnit pmCompilationUnitB = getProject().getPMCompilationUnit(compilationUnitB);

        final TypeDeclaration type = findClassByName("A", pmCompilationUnitA);
        final MethodDeclaration methodDeclaration = findMethodByName("A", type);

        final SimpleName methodName = methodDeclaration.getName();

        final RenameStep renameStep = new RenameStep(getProject(), methodName);

        renameStep.setNewName("x__5");

        renameStep.apply();

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

        assertThat(pmCompilationUnitA, hasPMSource(expectedTransformedSourceA));
        assertThat(pmCompilationUnitB, hasPMSource(expectedTransformedSourceB));
    }

    @Test
    public void testNameCaptureInSubclass() {
        final String sourceSuper = "public class Super { public void f(){} }";
        final String sourceSub = "public class Sub extends Super { public void g(){} public static void main(String... args){new Sub().f();}}";

        createCompilationUnit("", "A.java", sourceSuper);
        final ICompilationUnit compilationUnitSub = createCompilationUnit("", "B.java", sourceSub);

        final PMCompilationUnit pmCompilationUnitSub = getProject().getPMCompilationUnit(compilationUnitSub);

        final TypeDeclaration type = findClassByName("Sub", pmCompilationUnitSub);
        final MethodDeclaration methodDeclaration = findMethodByName("g", type);
        final SimpleName methodName = methodDeclaration.getName();

        final RenameStep renameStep = new RenameStep(getProject(), methodName);
        renameStep.setNewName("f");
        renameStep.apply();

        ConsistencyValidator.getInstance().rescanForInconsistencies(getProject());
        Collection<Inconsistency> inconsistencies = ConsistencyValidator.getInstance().getInconsistencies();

        assertThat("Unexpected inconsistency list:" + inconsistencies, inconsistencies.size(), is(1));
        Inconsistency inconsistency = inconsistencies.iterator().next();
        assertTrue(inconsistency instanceof NameCapture);
    }

    @Test
    public void testRenameLocalToShadowField() throws JavaModelException {
        final String sourceS = "public class S {" + "String iVar;" + "void m() {" + "String lVar;" + "iVar.length();"
                + "}" + "}";

        final ICompilationUnit compilationUnitS = createCompilationUnit("", "S.java", sourceS);

        final PMCompilationUnit pmCompilationUnitS = getProject().getPMCompilationUnit(compilationUnitS);

        final SimpleName name = findSimpleName("lVar", pmCompilationUnitS);

        final RenameStep step = new RenameStep(getProject(), name);

        step.setNewName("iVar");

        step.apply();

        final String expectedNewSourceS = "public class S {" + "String iVar;" + "void m() {" + "String iVar;"
                + "iVar.length();" + "}" + "}";

        assertThat(compilationUnitS, hasSource(expectedNewSourceS));

        final Collection<Inconsistency> inconsistencies = ConsistencyValidator.getInstance().getInconsistencies();

        // Inconsistencies are:
        // [Definition (null) should be used by iVar,
        // iVar was captured.,
        // Unexpected definition (iVar) used by iVar]

        assertEquals("Unexpected inconsistency list:" + inconsistencies, 3, inconsistencies.size());

    }

    // Should also test via static class name like Foo.doSomething() and via
    // this expression (Foo.this) and qualified name (Foo.something) in inner
    // class to get to outer class
}
