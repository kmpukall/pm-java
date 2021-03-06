/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm.steps;

import static net.creichen.pm.tests.Matchers.hasSource;
import static net.creichen.pm.utils.ASTQuery.findClassByName;
import static net.creichen.pm.utils.ASTQuery.findFieldByName;
import static net.creichen.pm.utils.ASTQuery.findMethodByName;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.List;

import net.creichen.pm.api.PMCompilationUnit;
import net.creichen.pm.tests.PMTest;
import net.creichen.pm.utils.ASTQuery;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.junit.Test;

public class PasteStepTest extends PMTest {

    @Test
    public void testCutPasteField() throws JavaModelException {

        final String source1 = "public class S1 {S1 s; void m1(){System.out.println(s);}}";
        final String source2 = "public class S2 {void a(){} void b(){} }";

        final ICompilationUnit compilationUnit1 = createCompilationUnit("", "S1.java", source1);
        final ICompilationUnit compilationUnit2 = createCompilationUnit("", "S2.java", source2);

        final TypeDeclaration type = findClassByName("S1", getProject().getPMCompilationUnit(compilationUnit1));
        final FieldDeclaration fieldDeclaration = findFieldByName("s", type);

        final CutStep cutStep = new CutStep(getProject(), fieldDeclaration);

        cutStep.apply();

        final TypeDeclaration classDeclaration = ASTQuery.findClassByName("S2",
                getProject().getPMCompilationUnit(compilationUnit2));

        final PasteStep pasteStep = new PasteStep(getProject(), classDeclaration,
                TypeDeclaration.BODY_DECLARATIONS_PROPERTY, 1);

        pasteStep.apply();

        assertThat(compilationUnit1, hasSource("public class S1 {void m1(){System.out.println(s);}}"));
        assertThat(compilationUnit2, hasSource("public class S2 {void a(){} S1 s; void b(){} }"));
    }

    @Test
    public void testCutPasteMethod() throws JavaModelException {

        final String source1 = "public class S1 {S1 s; void m(){System.out.println(s);}}";
        final String source2 = "public class S2 {String a; String b;";

        final ICompilationUnit compilationUnit1 = createCompilationUnit("", "S1.java", source1);
        final ICompilationUnit compilationUnit2 = createCompilationUnit("", "S2.java", source2);

        final TypeDeclaration type = findClassByName("S1", getProject().getPMCompilationUnit(compilationUnit1));
        final MethodDeclaration methodDeclaration = ASTQuery.findMethodByName("m", type);

        new CutStep(getProject(), methodDeclaration).apply();

        final TypeDeclaration classDeclaration = ASTQuery.findClassByName("S2",
                getProject().getPMCompilationUnit(compilationUnit2));

        final PasteStep pasteStep = new PasteStep(getProject(), classDeclaration,
                TypeDeclaration.BODY_DECLARATIONS_PROPERTY, 1);

        pasteStep.apply();

        assertThat(compilationUnit1, hasSource("public class S1 {S1 s;}"));
        assertThat(compilationUnit2,
                hasSource("public class S2 {String a;  void m(){System.out.println(s);} String b;"));
    }

    @Test
    public void testCutPasteStatement() throws JavaModelException {

        final String source1 = "public class S1 {S1 s; void m(){System.out.println(s);}}";
        final String source2 = "public class S2 {void a(){System.out.println(1); System.out.println(2);}}";

        final ICompilationUnit compilationUnit1 = createCompilationUnit("", "S1.java", source1);
        final ICompilationUnit compilationUnit2 = createCompilationUnit("", "S2.java", source2);

        final TypeDeclaration type1 = findClassByName("S1", getProject().getPMCompilationUnit(compilationUnit1));
        final MethodDeclaration sourceMethodDeclaration = findMethodByName("m", type1);

        final Statement firstStatement = (Statement) sourceMethodDeclaration.getBody().statements().get(0);

        final CutStep cutStep = new CutStep(getProject(), firstStatement);

        cutStep.apply();

        final TypeDeclaration type2 = findClassByName("S2", getProject().getPMCompilationUnit(compilationUnit2));
        final MethodDeclaration targetMethodDeclaration = findMethodByName("a", type2);

        final Block targetBlock = targetMethodDeclaration.getBody();

        final PasteStep pasteStep = new PasteStep(getProject(), targetBlock, Block.STATEMENTS_PROPERTY, 1);

        pasteStep.apply();

        assertThat(compilationUnit1, hasSource("public class S1 {S1 s; void m(){}}"));
        assertThat(
                compilationUnit2,
                hasSource("public class S2 {void a(){System.out.println(1);System.out.println(s); System.out.println(2);}}"));
    }

