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

import autofrob.transform.*;

public class ASuperSubtypePair
{
	private AType supertype;
	private AType subtype;

	public
	ASuperSubtypePair(TProject p, IType supertype, IType subtype)
	{
		this.supertype = new AType(p, supertype);
		this.subtype = new AType(p, subtype);
	}

	public AType
	getSupertype()
	{
		return supertype;
	}

	public AType
	getSubtype()
	{
		return subtype;
	}


	public IType
	getISupertype()
	{
		return supertype.getIType();
	}

	public IType
	getISubtype()
	{
		return subtype.getIType();
	}


	@Override
	public int
	hashCode()
	{
		return (this.supertype.hashCode() << 2) ^ this.subtype.hashCode();
	}

	@Override
	public String
	toString()
	{
		return "[" + this.subtype + " <: " + this.supertype + "]";
	}

	@Override
	public boolean
	equals(Object o)
	{
		if (o == null)
			return false;

		if (o instanceof ASuperSubtypePair) {
			ASuperSubtypePair other = (ASuperSubtypePair) o;
			return other.supertype.equals(this.supertype)
				&& other.subtype.equals(this.subtype);
		}
		return false;
	}
}