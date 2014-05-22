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

/**
 *  Method/Field generator
 */
public final class MFGenerator<MF>
{
	private TProject project;
	private AEnumerator<MF> enumerator;
	private ATypeKindSelector kind_selector;

	public
	MFGenerator(TProject project, AEnumerator<MF> enumerator, ATypeKindSelector kind_selector)
	{
		this.project = project;
		this.enumerator = enumerator;
		this.kind_selector = kind_selector;
	}
	
	public Pair<ASuperSubtypePair, MF>
	randomChoice()
	{
		int choices_nr = 0;

		final ProjectLocationCache plc = ProjectLocationCache.find(project);
		LinkedList<ASuperSubtypePair> pairs = plc.genSuperSubtypePairs();

		for (ASuperSubtypePair pair : pairs)
			choices_nr += enumerator.enumerate(kind_selector.select(pair)).size();

		if (choices_nr == 0)
			throw new RuntimeException("no choices for " + this);

		int choice = plc.getRandom(choices_nr);

		for (ASuperSubtypePair pair : pairs) {
			final LinkedList<MF> choices = enumerator.enumerate(kind_selector.select(pair));
			final int local_choices_nr = choices.size();

			if (choice < local_choices_nr)
				return new Pair<ASuperSubtypePair, MF>(pair, choices.get(choice));
			else
				choice -= local_choices_nr;
		}

		throw new RuntimeException("End of the universe");
	}

	public String
	toString()
	{
		return this.kind_selector + "/" + this.enumerator;
	}
}