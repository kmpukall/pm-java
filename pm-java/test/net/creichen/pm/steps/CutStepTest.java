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

import net.creichen.pm.PMTest;
import net.creichen.pm.Workspace;
import net.creichen.pm.analysis.ASTQuery;
import net.creichen.pm.models.Project;
import net.creichen.pm.utils.Pasteboard;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.junit.Test;

public class CutStepTest extends PMTest {

    @Test
    public void testInstantiation() {

        String source = "public class S {S s; void m(){s.getClass(); m();}}";

        ICompilationUnit compilationUnit = createNewCompilationUnit("", "S.java", source);

        Project pmProject = Workspace.sharedWorkspace().projectForIJavaProject(getIJavaProject());
        final ICompilationUnit iCompilationUnit = compilationUnit;

        MethodDeclaration methodDeclaration = ASTQuery.methodWithNameInClassInCompilationUnit("m", 0, "S", 0,
                (CompilationUnit) pmProject.getCompilationUnitForICompilationUnit(iCompilationUnit));

        CutStep cutStep = new CutStep(pmProject, methodDeclaration);

        assertTrue(cutStep != null); // just to make warning go away
    }

    // None of these tests non-textual side effects of cut method

    @Test
    public void testCutMethod() throws JavaModelException {
        String source = "public class S {S s; void m(){System.out.println(s);}}";

        ICompilationUnit compilationUnit = createNewCompilationUnit("", "S.java", source);

        Project pmProject = Workspace.sharedWorkspace().projectForIJavaProject(getIJavaProject());
        final ICompilationUnit iCompilationUnit = compilationUnit;

        MethodDeclaration methodDeclaration = ASTQuery.methodWithNameInClassInCompilationUnit("m", 0, "S", 0,
                (CompilationUnit) pmProject.getCompilationUnitForICompilationUnit(iCompilationUnit));

        CutStep cutStep = new CutStep(pmProject, methodDeclaration);

        cutStep.applyAllAtOnce();

        assertTrue(compilationUnitSourceMatchesSource("public class S {S s;}", compilationUnit.getSource()));
    }

    @Test
    public void testCutStatement() throws JavaModelException {
        String source = "public class S {S s; void m(){System.out.println(s);}}";

        ICompilationUnit compilationUnit = createNewCompilationUnit("", "S.java", source);

        Project pmProject = Workspace.sharedWorkspace().projectForIJavaProject(getIJavaProject());
        final ICompilationUnit iCompilationUnit = compilationUnit;

        MethodDeclaration methodDeclaration = ASTQuery.methodWithNameInClassInCompilationUnit("m", 0, "S", 0,
                (CompilationUnit) pmProject.getCompilationUnitForICompilationUnit(iCompilationUnit));

        Statement firstStatement = (Statement) methodDeclaration.getBody().statements().get(0);

        CutStep cutStep = new CutStep(pmProject, firstStatement);

        cutStep.applyAllAtOnce();

        assertTrue(compilationUnitSourceMatchesSource("public class S {S s; void m(){}}", compilationUnit.getSource()));
    }

    @Test
    public void testCutField() throws JavaModelException {
        String source = "public class S {S s; void m(){System.out.println(s);}}";

        ICompilationUnit compilationUnit = createNewCompilationUnit("", "S.java", source);

        Project pmProject = Workspace.sharedWorkspace().projectForIJavaProject(getIJavaProject());
        final ICompilationUnit iCompilationUnit = compilationUnit;

        VariableDeclarationFragment fieldDeclarationFragment = ASTQuery.fieldWithNameInClassInCompilationUnit("s", 0,
                "S", 0, (CompilationUnit) pmProject.getCompilationUnitForICompilationUnit(iCompilationUnit));

        FieldDeclaration fieldDeclaration = (FieldDeclaration) fieldDeclarationFragment.getParent();

        CutStep cutStep = new CutStep(pmProject, fieldDeclaration);

        cutStep.applyAllAtOnce();

        assertTrue(compilationUnitSourceMatchesSource("public class S {void m(){System.out.println(s);}}",
                compilationUnit.getSource()));
    }

