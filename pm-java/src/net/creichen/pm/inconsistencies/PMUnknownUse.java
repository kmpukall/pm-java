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

public class PMUnknownUse extends PMInconsistency {
    SimpleName _unknownUse;

    public PMUnknownUse(PMProject project, PMCompilationUnit iCompilationUnit, SimpleName unknownUse) {
        super(project, iCompilationUnit, unknownUse);

        _unknownUse = unknownUse;
    }

    public String getHumanReadableDescription() {
        return "Unknown use " + _unknownUse;
    }
}
