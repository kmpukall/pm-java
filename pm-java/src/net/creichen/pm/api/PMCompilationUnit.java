/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm.api;

import net.creichen.pm.consistency.inconsistencies.Inconsistency;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;

public interface PMCompilationUnit {

    CompilationUnit getCompilationUnit();

    ICompilationUnit getICompilationUnit();

    String getSource();

    String getHandleIdentifier();

    void rename(String newName);

    void updatePair(ICompilationUnit source, CompilationUnit newCompilationUnit);

    void createMarker(final Inconsistency inconsistency, final IJavaProject iJavaProject) throws CoreException;

    void accept(ASTVisitor visitor);

    IProblem[] getProblems();
}
