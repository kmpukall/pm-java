/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm.tests;

import java.util.HashSet;

import static org.junit.Assert.*;
import net.creichen.pm.PMASTQuery;
import net.creichen.pm.PMProject;
import net.creichen.pm.PMWorkspace;
import net.creichen.pm.inconsistencies.PMInconsistency;
import net.creichen.pm.steps.PMCopyStep;
import net.creichen.pm.steps.PMCutStep;
import net.creichen.pm.steps.PMPasteStep;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.junit.Test;

public class PMPushDownFieldTest extends PMTest {

    @Test
    public void testPushDownFieldWithNoUses() throws JavaModelException {
        ICompilationUnit iCompilationUnitS = createNewCompilationUnit("", "S.java",
                "public class S { int _y;}");
        ICompilationUnit iCompilationUnitT1 = createNewCompilationUnit("", "T1.java",
                "public class T1 extends S {  }");
        ICompilationUnit iCompilationUnitT2 = createNewCompilationUnit("", "T2.java",
                "public class T2 extends S {  }");

        PMProject project = PMWorkspace.sharedWorkspace().projectForIJavaProject(getIJavaProject());

        FieldDeclaration yField = (FieldDeclaration) PMASTQuery
                .fieldWithNameInClassInCompilationUnit("_y", 0, "S", 0,
                        (CompilationUnit) project.findASTRootForICompilationUnit(iCompilationUnitS))
                .getParent();
        PMCopyStep copyStep1 = new PMCopyStep(project, yField);
        yField = null;
        copyStep1.applyAllAtOnce();

        CompilationUnit compilationUnitT1 = (CompilationUnit) project
                .findASTRootForICompilationUnit(iCompilationUnitT1);
        TypeDeclaration classT1 = PMASTQuery.classWithNameInCompilationUnit("T1", 0,
                compilationUnitT1);

        PMPasteStep pasteStep1 = new PMPasteStep(project, classT1,
                classT1.getBodyDeclarationsProperty(), classT1.bodyDeclarations().size());
        classT1 = null;

        pasteStep1.applyAllAtOnce();

        assertEquals(new HashSet<PMInconsistency>(), project.allInconsistencies());

        yField = (FieldDeclaration) PMASTQuery.fieldWithNameInClassInCompilationUnit("_y", 0, "S",
                0, (CompilationUnit) project.findASTRootForICompilationUnit(iCompilationUnitS))
                .getParent();
        PMCopyStep copyStep2 = new PMCopyStep(project, yField);
        yField = null;
        copyStep2.applyAllAtOnce();

        CompilationUnit compilationUnitT2 = (CompilationUnit) project
                .findASTRootForICompilationUnit(iCompilationUnitT2);
        TypeDeclaration classT2 = PMASTQuery.classWithNameInCompilationUnit("T2", 0,
                compilationUnitT2);

        PMPasteStep pasteStep2 = new PMPasteStep(project, classT2,
                classT2.getBodyDeclarationsProperty(), classT2.bodyDeclarations().size());
        classT2 = null;

        pasteStep2.applyAllAtOnce();

        yField = (FieldDeclaration) PMASTQuery.fieldWithNameInClassInCompilationUnit("_y", 0, "S",
                0, (CompilationUnit) project.findASTRootForICompilationUnit(iCompilationUnitS))
                .getParent();
        PMCutStep cutStep = new PMCutStep(project, yField); // We use cut to
                                                            // delete the
                                                            // original field

        yField = null;
        cutStep.applyAllAtOnce();

        assertEquals(new HashSet<PMInconsistency>(), project.allInconsistencies());

        assertTrue(compilationUnitSourceMatchesSource("public class S {} }",
                iCompilationUnitS.getSource()));

        assertTrue(compilationUnitSourceMatchesSource("public class T1 extends S { int _y;  }",
                iCompilationUnitT1.getSource()));
        assertTrue(compilationUnitSourceMatchesSource("public class T2 extends S { int _y;  }",
                iCompilationUnitT2.getSource()));

    }

}
