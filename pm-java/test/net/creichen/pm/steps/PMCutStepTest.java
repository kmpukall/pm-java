/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm.steps;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import net.creichen.pm.PMASTQuery;
import net.creichen.pm.PMProject;
import net.creichen.pm.PMWorkspace;
import net.creichen.pm.tests.PMTest;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.junit.Test;

public class PMCutStepTest extends PMTest {

    @Test
    public void testInstantiation() {

        String source = "public class S {S s; void m(){s.getClass(); m();}}";

        ICompilationUnit compilationUnit = createNewCompilationUnit("", "S.java", source);

        PMProject pmProject = PMWorkspace.sharedWorkspace().projectForIJavaProject(iJavaProject);

        MethodDeclaration methodDeclaration = PMASTQuery.methodWithNameInClassInCompilationUnit(
                "m", 0, "S", 0,
                (CompilationUnit) pmProject.findASTRootForICompilationUnit(compilationUnit));

        PMCutStep cutStep = new PMCutStep(pmProject, methodDeclaration);

        assertTrue(cutStep != null); // just to make warning go away
    }

    // None of these tests non-textual side effects of cut method

    @Test
    public void testCutMethod() throws JavaModelException {
        String source = "public class S {S s; void m(){System.out.println(s);}}";

        ICompilationUnit compilationUnit = createNewCompilationUnit("", "S.java", source);

        PMProject pmProject = PMWorkspace.sharedWorkspace().projectForIJavaProject(iJavaProject);

        MethodDeclaration methodDeclaration = PMASTQuery.methodWithNameInClassInCompilationUnit(
                "m", 0, "S", 0,
                (CompilationUnit) pmProject.findASTRootForICompilationUnit(compilationUnit));

        PMCutStep cutStep = new PMCutStep(pmProject, methodDeclaration);

        cutStep.applyAllAtOnce();

        assertTrue(compilationUnitSourceMatchesSource("public class S {S s;}",
                compilationUnit.getSource()));
    }

    @Test
    public void testCutStatement() throws JavaModelException {
        String source = "public class S {S s; void m(){System.out.println(s);}}";

        ICompilationUnit compilationUnit = createNewCompilationUnit("", "S.java", source);

        PMProject pmProject = PMWorkspace.sharedWorkspace().projectForIJavaProject(iJavaProject);

        MethodDeclaration methodDeclaration = PMASTQuery.methodWithNameInClassInCompilationUnit(
                "m", 0, "S", 0,
                (CompilationUnit) pmProject.findASTRootForICompilationUnit(compilationUnit));

        Statement firstStatement = (Statement) methodDeclaration.getBody().statements().get(0);

        PMCutStep cutStep = new PMCutStep(pmProject, firstStatement);

        cutStep.applyAllAtOnce();

        assertTrue(compilationUnitSourceMatchesSource("public class S {S s; void m(){}}",
                compilationUnit.getSource()));
    }

    @Test
    public void testCutField() throws JavaModelException {
        String source = "public class S {S s; void m(){System.out.println(s);}}";

        ICompilationUnit compilationUnit = createNewCompilationUnit("", "S.java", source);

        PMProject pmProject = PMWorkspace.sharedWorkspace().projectForIJavaProject(iJavaProject);

        VariableDeclarationFragment fieldDeclarationFragment = PMASTQuery
                .fieldWithNameInClassInCompilationUnit("s", 0, "S", 0,
                        (CompilationUnit) pmProject.findASTRootForICompilationUnit(compilationUnit));

        FieldDeclaration fieldDeclaration = (FieldDeclaration) fieldDeclarationFragment.getParent();

        PMCutStep cutStep = new PMCutStep(pmProject, fieldDeclaration);

        cutStep.applyAllAtOnce();

        assertTrue(compilationUnitSourceMatchesSource(
                "public class S {void m(){System.out.println(s);}}", compilationUnit.getSource()));
    }

    @Test
    public void testCutMultipleStatements() throws JavaModelException {
        String source = "public class S {void m(){int x,y; int a; a = 1; y = 3; x = 2;}}";

        ICompilationUnit iCompilationUnit = createNewCompilationUnit("", "S.java", source);

        PMProject pmProject = PMWorkspace.sharedWorkspace().projectForIJavaProject(iJavaProject);

        CompilationUnit compilationUnit = (CompilationUnit) pmProject
                .findASTRootForICompilationUnit(iCompilationUnit);

        MethodDeclaration methodDeclaration = PMASTQuery.methodWithNameInClassInCompilationUnit(
                "m", 0, "S", 0, compilationUnit);

        Statement thirdStatement = (Statement) methodDeclaration.getBody().statements().get(2);
        Statement fourthStatement = (Statement) methodDeclaration.getBody().statements().get(3);

        List<ASTNode> nodesToCut = new ArrayList<ASTNode>();
        nodesToCut.add(thirdStatement);
        nodesToCut.add(fourthStatement);

        PMCutStep cutStep = new PMCutStep(pmProject, nodesToCut);

        cutStep.applyAllAtOnce();

        assertTrue(compilationUnitSourceMatchesSource(
                "public class S {void m(){int x,y; int a; x = 2;}}", iCompilationUnit.getSource()));

        assertEquals(pmProject.getPasteboard().getPasteboardRoots().size(), 2);
        assertTrue(pmProject.getPasteboard().containsOnlyNodesOfClass(Statement.class));
    }

    @Test
    public void testCutDeclarationButNotReference() throws JavaModelException {
        String source = "public class S {void m(){int x; x = 1;}}";

        ICompilationUnit iCompilationUnit = createNewCompilationUnit("", "S.java", source);

        PMProject pmProject = PMWorkspace.sharedWorkspace().projectForIJavaProject(iJavaProject);

        CompilationUnit compilationUnit = (CompilationUnit) pmProject
                .findASTRootForICompilationUnit(iCompilationUnit);

        MethodDeclaration methodDeclaration = PMASTQuery.methodWithNameInClassInCompilationUnit(
                "m", 0, "S", 0, compilationUnit);

        Statement secondStatement = (Statement) methodDeclaration.getBody().statements().get(0);

        try {
            PMCutStep cutStep = new PMCutStep(pmProject, secondStatement);

            cutStep.applyAllAtOnce();
        } catch (RuntimeException e) {

            System.out.println("Shouldn't throw exception");

            org.junit.Assert.fail("Shouldn't throw exception");
        }

        assertTrue(compilationUnitSourceMatchesSource("public class S {void m(){x = 1;}}",
                iCompilationUnit.getSource()));

    }

    @Test
    public void testCutFieldWithReference() throws JavaModelException {
        String source = "public class S {int x; void m(){x = 1;}}";

        ICompilationUnit iCompilationUnit = createNewCompilationUnit("", "S.java", source);

        PMProject pmProject = PMWorkspace.sharedWorkspace().projectForIJavaProject(iJavaProject);

        VariableDeclarationFragment fieldDeclarationFragment = PMASTQuery
                .fieldWithNameInClassInCompilationUnit("x", 0, "S", 0, (CompilationUnit) pmProject
                        .findASTRootForICompilationUnit(iCompilationUnit));

        FieldDeclaration fieldDeclaration = (FieldDeclaration) fieldDeclarationFragment.getParent();

        PMCutStep cutStep = new PMCutStep(pmProject, fieldDeclaration);

        cutStep.applyAllAtOnce();

        assertTrue(compilationUnitSourceMatchesSource("public class S {void m(){x = 1;}}",
                iCompilationUnit.getSource()));
    }

}
