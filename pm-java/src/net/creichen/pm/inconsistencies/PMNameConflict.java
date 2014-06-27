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

    private final String expectedName;
    private final SimpleName name;

    public PMNameConflict(final PMProject project, final PMCompilationUnit iCompilationUnit,
            final SimpleName name, final String expectedName) {
        super(project, iCompilationUnit, name);
        this.name = name;

        this.expectedName = expectedName;
    }

    @Override
    public String getHumanReadableDescription() {
        return "Variable named " + this.name + " refers to declaration with name "
                + this.expectedName;
    }
}
