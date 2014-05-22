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

public class PullUpFieldTransformationGenerator extends TransformationGenerator<PullUpTransformation>
{
	private MFGenerator<AField> mfgenerator;

	public PullUpTransformation
	generateTransformation()
	{
		Pair<ASuperSubtypePair, AField> choice = mfgenerator.randomChoice();
		LinkedList<AMember> elements = new LinkedList<AMember>();
		elements.add(choice.getRight());
		return new PullUpTransformation(this.tproject, choice.getLeft(), elements, false);
	}

	@Override
	public void
	handleProjectChange()
	{
		mfgenerator = new MFGenerator<AField>(this.tproject, AFieldEnumerator.singleton, ASubtypeKindSelector.singleton);
	}

	@Override
	public String
	getName()
	{
		return "pull-up-field";
	}
}
