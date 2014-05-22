/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

*******************************************************************************/

package pm_refactoring;

import org.eclipse.jdt.core.ICompilationUnit;


public interface PMProcessor {
	public void textChangeWasApplied();
	
	public void textChangeWasNotApplied();
	
	public ICompilationUnit getICompilationUnit();
}
