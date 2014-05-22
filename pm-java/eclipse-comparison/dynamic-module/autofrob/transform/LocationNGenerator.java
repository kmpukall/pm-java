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

public abstract class LocationNGenerator extends ASTVisitor
{
	ArrayList<ASTNode> matched_locations = null;
	private TProject project = null;

	public void
	setProject(TProject project)
	{
		if (this.project != null)
			throw new RuntimeException("Can't change project once set");
		this.project = project;
	}

	public TProject
	getProject()
	{
		return this.project;
	}

	/**
	 * This should only be called by the ProjectLocationCache.
	 */
	public List<ASTNode>
	allLocations(ASTNode ast)
	{
		matched_locations = new ArrayList<ASTNode>();

		ast.accept(this);

		return matched_locations;
	}

	public Location
	getRandomNode()
	{
		if (this.project == null)
			throw new RuntimeException("Set new project first using setProject()");

		return ProjectLocationCache.find(this.project).genRandomLocation(this);
	}

	public Location
	getRandomNodeIn(ICompilationUnit ic)
	{
		if (this.project == null)
			throw new RuntimeException("Set new project first using setProject()");

		return ProjectLocationCache.find(this.project).genRandomLocationIn(this, ic);
	}

	public Pair<ASTNode, ICompilationUnit>
	getNode(Location l)
	{
		return ProjectLocationCache.find(this.project).getNode(this, l);
	}

	/**
	 * Adds an AST node to the set of returned nodes
	 *
	 * Concrete location generators should use this method to report pertinent nodes
	 */
	protected void
	add(ASTNode n)
	{
		this.matched_locations.add(n);
	}
}