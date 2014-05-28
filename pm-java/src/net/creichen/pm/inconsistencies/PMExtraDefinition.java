/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

*******************************************************************************/

package net.creichen.pm.inconsistencies;


import net.creichen.pm.PMCompilationUnit;
import net.creichen.pm.PMProject;

import org.eclipse.jdt.core.dom.ASTNode;

public class PMExtraDefinition extends PMInconsistency {
	protected ASTNode _definingNode;
	
	public PMExtraDefinition(PMProject project, PMCompilationUnit iCompilationUnit, ASTNode usingNode, ASTNode definingNode) {
		super(project, iCompilationUnit, usingNode);
		
		_definingNode = definingNode;
	}
	
	public String getHumanReadableDescription() {
		return "Unexpected definition (" + _definingNode + ") used by " + getNode();
	}
}
