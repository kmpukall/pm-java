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

import java.util.*;
import java.io.*;

import autofrob.transform.*;

public class AMethod extends AMember
{
	private String id;
	private String[] args;

	public
	AMethod(TProject project, IMethod method)
	{
		init(project, new AType(project, method.getDeclaringType()), method);
	}

	public
	AMethod(AType type, IMethod method)
	{
		init(type.getProject(), type, method);
	}

	private final void
	init(TProject project, AType atype, IMethod method)
	{
		this.project = project;
		this.type = atype;
		this.id = method.getElementName();
		this.args = method.getParameterTypes();
	}

	public IMethod
	getIMethod()
	{
		return this.getDeclaringIType().getMethod(this.id, this.args);
	}

	public LinkedList<AElement>
	enumerateMatchingInSubtypesOf(AType t)
	{
		LinkedList<AElement> list = new LinkedList<AElement>();

		for (IType subtype : project.getSubtypes(t.getIType())) {

			try {
				for (IMethod method_ : subtype.getMethods()) {
					AMethod method = new AMethod(project, method_);
					
					if (this.signatureMatches(method))
						list.add(method);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return list;
	}

	public boolean
	signatureMatches(AMethod other)
	{
		if (!(other.id.equals(this.id)
		      && other.args.length == this.args.length))
			return false;

		for (int i = 0; i < args.length; i++)
			if (!this.args[i].equals(other.args[i]))
				return false;

		return true;
	}

	@Override
	public boolean
	equals(Object o)
	{
		if (o == null)
			return false;

		if (o instanceof AMethod) {
			AMethod other = (AMethod) o;

			return this.signatureMatches(other)
				&& other.type.equals(this.type);
		}
		return false;
	}

	@Override
	public String
	toString()
	{
		String arglist = "";
		for (String a : this.args) {
			if (!arglist.equals(""))
				arglist = arglist + ", ";
			arglist = arglist + a;
		}

		return "method:" + type + "." + id + "(" + arglist + ")";
	}

	@Override
	public int
	hashCode()
	{
		return id.hashCode() + args.length ^ type.hashCode();
	}

	public IMember
	getIMember()
	{
		return this.getIMethod();
	}
}