    @Test
    public void testCutMultipleStatements() throws JavaModelException {
        String source = "public class S {void m(){int x,y; int a; a = 1; y = 3; x = 2;}}";

        ICompilationUnit iCompilationUnit = createNewCompilationUnit("", "S.java", source);

        Project pmProject = Workspace.sharedWorkspace().projectForIJavaProject(getIJavaProject());
        final ICompilationUnit iCompilationUnit1 = iCompilationUnit;

        CompilationUnit compilationUnit = (CompilationUnit) pmProject.getCompilationUnitForICompilationUnit(iCompilationUnit1);

        MethodDeclaration methodDeclaration = ASTQuery.methodWithNameInClassInCompilationUnit("m", 0, "S", 0,
                compilationUnit);

        Statement thirdStatement = (Statement) methodDeclaration.getBody().statements().get(2);
        Statement fourthStatement = (Statement) methodDeclaration.getBody().statements().get(3);

        List<ASTNode> nodesToCut = new ArrayList<ASTNode>();
        nodesToCut.add(thirdStatement);
        nodesToCut.add(fourthStatement);

        CutStep cutStep = new CutStep(pmProject, nodesToCut);

        cutStep.applyAllAtOnce();

        assertTrue(compilationUnitSourceMatchesSource("public class S {void m(){int x,y; int a; x = 2;}}",
                iCompilationUnit.getSource()));

        assertEquals(Pasteboard.getInstance().getPasteboardRoots().size(), 2);
        assertTrue(Pasteboard.getInstance().containsOnlyNodesOfClass(Statement.class));
    }

    @Test
    public void testCutDeclarationButNotReference() throws JavaModelException {
        String source = "public class S {void m(){int x; x = 1;}}";

        ICompilationUnit iCompilationUnit = createNewCompilationUnit("", "S.java", source);

        Project pmProject = Workspace.sharedWorkspace().projectForIJavaProject(getIJavaProject());
        final ICompilationUnit iCompilationUnit1 = iCompilationUnit;

        CompilationUnit compilationUnit = (CompilationUnit) pmProject.getCompilationUnitForICompilationUnit(iCompilationUnit1);

        MethodDeclaration methodDeclaration = ASTQuery.methodWithNameInClassInCompilationUnit("m", 0, "S", 0,
                compilationUnit);

        Statement secondStatement = (Statement) methodDeclaration.getBody().statements().get(0);

        CutStep cutStep = new CutStep(pmProject, secondStatement);

        cutStep.applyAllAtOnce();

        assertTrue(compilationUnitSourceMatchesSource("public class S {void m(){x = 1;}}", iCompilationUnit.getSource()));

    }

    @Test
    public void testCutFieldWithReference() throws JavaModelException {
        String source = "public class S {int x; void m(){x = 1;}}";

        ICompilationUnit iCompilationUnit = createNewCompilationUnit("", "S.java", source);

        Project pmProject = Workspace.sharedWorkspace().projectForIJavaProject(getIJavaProject());
        final ICompilationUnit iCompilationUnit1 = iCompilationUnit;

        VariableDeclarationFragment fieldDeclarationFragment = ASTQuery.fieldWithNameInClassInCompilationUnit("x", 0,
                "S", 0, (CompilationUnit) pmProject.getCompilationUnitForICompilationUnit(iCompilationUnit1));

        FieldDeclaration fieldDeclaration = (FieldDeclaration) fieldDeclarationFragment.getParent();

        CutStep cutStep = new CutStep(pmProject, fieldDeclaration);

        cutStep.applyAllAtOnce();

        assertTrue(compilationUnitSourceMatchesSource("public class S {void m(){x = 1;}}", iCompilationUnit.getSource()));
    }

}
