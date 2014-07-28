/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm.refactorings;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collection;

import net.creichen.pm.PMTest;
import net.creichen.pm.Workspace;
import net.creichen.pm.checkers.ConsistencyValidator;
import net.creichen.pm.inconsistencies.Inconsistency;
import net.creichen.pm.models.Project;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.text.TextSelection;
import org.junit.Test;

public class SplitProcessorTest extends PMTest {
    @Test
    public void testStraightlineCode() throws JavaModelException {
        final String source = "public class S {void m(){int x;x = 1;x = x + 1;}}";

        final ICompilationUnit compilationUnit = createNewCompilationUnit("", "S.java", source);

        final SplitProcessor splitTemporary = new SplitProcessor(new TextSelection(31, 6), compilationUnit);

        final Project pmProject = Workspace.sharedWorkspace().projectForIJavaProject(getIJavaProject());

        ProcessorDriver.drive(splitTemporary);

        assertTrue(compilationUnitSourceMatchesSource("public class S {void m(){int x;int x = 1;x = x + 1;}}",
                compilationUnit.getSource()));

        final Collection<Inconsistency> inconsistencies = ConsistencyValidator.getInstance().getInconsistencies();

        assertEquals(1, inconsistencies.size());
    }
}
