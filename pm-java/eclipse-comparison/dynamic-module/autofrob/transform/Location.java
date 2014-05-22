/*******************************************************************************
  Copyright (C) 2008 Christoph Reichenbach

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

*******************************************************************************/

package autofrob.transform;

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

import autofrob.model.*;

import java.util.*;
import java.io.File;

public class Location
{
	private int index;
	private LocationNGenerator lgen;

	public Location(LocationNGenerator lgen, int index)
	{
		this.index = index;
		this.lgen = lgen;
	}

	public int
	getIndex()
	{
		return index;
	}

	public String
	toString()
	{
		final Pair<ASTNode, ICompilationUnit> blah = lgen.getNode(this);
		final ASTNode node = blah.getLeft();
		final ICompilationUnit icu = blah.getRight();
		final int start_pos = node.getStartPosition();
		final int length = node.getLength();

		return "index#" + index + "/" + lgen + "(" + icu.getPath() + "@" + start_pos + "+" + length + ")";
	}

	public ICompilationUnit
	getICompilationUnit()
	{
		return lgen.getNode(this).getRight();
	}

	public CompilationUnit
	getCompilationUnit()
	{
		return lgen.getProject().compilationUnit(getICompilationUnit());
	}

	public ASTNode
	getASTNode()
	{
		return lgen.getNode(this).getLeft();
	}

	public IJavaElement
	getIJavaElement()
	{
		final ASTNode ast_node = getASTNode();

		if (ast_node == null) {
			System.err.println("!!! No AST node to represent location " + this);
			return null;
		}

		if (ast_node instanceof Name) {
			Name n = (Name) ast_node;
			if (n.resolveBinding() == null)
				return getIJavaElementOld();
			else
				return n.resolveBinding().getJavaElement();
		} else {
			System.err.println("!!! AST node of type " + ast_node.getClass() + " is not of type Name");
			throw new RuntimeException("!!! No name found");
		}
	}

	public IJavaElement
	getIJavaElementOld()
	{
		final Pair<ASTNode, ICompilationUnit> blah = lgen.getNode(this);
		final ASTNode node = blah.getLeft();
		final ICompilationUnit icu = blah.getRight();
		final int start_pos = node.getStartPosition();
		final int length = node.getLength();

		System.err.println("ICU information for " + icu.getPath() + " : " + icu.getClass() + " :");
		System.err.println("  is working copy: " + icu.isWorkingCopy());
		System.err.println("  has resource changed: " + icu.hasResourceChanged());
		System.err.println("  primary: " + (icu.getOwner() == null ? "null" : icu.getPrimary().getPath()));
		try {
			System.err.println("  types#: " + icu.getTypes().length);
		} catch (Exception _) {
			System.err.println("  types# FAILED");
		}

		try {
			icu.open(new NullProgressMonitor());
			final IJavaElement[] elts = icu.codeSelect(start_pos, length);

			if (elts.length != 1) {
				System.err.println("Total # of matches: " + elts.length);
				for (IJavaElement e : elts) {
					System.err.println(" - " + e + " : " + e.getClass());
				}
				throw new RuntimeException("(Incomplete-Match) Found " + elts.length + " != 1 IJavaElement matches for AST at " + icu.getPath() + "@" + start_pos + "+" + length);
			}

			return elts[0];
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}