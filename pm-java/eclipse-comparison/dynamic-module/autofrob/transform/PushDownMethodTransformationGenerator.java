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

public class PushDownMethodTransformationGenerator extends TransformationGenerator<PushDownTransformation>
{
	private MFGenerator<AMethod> mfgenerator;

	public PushDownTransformation
	generateTransformation()
	{
		Pair<ASuperSubtypePair, AMethod> choice = mfgenerator.randomChoice();
		LinkedList<AElement> elements = new LinkedList<AElement>();
		elements.add(choice.getRight());
		PushDownTransformation retval = new PushDownTransformation(this.tproject, choice.getLeft(), elements, true);
		retval.setDestTypesToSourceChildren();
		return retval;
	}

	@Override
	public void
	handleProjectChange()
	{
		mfgenerator = new MFGenerator<AMethod>(this.tproject, AMethodEnumerator.singleton, ASupertypeKindSelector.singleton);
	}

	@Override
	public String
	getName()
	{
		return "push-down-method";
	}
}
