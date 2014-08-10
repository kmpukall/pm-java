/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm.consistency.inconsistencies;

import net.creichen.pm.api.PMCompilationUnit;

import org.eclipse.jdt.core.dom.SimpleName;

public class UnknownUse extends Inconsistency {
	private final SimpleName unknownUse;

	public UnknownUse(final PMCompilationUnit iCompilationUnit, final SimpleName unknownUse) {
		super(iCompilationUnit, unknownUse);
		this.unknownUse = unknownUse;
	}

	@Override
	public String getHumanReadableDescription() {
		return "Unknown use " + this.unknownUse;
	}
}
