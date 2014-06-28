/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm.steps;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import net.creichen.pm.ASTQuery;
import net.creichen.pm.PMProject;
import net.creichen.pm.PMWorkspace;
import net.creichen.pm.tests.PMTest;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.*;
import org.junit.Test;

public class PasteStepTest extends PMTest {

    @Test
    public void testCutPasteField() throws JavaModelException {

        final String source1 = "public class S1 {S1 s; void m1(){System.out.println(s);}}";
        final String source2 = "public class S2 {void a(){} void b(){} }";

        final ICompilationUnit compilationUnit1 = createNewCompilationUnit("", "S1.java", source1);
        final ICompilationUnit compilationUnit2 = createNewCompilationUnit("", "S2.java", source2);

        final PMProject pmProject = PMWorkspace.sharedWorkspace().projectForIJavaProject(
                this.getIJavaProject());

        final VariableDeclarationFragment fieldDeclarationFragment = ASTQuery
                .fieldWithNameInClassInCompilationUnit("s", 0, "S1", 0, (CompilationUnit) pmProject
                        .findASTRootForICompilationUnit(compilationUnit1));

        final FieldDeclaration fieldDeclaration = (FieldDeclaration) fieldDeclarationFragment
                .getParent();

        final CutStep cutStep = new CutStep(pmProject, fieldDeclaration);

        cutStep.applyAllAtOnce();

        final TypeDeclaration classDeclaration = ASTQuery.classWithNameInCompilationUnit("S2", 0,
                (CompilationUnit) pmProject.findASTRootForICompilationUnit(compilationUnit2));

        final PasteStep pasteStep = new PasteStep(pmProject, classDeclaration,
                TypeDeclaration.BODY_DECLARATIONS_PROPERTY, 1);

        pasteStep.applyAllAtOnce();

        assertTrue(compilationUnitSourceMatchesSource(
                "public class S1 {void m1(){System.out.println(s);}}", compilationUnit1.getSource()));
        assertTrue(compilationUnitSourceMatchesSource(
                "public class S2 {void a(){} S1 s; void b(){} }", compilationUnit2.getSource()));
    }

    @Test
    public void testCutPasteMethod() throws JavaModelException {

        final String source1 = "public class S1 {S1 s; void m(){System.out.println(s);}}";
        final String source2 = "public class S2 {String a; String b;";

        final ICompilationUnit compilationUnit1 = createNewCompilationUnit("", "S1.java", source1);
        final ICompilationUnit compilationUnit2 = createNewCompilationUnit("", "S2.java", source2);

        final PMProject pmProject = PMWorkspace.sharedWorkspace().projectForIJavaProject(
                this.getIJavaProject());

        final MethodDeclaration methodDeclaration = ASTQuery
                .methodWithNameInClassInCompilationUnit("m", 0, "S1", 0,
                        (CompilationUnit) pmProject
                                .findASTRootForICompilationUnit(compilationUnit1));

        final CutStep cutStep = new CutStep(pmProject, methodDeclaration);

        cutStep.applyAllAtOnce();

        final TypeDeclaration classDeclaration = ASTQuery.classWithNameInCompilationUnit("S2", 0,
                (CompilationUnit) pmProject.findASTRootForICompilationUnit(compilationUnit2));

        final PasteStep pasteStep = new PasteStep(pmProject, classDeclaration,
                TypeDeclaration.BODY_DECLARATIONS_PROPERTY, 1);

        pasteStep.applyAllAtOnce();

        assertTrue(compilationUnitSourceMatchesSource("public class S1 {S1 s;}",
                compilationUnit1.getSource()));
        assertTrue(compilationUnitSourceMatchesSource(
                "public class S2 {String a;  void m(){System.out.println(s);} String b;",
                compilationUnit2.getSource()));
    }

    @Test
    public void testCutPasteStatement() throws JavaModelException {

        final String source1 = "public class S1 {S1 s; void m(){System.out.println(s);}}";
        final String source2 = "public class S2 {void a(){System.out.println(1); System.out.println(2);}}";

        final ICompilationUnit compilationUnit1 = createNewCompilationUnit("", "S1.java", source1);
        final ICompilationUnit compilationUnit2 = createNewCompilationUnit("", "S2.java", source2);

        final PMProject pmProject = PMWorkspace.sharedWorkspace().projectForIJavaProject(
                this.getIJavaProject());

        final MethodDeclaration sourceMethodDeclaration = ASTQuery
                .methodWithNameInClassInCompilationUnit("m", 0, "S1", 0,
                        (CompilationUnit) pmProject
                                .findASTRootForICompilationUnit(compilationUnit1));

        final Statement firstStatement = (Statement) sourceMethodDeclaration.getBody().statements()
                .get(0);

        final CutStep cutStep = new CutStep(pmProject, firstStatement);

        cutStep.applyAllAtOnce();

        final MethodDeclaration targetMethodDeclaration = ASTQuery
                .methodWithNameInClassInCompilationUnit("a", 0, "S2", 0,
                        (CompilationUnit) pmProject
                                .findASTRootForICompilationUnit(compilationUnit2));

        final Block targetBlock = targetMethodDeclaration.getBody();

        final PasteStep pasteStep = new PasteStep(pmProject, targetBlock,
                Block.STATEMENTS_PROPERTY, 1);

        pasteStep.applyAllAtOnce();

        assertTrue(compilationUnitSourceMatchesSource("public class S1 {S1 s; void m(){}}",
                compilationUnit1.getSource()));
        assertTrue(compilationUnitSourceMatchesSource(
                "public class S2 {void a(){System.out.println(1);System.out.println(s); System.out.println(2);}}",
                compilationUnit2.getSource()));
    }

