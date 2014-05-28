/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm.inconsistencies;

import net.creichen.pm.PMCompilationUnit;
import net.creichen.pm.PMProject;

import org.eclipse.jdt.core.dom.SimpleName;

public class PMNameConflict extends PMInconsistency {

	String _expectedName;
	SimpleName _name;

	public PMNameConflict(PMProject project,
			PMCompilationUnit iCompilationUnit, SimpleName name,
			String expectedName) {
		super(project, iCompilationUnit, name);
		_name = name;

		_expectedName = expectedName;
	}

	public String getHumanReadableDescription() {
		return "Variable named " + _name + " refers to declaration with name "
				+ _expectedName;
	}
}
