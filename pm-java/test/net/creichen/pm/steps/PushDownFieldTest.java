/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm.steps;

import java.util.HashSet;

import static org.junit.Assert.*;
import net.creichen.pm.PMTest;
import net.creichen.pm.Project;
import net.creichen.pm.Workspace;
import net.creichen.pm.analysis.ASTQuery;
import net.creichen.pm.inconsistencies.Inconsistency;
import net.creichen.pm.steps.CopyStep;
import net.creichen.pm.steps.CutStep;
import net.creichen.pm.steps.PasteStep;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.junit.Test;

public class PushDownFieldTest extends PMTest {

    @Test
    public void testPushDownFieldWithNoUses() throws JavaModelException {
        ICompilationUnit iCompilationUnitS = createNewCompilationUnit("", "S.java",
                "public class S { int _y;}");
        ICompilationUnit iCompilationUnitT1 = createNewCompilationUnit("", "T1.java",
                "public class T1 extends S {  }");
        ICompilationUnit iCompilationUnitT2 = createNewCompilationUnit("", "T2.java",
                "public class T2 extends S {  }");

        Project project = Workspace.sharedWorkspace().projectForIJavaProject(getIJavaProject());

        FieldDeclaration yField = (FieldDeclaration) ASTQuery
                .fieldWithNameInClassInCompilationUnit("_y", 0, "S", 0,
                        (CompilationUnit) project.findASTRootForICompilationUnit(iCompilationUnitS))
                .getParent();
        CopyStep copyStep1 = new CopyStep(project, yField);
        yField = null;
        copyStep1.applyAllAtOnce();

        CompilationUnit compilationUnitT1 = (CompilationUnit) project
                .findASTRootForICompilationUnit(iCompilationUnitT1);
        TypeDeclaration classT1 = ASTQuery.classWithNameInCompilationUnit("T1", 0,
                compilationUnitT1);

        PasteStep pasteStep1 = new PasteStep(project, classT1,
                classT1.getBodyDeclarationsProperty(), classT1.bodyDeclarations().size());
        classT1 = null;

        pasteStep1.applyAllAtOnce();

        assertEquals(new HashSet<Inconsistency>(), project.allInconsistencies());

        yField = (FieldDeclaration) ASTQuery.fieldWithNameInClassInCompilationUnit("_y", 0, "S",
                0, (CompilationUnit) project.findASTRootForICompilationUnit(iCompilationUnitS))
                .getParent();
        CopyStep copyStep2 = new CopyStep(project, yField);
        yField = null;
        copyStep2.applyAllAtOnce();

        CompilationUnit compilationUnitT2 = (CompilationUnit) project
                .findASTRootForICompilationUnit(iCompilationUnitT2);
        TypeDeclaration classT2 = ASTQuery.classWithNameInCompilationUnit("T2", 0,
                compilationUnitT2);

        PasteStep pasteStep2 = new PasteStep(project, classT2,
                classT2.getBodyDeclarationsProperty(), classT2.bodyDeclarations().size());
        classT2 = null;

        pasteStep2.applyAllAtOnce();

        yField = (FieldDeclaration) ASTQuery.fieldWithNameInClassInCompilationUnit("_y", 0, "S",
                0, (CompilationUnit) project.findASTRootForICompilationUnit(iCompilationUnitS))
                .getParent();
        CutStep cutStep = new CutStep(project, yField); // We use cut to
                                                            // delete the
                                                            // original field

        yField = null;
        cutStep.applyAllAtOnce();

        assertEquals(new HashSet<Inconsistency>(), project.allInconsistencies());

        assertTrue(compilationUnitSourceMatchesSource("public class S {} }",
                iCompilationUnitS.getSource()));

        assertTrue(compilationUnitSourceMatchesSource("public class T1 extends S { int _y;  }",
                iCompilationUnitT1.getSource()));
        assertTrue(compilationUnitSourceMatchesSource("public class T2 extends S { int _y;  }",
                iCompilationUnitT2.getSource()));

    }

}
