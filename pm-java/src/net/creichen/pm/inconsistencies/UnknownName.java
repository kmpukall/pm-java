/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm.inconsistencies;

import net.creichen.pm.api.PMCompilationUnit;

import org.eclipse.jdt.core.dom.SimpleName;

public class UnknownName extends Inconsistency {

	private final SimpleName unknownName;

	public UnknownName(final PMCompilationUnit iCompilationUnit, final SimpleName unknownName) {
		super(iCompilationUnit, unknownName);
		this.unknownName = unknownName;
	}

	@Override
	public String getHumanReadableDescription() {
		return "Unknown name " + this.unknownName;
	}
}
