/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm.models;

import static org.junit.Assert.*;

import java.util.Set;

import net.creichen.pm.api.PMProject;
import net.creichen.pm.api.PMWorkspace;
import net.creichen.pm.inconsistencies.Inconsistency;
import net.creichen.pm.tests.PMTest;

import org.junit.Test;

public class NameModelTest extends PMTest {

    @Test
    public void testArrayLengthIsSane() {

        String source = "public class S {void m(){int array[] = new int[5]; System.out.println(array.length); } }";

        createNewCompilationUnit("", "S.java", source);

        PMProject pmProject = PMWorkspace.sharedWorkspace().projectForIJavaProject(getIJavaProject());

        pmProject.rescanForInconsistencies();

        Set<Inconsistency> inconsistencies = pmProject.allInconsistencies();

        assertEquals(0, inconsistencies.size());
    }

}