    @Test
    public void testCutPasteStatements() throws JavaModelException {
        final String source = "public class S {void m(){int x,y; int a; a = 1; y = 3; x = 2;}}";

        final ICompilationUnit iCompilationUnit = createNewCompilationUnit("", "S.java", source);

        final PMProject pmProject = PMWorkspace.sharedWorkspace().projectForIJavaProject(
                this.getIJavaProject());

        CompilationUnit compilationUnit = (CompilationUnit) pmProject
                .findASTRootForICompilationUnit(iCompilationUnit);

        MethodDeclaration methodDeclaration = ASTQuery.methodWithNameInClassInCompilationUnit(
                "m", 0, "S", 0, compilationUnit);

        final Statement thirdStatement = (Statement) methodDeclaration.getBody().statements()
                .get(2);
        final Statement fourthStatement = (Statement) methodDeclaration.getBody().statements()
                .get(3);

        final List<ASTNode> nodesToCut = new ArrayList<ASTNode>();
        nodesToCut.add(thirdStatement);
        nodesToCut.add(fourthStatement);

        final CutStep cutStep = new CutStep(pmProject, nodesToCut);

        cutStep.applyAllAtOnce();

        assertTrue(compilationUnitSourceMatchesSource(
                "public class S {void m(){int x,y; int a; x = 2;}}", iCompilationUnit.getSource()));

        // have to get new ASTNodes b/c of reparsing
        compilationUnit = (CompilationUnit) pmProject
                .findASTRootForICompilationUnit(iCompilationUnit);

        methodDeclaration = ASTQuery.methodWithNameInClassInCompilationUnit("m", 0, "S", 0,
                compilationUnit);

        final PasteStep pasteStep = new PasteStep(pmProject, methodDeclaration.getBody(),
                Block.STATEMENTS_PROPERTY, 2);

        pasteStep.applyAllAtOnce();

        assertTrue(compilationUnitSourceMatchesSource(
                "public class S {void m(){int x,y; int a; a = 1; y = 3; x = 2;}}",
                iCompilationUnit.getSource()));

    }

    @Test
    public void testPullupFieldToDifferentPackageWithStaticMethodInitializer()
            throws JavaModelException {

        final String sourceS = "package A; public class S extends A.T {String string = foo(); void m(){System.out.println(string);} private static String foo() {return \"foo\";} }";
        final String sourceT = "package B; public class T {}";

        final ICompilationUnit compilationUnitS = createNewCompilationUnit("A", "S.java", sourceS);
        final ICompilationUnit compilationUnitT = createNewCompilationUnit("B", "T.java", sourceT);

        final PMProject pmProject = PMWorkspace.sharedWorkspace().projectForIJavaProject(
                this.getIJavaProject());

        final FieldDeclaration fieldDeclaration = (FieldDeclaration) ASTQuery
                .fieldWithNameInClassInCompilationUnit(
                        "string",
                        0,
                        "S",
                        0,
                        (CompilationUnit) pmProject
                                .findASTRootForICompilationUnit(compilationUnitS)).getParent();

        final CutStep cutStep = new CutStep(pmProject, fieldDeclaration);

        cutStep.applyAllAtOnce();

        final TypeDeclaration targetDeclaration = ASTQuery.classWithNameInCompilationUnit("T", 0,
                (CompilationUnit) pmProject.findASTRootForICompilationUnit(compilationUnitT));

        final PasteStep pasteStep = new PasteStep(pmProject, targetDeclaration,
                TypeDeclaration.BODY_DECLARATIONS_PROPERTY, 0);

        pasteStep.applyAllAtOnce();

        final String expectedNewSourceS = "package A; public class S extends A.T {void m(){System.out.println(string);} private static String foo() {return \"foo\";} }";
        final String expectedNewSourceT = "package B; public class T {String string = foo(); }";

        assertTrue(compilationUnitSourceMatchesSource(expectedNewSourceS,
                compilationUnitS.getSource()));
        assertTrue(compilationUnitSourceMatchesSource(expectedNewSourceT,
                compilationUnitT.getSource()));
    }