    @Test
    public void testCutPasteStatements() throws JavaModelException {
        final String source = "public class S {void m(){int x,y; int a; a = 1; y = 3; x = 2;}}";

        final ICompilationUnit iCompilationUnit = createCompilationUnit("", "S.java", source);

        PMCompilationUnit compilationUnit = getProject().getPMCompilationUnit(iCompilationUnit);

        TypeDeclaration type = findClassByName("S", compilationUnit);
        MethodDeclaration methodDeclaration = findMethodByName("m", type);

        final Statement thirdStatement = (Statement) methodDeclaration.getBody().statements().get(2);
        final Statement fourthStatement = (Statement) methodDeclaration.getBody().statements().get(3);

        final List<ASTNode> nodesToCut = new ArrayList<ASTNode>();
        nodesToCut.add(thirdStatement);
        nodesToCut.add(fourthStatement);

        final CutStep cutStep = new CutStep(getProject(), nodesToCut);

        cutStep.apply();

        assertThat(iCompilationUnit, hasSource("public class S {void m(){int x,y; int a; x = 2;}}"));

        // have to get new ASTNodes b/c of reparsing
        compilationUnit = getProject().getPMCompilationUnit(iCompilationUnit);

        type = findClassByName("S", compilationUnit);
        methodDeclaration = findMethodByName("m", type);

        final PasteStep pasteStep = new PasteStep(getProject(), methodDeclaration.getBody(), Block.STATEMENTS_PROPERTY,
                2);

        pasteStep.apply();

        assertThat(iCompilationUnit, hasSource("public class S {void m(){int x,y; int a; a = 1; y = 3; x = 2;}}"));

    }

    @Test
    public void testPullupFieldToDifferentPackageWithStaticMethodInitializer() throws JavaModelException {

        final String sourceS = "package A; public class S extends A.T {String string = foo(); void m(){System.out.println(string);} private static String foo() {return \"foo\";} }";
        final String sourceT = "package B; public class T {}";

        final ICompilationUnit compilationUnitS = createCompilationUnit("A", "S.java", sourceS);
        final ICompilationUnit compilationUnitT = createCompilationUnit("B", "T.java", sourceT);

        final TypeDeclaration type = findClassByName("S", getProject().getPMCompilationUnit(compilationUnitS));
        final FieldDeclaration fieldDeclaration = findFieldByName("string", type);

        new CutStep(getProject(), fieldDeclaration).apply();

        final TypeDeclaration targetDeclaration = findClassByName("T",
                getProject().getPMCompilationUnit(compilationUnitT));

        final PasteStep pasteStep = new PasteStep(getProject(), targetDeclaration,
                TypeDeclaration.BODY_DECLARATIONS_PROPERTY, 0);

        pasteStep.apply();

        assertThat(
                compilationUnitS,
                hasSource("package A; public class S extends A.T {void m(){System.out.println(string);} private static String foo() {return \"foo\";} }"));
        assertThat(compilationUnitT, hasSource("package B; public class T {String string = foo(); }"));
    }

    @Test
    public void testPullupFieldViaCutAndPaste() throws JavaModelException {

        final String sourceS = "public class S extends T {String string; void m(){System.out.println(string);}}";
        final String sourceT = "public class T {}";

        final ICompilationUnit compilationUnitS = createCompilationUnit("", "S.java", sourceS);
        final ICompilationUnit compilationUnitT = createCompilationUnit("", "T.java", sourceT);

        final TypeDeclaration type = findClassByName("S", getProject().getPMCompilationUnit(compilationUnitS));
        final FieldDeclaration fieldDeclaration = findFieldByName("string", type);

        new CutStep(getProject(), fieldDeclaration).apply();

        final TypeDeclaration targetDeclaration = findClassByName("T",
                getProject().getPMCompilationUnit(compilationUnitT));

        final PasteStep pasteStep = new PasteStep(getProject(), targetDeclaration,
                TypeDeclaration.BODY_DECLARATIONS_PROPERTY, 0);

        pasteStep.apply();

        final String expectedNewSourceS = "public class S extends T {void m(){System.out.println(string);}}";
        final String expectedNewSourceT = "public class T {String string;}";

        assertThat(compilationUnitS, hasSource(expectedNewSourceS));
        assertThat(compilationUnitT, hasSource(expectedNewSourceT));
    }

