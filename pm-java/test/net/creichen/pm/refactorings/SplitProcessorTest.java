/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm.refactorings;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Set;

import net.creichen.pm.PMTest;
import net.creichen.pm.Project;
import net.creichen.pm.Workspace;
import net.creichen.pm.inconsistencies.Inconsistency;
import net.creichen.pm.refactorings.SplitProcessor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.text.TextSelection;
import org.junit.Test;

public class SplitProcessorTest extends PMTest {
    @Test
    public void testStraightlineCode() throws JavaModelException {
        final String source = "public class S {void m(){int x;x = 1;x = x + 1;}}";

        final ICompilationUnit compilationUnit = createNewCompilationUnit("", "S.java", source);

        final SplitProcessor splitTemporary = new SplitProcessor(new TextSelection(57 - 26,
                63 - 57), compilationUnit);

        final Project pmProject = Workspace.sharedWorkspace().projectForIJavaProject(
                this.getIJavaProject());

        ProcessorDriver.drive(splitTemporary);

        assertTrue(compilationUnitSourceMatchesSource(
                "public class S {void m(){int x;int x = 1;x = x + 1;}}",
                compilationUnit.getSource()));

        final Set<Inconsistency> inconsistencies = pmProject.allInconsistencies();

        assertEquals(1, inconsistencies.size());
    }
}
