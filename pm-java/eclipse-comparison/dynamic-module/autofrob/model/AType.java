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

import java.util.*;
import java.io.*;

import java.util.Set;
import java.util.HashSet;

import autofrob.transform.*;

public class AType extends AElement
{
	private String name;

	public
	AType(TProject p, IType t)
	{
		this.name = t.getFullyQualifiedName();
		if (this.name == null)
			throw new RuntimeException("Fully qualified name of `" + t + "' is null!");
		this.project = p;
	}

	public TProject
	getProject()
	{
		return this.project;
	}

	public IType
	getIType()
	{
		try {
			return this.project.getJavaProject().findType(this.name);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	public Set<AType>
	getDirectSubtypes()
	{
		Set<AType> retval = new HashSet<AType>();
		for (IType t : this.project.getDirectSubtypes(this.getIType()))
			retval.add(new AType(this.project, t));
		return retval;
	}

	@Override
	public boolean
	equals(Object o)
	{
		if (o == null)
			return false;

		if (o instanceof AType) {
			AType other = (AType) o;

			return other.name.equals(this.name);
		}
		return false;
	}

	@Override
	public String
	toString()
	{
		return name;
	}

	@Override
	public int
	hashCode()
	{
		return name.hashCode();
	}

	public IMember
	getIMember()
	{
		return this.getIType();
	}
}