    @Test
    public void testPullupFieldViaWithConstantInitializer() throws JavaModelException {
        final String sourceS = "public class S extends T {String string = \"Bar\"; void m(){System.out.println(string);}}";
        final String sourceT = "public class T {}";

        final ICompilationUnit compilationUnitS = createCompilationUnit("", "S.java", sourceS);
        final ICompilationUnit compilationUnitT = createCompilationUnit("", "T.java", sourceT);

        final TypeDeclaration type = findClassByName("S", getProject().getPMCompilationUnit(compilationUnitS));
        final FieldDeclaration fieldDeclaration = findFieldByName("string", type);

        new CutStep(getProject(), fieldDeclaration).apply();

        final TypeDeclaration targetDeclaration = findClassByName("T",
                getProject().getPMCompilationUnit(compilationUnitT));

        final PasteStep pasteStep = new PasteStep(getProject(), targetDeclaration,
                TypeDeclaration.BODY_DECLARATIONS_PROPERTY, 0);

        pasteStep.apply();

        assertThat(compilationUnitS, hasSource("public class S extends T {void m(){System.out.println(string);}}"));
        assertThat(compilationUnitT, hasSource("public class T {String string = \"Bar\";}"));
    }

    @Test
    public void testPullupFieldViaWithStaticMethodInitializer() throws JavaModelException {

        final String sourceS = "public class S extends T {String string = foo(); void m(){System.out.println(string);} private static String foo() {return \"foo\";} }";
        final String sourceT = "public class T {}";

        final ICompilationUnit compilationUnitS = createCompilationUnit("", "S.java", sourceS);
        final ICompilationUnit compilationUnitT = createCompilationUnit("", "T.java", sourceT);

        final TypeDeclaration type = findClassByName("S", getProject().getPMCompilationUnit(compilationUnitS));
        final FieldDeclaration fieldDeclaration = findFieldByName("string", type);

        new CutStep(getProject(), fieldDeclaration).apply();

        final TypeDeclaration targetDeclaration = findClassByName("T",
                getProject().getPMCompilationUnit(compilationUnitT));

        final PasteStep pasteStep = new PasteStep(getProject(), targetDeclaration,
                TypeDeclaration.BODY_DECLARATIONS_PROPERTY, 0);
        pasteStep.apply();

        assertThat(
                compilationUnitS,
                hasSource("public class S extends T { void m(){System.out.println(string);} private static String foo() {return \"foo\";} }"));
        assertThat(compilationUnitT, hasSource("public class T {String string = foo();}"));
    }

    @Test
    public void testPullupStaticFieldWithStaticMethodInitializer() throws JavaModelException {

        final String sourceS = "public class S extends T {static String string = foo(); void m(){System.out.println(string);} private static String foo() {return \"foo\";} }";
        final String sourceT = "public class T {}";

        final ICompilationUnit compilationUnitS = createCompilationUnit("", "S.java", sourceS);
        final ICompilationUnit compilationUnitT = createCompilationUnit("", "T.java", sourceT);

        final TypeDeclaration type = findClassByName("S", getProject().getPMCompilationUnit(compilationUnitS));
        final FieldDeclaration fieldDeclaration = findFieldByName("string", type);

        new CutStep(getProject(), fieldDeclaration).apply();

        final TypeDeclaration targetDeclaration = findClassByName("T",
                getProject().getPMCompilationUnit(compilationUnitT));

        final PasteStep pasteStep = new PasteStep(getProject(), targetDeclaration,
                TypeDeclaration.BODY_DECLARATIONS_PROPERTY, 0);

        pasteStep.apply();

        assertThat(
                compilationUnitS,
                hasSource("public class S extends T { void m(){System.out.println(string);} private static String foo() {return \"foo\";} }"));
        assertThat(compilationUnitT, hasSource("public class T {static String string = foo();}"));
    }

    @Test
    public void testPullupStaticMethodWithStaticFieldInitializerReferencingIt() throws JavaModelException {

        final String sourceS = "public class S extends T {static String string = foo(); void m(){System.out.println(string);} private static String foo() {return \"foo\";} }";
        final String sourceT = "public class T {}";

        final ICompilationUnit compilationUnitS = createCompilationUnit("", "S.java", sourceS);
        final ICompilationUnit compilationUnitT = createCompilationUnit("", "T.java", sourceT);

        final TypeDeclaration type = findClassByName("S", getProject().getPMCompilationUnit(compilationUnitS));
        final MethodDeclaration methodDeclaration = findMethodByName("foo", type);

        final CutStep cutStep = new CutStep(getProject(), methodDeclaration);

        cutStep.apply();

        final TypeDeclaration targetDeclaration = findClassByName("T",
                getProject().getPMCompilationUnit(compilationUnitT));

        final PasteStep pasteStep = new PasteStep(getProject(), targetDeclaration,
                TypeDeclaration.BODY_DECLARATIONS_PROPERTY, 0);
        pasteStep.apply();

        assertThat(
                compilationUnitS,
                hasSource("public class S extends T {static String string = foo(); void m(){System.out.println(string);}  }"));
        assertThat(compilationUnitT, hasSource("public class T { private static String foo() {return \"foo\";}}"));
    }

}
