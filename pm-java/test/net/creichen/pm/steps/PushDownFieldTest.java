/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm.steps;

import static net.creichen.pm.tests.Matchers.hasNoInconsistencies;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import net.creichen.pm.tests.PMTest;
import net.creichen.pm.utils.ASTQuery;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.junit.Test;

public class PushDownFieldTest extends PMTest {

    @Test
    public void testPushDownFieldWithNoUses() throws JavaModelException {
        ICompilationUnit iCompilationUnitS = createCompilationUnit("", "S.java", "public class S { int _y;}");
        ICompilationUnit iCompilationUnitT1 = createCompilationUnit("", "T1.java", "public class T1 extends S {  }");
        ICompilationUnit iCompilationUnitT2 = createCompilationUnit("", "T2.java", "public class T2 extends S {  }");

        FieldDeclaration yField = (FieldDeclaration) ASTQuery.findFieldByName("_y", 0, "S", 0,
                getProject().getCompilationUnit(iCompilationUnitS)).getParent();
        CopyStep copyStep1 = new CopyStep(getProject(), yField);
        copyStep1.applyAllAtOnce();
        final ICompilationUnit iCompilationUnit1 = iCompilationUnitT1;

        CompilationUnit compilationUnitT1 = getProject().getCompilationUnit(iCompilationUnit1);
        TypeDeclaration classT1 = ASTQuery.findClassByName("T1", compilationUnitT1);

        PasteStep pasteStep1 = new PasteStep(getProject(), classT1, classT1.getBodyDeclarationsProperty(), classT1
                .bodyDeclarations().size());
        classT1 = null;

        pasteStep1.applyAllAtOnce();

        assertThat(getProject(), hasNoInconsistencies());

        final ICompilationUnit iCompilationUnit2 = iCompilationUnitS;

        yField = (FieldDeclaration) ASTQuery.findFieldByName("_y", 0, "S", 0,
                getProject().getCompilationUnit(iCompilationUnit2)).getParent();
        CopyStep copyStep2 = new CopyStep(getProject(), yField);
        yField = null;
        copyStep2.applyAllAtOnce();
        final ICompilationUnit iCompilationUnit3 = iCompilationUnitT2;

        CompilationUnit compilationUnitT2 = getProject().getCompilationUnit(iCompilationUnit3);
        TypeDeclaration classT2 = ASTQuery.findClassByName("T2", compilationUnitT2);

        PasteStep pasteStep2 = new PasteStep(getProject(), classT2, classT2.getBodyDeclarationsProperty(), classT2
                .bodyDeclarations().size());
        classT2 = null;

        pasteStep2.applyAllAtOnce();
        final ICompilationUnit iCompilationUnit4 = iCompilationUnitS;

        yField = (FieldDeclaration) ASTQuery.findFieldByName("_y", 0, "S", 0,
                getProject().getCompilationUnit(iCompilationUnit4)).getParent();
        CutStep cutStep = new CutStep(getProject(), yField); // We use cut to
        // delete the
        // original field

        yField = null;
        cutStep.applyAllAtOnce();

        assertThat(getProject(), hasNoInconsistencies());
        assertTrue(matchesSource("public class S {} }", iCompilationUnitS.getSource()));

        assertTrue(matchesSource("public class T1 extends S { int _y;  }", iCompilationUnitT1.getSource()));
        assertTrue(matchesSource("public class T2 extends S { int _y;  }", iCompilationUnitT2.getSource()));

    }

}
