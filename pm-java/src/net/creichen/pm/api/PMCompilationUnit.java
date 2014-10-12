/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm.api;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.CompilationUnit;

public interface PMCompilationUnit {

    CompilationUnit getCompilationUnit();

    ICompilationUnit getICompilationUnit();

    String getSource();

    void rename(String newName);

    void updatePair(ICompilationUnit source, CompilationUnit newCompilationUnit);
}
