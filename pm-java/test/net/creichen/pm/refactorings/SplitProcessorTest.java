/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm.refactorings;

import static net.creichen.pm.tests.Matchers.hasSource;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import net.creichen.pm.consistency.ConsistencyValidator;
import net.creichen.pm.tests.PMTest;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.text.TextSelection;
import org.junit.Test;

public class SplitProcessorTest extends PMTest {
    @Test
    public void testStraightlineCode() throws JavaModelException {
        final String source = "public class S {void m(){int x;x = 1;x = x + 1;}}";

        final ICompilationUnit compilationUnit = createCompilationUnit("", "S.java", source);

        final SplitProcessor splitTemporary = new SplitProcessor(new TextSelection(31, 6), compilationUnit);

        ProcessorDriver.drive(splitTemporary);

        assertThat(compilationUnit, hasSource("public class S {void m(){int x;int x = 1;x = x + 1;}}"));
        assertThat(ConsistencyValidator.getInstance().getInconsistencies(), hasSize(1));
    }
}
