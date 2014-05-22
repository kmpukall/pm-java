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

public class AField extends AMember
{
	private String id;

	public
	AField(TProject project, IField field)
	{
		init(project, new AType(project, field.getDeclaringType()), field);
	}

	public
	AField(AType type, IField field)
	{
		init(type.getProject(), type, field);
	}

	private AField(AField source, AType atype)
	{
		this.project = source.project;
		this.type = atype;
		this.id = source.id;
	}

	public LinkedList<AElement>
	enumerateMatchingInSubtypesOf(AType t)
	{
		LinkedList<AElement> list = new LinkedList<AElement>();

		for (IType subtype : project.getSubtypes(t.getIType())) {

			try {
				for (IField field_ : subtype.getFields()) {
					AField field = new AField(project, field_);

					if (this.signatureMatches(field))
						list.add(field);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return list;
	}

	private final void
	init(TProject project, AType atype, IField field)
	{
		this.project = project;
		this.type = atype;
		this.id = field.getElementName();
	}

	public IField
	getIField()
	{
		return this.getDeclaringIType().getField(this.id);
	}

	public boolean
	signatureMatches(AField other)
	{
		return other.id.equals(this.id);
	}

	@Override
	public boolean
	equals(Object o)
	{
		if (o == null)
			return false;

		if (o instanceof AMethod) {
			AField other = (AField) o;

			return this.signatureMatches(other)
				&& other.type.equals(this.type);
		}
		return false;
	}

	@Override
	public String
	toString()
	{
		return "field:" + type + "." + id;
	}

	@Override
	public int
	hashCode()
	{
		return id.hashCode() << 1 ^ type.hashCode();
	}

	public IMember
	getIMember()
	{
		return this.getIField();
	}
}