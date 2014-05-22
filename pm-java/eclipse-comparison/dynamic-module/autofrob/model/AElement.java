/*******************************************************************************
  Copyright (C) 2008 Christoph Reichenbach

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

*******************************************************************************/

package autofrob.model;

import org.eclipse.jdt.apt.core.build.JdtApt;
import org.eclipse.core.resources.*;
import org.eclipse.debug.core.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.apt.core.internal.AptPlugin;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.internal.junit.launcher.JUnitLaunchConfigurationConstants;
import org.eclipse.jdt.junit.launcher.JUnitLaunchConfigurationDelegate;
import org.eclipse.jdt.junit.model.*;
import org.eclipse.jdt.junit.TestRunListener;
import org.eclipse.jdt.junit.JUnitCore;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.debug.core.*;
import org.eclipse.debug.internal.core.*;
import org.eclipse.core.resources.IFile;
import org.eclipse.debug.internal.core.LaunchConfiguration;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.core.dom.*;
import pm_refactoring.*;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.internal.corext.refactoring.*;
import org.eclipse.jdt.internal.corext.dom.NodeFinder;

import java.util.*;
import java.io.*;

import autofrob.transform.*;

public abstract class AElement
{
	protected TProject project;

	public TProject
	getProject()
	{
		return this.project;
	}

	public abstract IMember
	getIMember();

	public ICompilationUnit
	getICompilationUnit()
	{
		return getIMember().getCompilationUnit();
	}

	public String
	getBody()
	{
		ASTNode n = getASTNode();
		if (n == null)
			return "<NO-BODY:" + this.toString() + ">";
		else
			return n.toString();
	}

	public boolean
	exists()
	{
		return getIMember().exists();
	}

	public ASTNode
	getASTNode()
	{
		final IMember elt = getIMember();

		try {
			final ISourceRange source_range = elt.getSourceRange();
			if (elt.getCompilationUnit() == null)
				throw new RuntimeException("No compilation unit : " + elt);
			final CompilationUnit compilation_unit = project.compilationUnit(elt.getCompilationUnit());
			ASTNode result = NodeFinder.perform(compilation_unit, source_range);
			if (result == null) {
				System.err.println("Did not find AST node in " + compilation_unit + ", " + source_range);
				throw new RuntimeException(this.toString());
			}
			return result;
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("(Continuing)");
			return null;
		}
	}
}