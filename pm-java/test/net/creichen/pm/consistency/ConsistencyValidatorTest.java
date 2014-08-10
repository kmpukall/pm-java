/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm.consistency;

import static org.junit.Assert.assertEquals;
import net.creichen.pm.consistency.ConsistencyValidator;
import net.creichen.pm.core.Workspace;
import net.creichen.pm.models.Project;
import net.creichen.pm.tests.PMTest;

import org.junit.Test;

public class ConsistencyValidatorTest extends PMTest {

	@Test
	public void testArrayLengthIsSane() {

		String source = "public class S {void m(){int array[] = new int[5]; System.out.println(array.length); } }";

		createNewCompilationUnit("", "S.java", source);

		Project pmProject = Workspace.getInstance().getProject(getIJavaProject());

		ConsistencyValidator.getInstance().rescanForInconsistencies(pmProject);

		assertEquals(0, ConsistencyValidator.getInstance().getInconsistencies().size());
	}

}
