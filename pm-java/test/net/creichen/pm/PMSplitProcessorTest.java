/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

*******************************************************************************/

package net.creichen.pm;


import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import java.util.Set;

import net.creichen.pm.PMProject;
import net.creichen.pm.PMSplitProcessor;
import net.creichen.pm.PMWorkspace;
import net.creichen.pm.inconsistencies.PMInconsistency;
import net.creichen.pm.tests.PMTest;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.text.TextSelection;
import org.junit.Test;


public class PMSplitProcessorTest extends PMTest {
	@Test public void testStraightlineCode() throws JavaModelException {
		String source = "public class S {void m(){int x;x = 1;x = x + 1;}}";
		
		ICompilationUnit compilationUnit = createNewCompilationUnit("", "S.java", source);

		
		PMSplitProcessor splitTemporary = new PMSplitProcessor(new TextSelection(57 - 26, 63 - 57), compilationUnit);
		
		PMProject pmProject = PMWorkspace.sharedWorkspace().projectForIJavaProject(_iJavaProject);
	
		
		PMProcessorDriver.drive(splitTemporary);
		
				
		assertTrue(compilationUnitSourceMatchesSource("public class S {void m(){int x;int x = 1;x = x + 1;}}", compilationUnit.getSource()));
	

		Set<PMInconsistency> inconsistencies = pmProject.allInconsistencies();

		
		assertEquals(1, inconsistencies.size());
	}
}