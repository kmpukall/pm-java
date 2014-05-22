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

import java.util.*;
import java.io.File;

public class RenameTransformationGenerator extends TransformationGenerator<RenameTransformation>
{
	SimpleNameNGenerator simple_name_gen = null;
	NameGenerator name_gen;

	public
	RenameTransformationGenerator(NameGenerator ng)
	{
		this.name_gen = ng;
	}

	public void
	handleProjectChange()
	{
		simple_name_gen = new SimpleNameNGenerator();
		simple_name_gen.setProject(tproject);
		name_gen.setProject(tproject);
	}

	public RenameTransformation
	generateTransformation()
	{
		final Location location = simple_name_gen.getRandomNode();
		String new_name = name_gen.getRandomName(location);
		final Location simple_name = simple_name_gen.getRandomNode();
		if (new_name.equals(((SimpleName) simple_name.getASTNode()).getIdentifier()))
			new_name = new_name + "__prime";
		return new RenameTransformation(tproject, simple_name, new_name);
	}

	public String
	getName()
	{
		return "rename";
	}
}