    @Test
    public void testPullupFieldViaCutAndPaste() throws JavaModelException {

        final String sourceS = "public class S extends T {String string; void m(){System.out.println(string);}}";
        final String sourceT = "public class T {}";

        final ICompilationUnit compilationUnitS = createNewCompilationUnit("", "S.java", sourceS);
        final ICompilationUnit compilationUnitT = createNewCompilationUnit("", "T.java", sourceT);

        final PMProject pmProject = PMWorkspace.sharedWorkspace().projectForIJavaProject(
                this.getIJavaProject());

        final VariableDeclarationFragment fieldDeclarationFragment = ASTQuery
                .fieldWithNameInClassInCompilationUnit("string", 0, "S", 0,
                        (CompilationUnit) pmProject
                                .findASTRootForICompilationUnit(compilationUnitS));

        final FieldDeclaration fieldDeclaration = (FieldDeclaration) fieldDeclarationFragment
                .getParent();

        final CutStep cutStep = new CutStep(pmProject, fieldDeclaration);

        cutStep.applyAllAtOnce();

        final TypeDeclaration targetDeclaration = ASTQuery.classWithNameInCompilationUnit("T", 0,
                (CompilationUnit) pmProject.findASTRootForICompilationUnit(compilationUnitT));

        final PasteStep pasteStep = new PasteStep(pmProject, targetDeclaration,
                TypeDeclaration.BODY_DECLARATIONS_PROPERTY, 0);

        pasteStep.applyAllAtOnce();

        final String expectedNewSourceS = "public class S extends T {void m(){System.out.println(string);}}";
        final String expectedNewSourceT = "public class T {String string;}";

        assertTrue(compilationUnitSourceMatchesSource(expectedNewSourceS,
                compilationUnitS.getSource()));
        assertTrue(compilationUnitSourceMatchesSource(expectedNewSourceT,
                compilationUnitT.getSource()));
    }

    @Test
    public void testPullupFieldViaWithConstantInitializer() throws JavaModelException {

        final String sourceS = "public class S extends T {String string = \"Bar\"; void m(){System.out.println(string);}}";
        final String sourceT = "public class T {}";

        final ICompilationUnit compilationUnitS = createNewCompilationUnit("", "S.java", sourceS);
        final ICompilationUnit compilationUnitT = createNewCompilationUnit("", "T.java", sourceT);

        final PMProject pmProject = PMWorkspace.sharedWorkspace().projectForIJavaProject(
                this.getIJavaProject());

        final FieldDeclaration fieldDeclaration = (FieldDeclaration) ASTQuery
                .fieldWithNameInClassInCompilationUnit(
                        "string",
                        0,
                        "S",
                        0,
                        (CompilationUnit) pmProject
                                .findASTRootForICompilationUnit(compilationUnitS)).getParent();

        final CutStep cutStep = new CutStep(pmProject, fieldDeclaration);

        cutStep.applyAllAtOnce();

        final TypeDeclaration targetDeclaration = ASTQuery.classWithNameInCompilationUnit("T", 0,
                (CompilationUnit) pmProject.findASTRootForICompilationUnit(compilationUnitT));

        final PasteStep pasteStep = new PasteStep(pmProject, targetDeclaration,
                TypeDeclaration.BODY_DECLARATIONS_PROPERTY, 0);

        pasteStep.applyAllAtOnce();

        final String expectedNewSourceS = "public class S extends T {void m(){System.out.println(string);}}";
        final String expectedNewSourceT = "public class T {String string = \"Bar\";}";

        assertTrue(compilationUnitSourceMatchesSource(expectedNewSourceS,
                compilationUnitS.getSource()));
        assertTrue(compilationUnitSourceMatchesSource(expectedNewSourceT,
                compilationUnitT.getSource()));
    }

    @Test
    public void testPullupFieldViaWithStaticMethodInitializer() throws JavaModelException {

        final String sourceS = "public class S extends T {String string = foo(); void m(){System.out.println(string);} private static String foo() {return \"foo\";} }";
        final String sourceT = "public class T {}";

        final ICompilationUnit compilationUnitS = createNewCompilationUnit("", "S.java", sourceS);
        final ICompilationUnit compilationUnitT = createNewCompilationUnit("", "T.java", sourceT);

        final PMProject pmProject = PMWorkspace.sharedWorkspace().projectForIJavaProject(
                this.getIJavaProject());

        final FieldDeclaration fieldDeclaration = (FieldDeclaration) ASTQuery
                .fieldWithNameInClassInCompilationUnit(
                        "string",
                        0,
                        "S",
                        0,
                        (CompilationUnit) pmProject
                                .findASTRootForICompilationUnit(compilationUnitS)).getParent();

        final CutStep cutStep = new CutStep(pmProject, fieldDeclaration);

        cutStep.applyAllAtOnce();

        final TypeDeclaration targetDeclaration = ASTQuery.classWithNameInCompilationUnit("T", 0,
                (CompilationUnit) pmProject.findASTRootForICompilationUnit(compilationUnitT));

        final PasteStep pasteStep = new PasteStep(pmProject, targetDeclaration,
                TypeDeclaration.BODY_DECLARATIONS_PROPERTY, 0);

        pasteStep.applyAllAtOnce();

        final String expectedNewSourceS = "public class S extends T { void m(){System.out.println(string);} private static String foo() {return \"foo\";} }";
        final String expectedNewSourceT = "public class T {String string = foo();}";

        assertTrue(compilationUnitSourceMatchesSource(expectedNewSourceS,
                compilationUnitS.getSource()));
        assertTrue(compilationUnitSourceMatchesSource(expectedNewSourceT,
                compilationUnitT.getSource()));
    }

    @Test
    public void testPullupStaticFieldWithStaticMethodInitializer() throws JavaModelException {

        final String sourceS = "public class S extends T {static String string = foo(); void m(){System.out.println(string);} private static String foo() {return \"foo\";} }";
        final String sourceT = "public class T {}";

        final ICompilationUnit compilationUnitS = createNewCompilationUnit("", "S.java", sourceS);
        final ICompilationUnit compilationUnitT = createNewCompilationUnit("", "T.java", sourceT);

        final PMProject pmProject = PMWorkspace.sharedWorkspace().projectForIJavaProject(
                this.getIJavaProject());

        final FieldDeclaration fieldDeclaration = (FieldDeclaration) ASTQuery
                .fieldWithNameInClassInCompilationUnit(
                        "string",
                        0,
                        "S",
                        0,
                        (CompilationUnit) pmProject
                                .findASTRootForICompilationUnit(compilationUnitS)).getParent();

        final CutStep cutStep = new CutStep(pmProject, fieldDeclaration);

        cutStep.applyAllAtOnce();

        final TypeDeclaration targetDeclaration = ASTQuery.classWithNameInCompilationUnit("T", 0,
                (CompilationUnit) pmProject.findASTRootForICompilationUnit(compilationUnitT));

        final PasteStep pasteStep = new PasteStep(pmProject, targetDeclaration,
                TypeDeclaration.BODY_DECLARATIONS_PROPERTY, 0);

        pasteStep.applyAllAtOnce();

        final String expectedNewSourceS = "public class S extends T { void m(){System.out.println(string);} private static String foo() {return \"foo\";} }";
        final String expectedNewSourceT = "public class T {static String string = foo();}";

        assertTrue(compilationUnitSourceMatchesSource(expectedNewSourceS,
                compilationUnitS.getSource()));
        assertTrue(compilationUnitSourceMatchesSource(expectedNewSourceT,
                compilationUnitT.getSource()));
    }

    @Test
    public void testPullupStaticMethodWithStaticFieldInitializerReferencingIt()
            throws JavaModelException {

        final String sourceS = "public class S extends T {static String string = foo(); void m(){System.out.println(string);} private static String foo() {return \"foo\";} }";
        final String sourceT = "public class T {}";

        final ICompilationUnit compilationUnitS = createNewCompilationUnit("", "S.java", sourceS);
        final ICompilationUnit compilationUnitT = createNewCompilationUnit("", "T.java", sourceT);

        final PMProject pmProject = PMWorkspace.sharedWorkspace().projectForIJavaProject(
                this.getIJavaProject());

        final MethodDeclaration methodDeclaration = ASTQuery
                .methodWithNameInClassInCompilationUnit("foo", 0, "S", 0,
                        (CompilationUnit) pmProject
                                .findASTRootForICompilationUnit(compilationUnitS));

        final CutStep cutStep = new CutStep(pmProject, methodDeclaration);

        cutStep.applyAllAtOnce();

        final TypeDeclaration targetDeclaration = ASTQuery.classWithNameInCompilationUnit("T", 0,
                (CompilationUnit) pmProject.findASTRootForICompilationUnit(compilationUnitT));

        final PasteStep pasteStep = new PasteStep(pmProject, targetDeclaration,
                TypeDeclaration.BODY_DECLARATIONS_PROPERTY, 0);

        pasteStep.applyAllAtOnce();

        final String expectedNewSourceS = "public class S extends T {static String string = foo(); void m(){System.out.println(string);}  }";
        final String expectedNewSourceT = "public class T { private static String foo() {return \"foo\";}}";

        assertTrue(compilationUnitSourceMatchesSource(expectedNewSourceS,
                compilationUnitS.getSource()));
        assertTrue(compilationUnitSourceMatchesSource(expectedNewSourceT,
                compilationUnitT.getSource()));
    }

}
