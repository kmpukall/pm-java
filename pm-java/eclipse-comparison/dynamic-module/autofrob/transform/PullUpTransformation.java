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
import org.eclipse.jdt.core.refactoring.*;
import org.eclipse.jdt.core.refactoring.descriptors.*;
import org.eclipse.ltk.core.refactoring.*;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.internal.corext.dom.NodeFinder;
import org.eclipse.jdt.internal.corext.refactoring.*;
import org.eclipse.jdt.internal.corext.refactoring.structure.PullUpRefactoringProcessor;
import org.eclipse.jdt.internal.corext.refactoring.structure.PullUpRefactoring;
import org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor;

import pm_refactoring.*;
import pm_refactoring.steps.*;
import pm_refactoring.inconsistencies.*;

import autofrob.model.*;
import autofrob.analysis.*;

import java.util.*;
import java.io.File;

public class PullUpTransformation extends HierarchyTransformation
{
	public
	PullUpTransformation(TProject tp, ASuperSubtypePair typair, List<AMember> elements, boolean include_dependencies)
	{
		super("pull-up", tp, typair, (List<AElement>)(Object)elements, include_dependencies, new UsesDependencyComputer());
	}

	public void
	init(ASuperSubtypePair typair)
	{
		this.source_type = typair.getSubtype();
		this.dest_type = typair.getSupertype();
	}

	public HashSet<AElement>
	getToDelete(HashSet<AElement> elts)
	{
		HashSet<AElement> retval = new HashSet<AElement>();

		for (AElement e: elts)
			if (e instanceof AMember) {
				final AMember m = (AMember) e;
				final String body = m.getBody();

				System.err.println("  - Final move of " + m + " to " + this.dest_type + " supercedes:");

				for (AElement subi : m.enumerateMatchingInSubtypesOf(this.dest_type)) {
					if (!subi.equals(m) && body.equals(subi.getBody())) {
						System.err.println("      + " + subi);
						retval.add(subi);
					}
				}
			}

		return retval;
	}

	private RefactoringProcessor
	buildWithMembers(IMember[] members0, IMember[] members1, boolean recurse) throws Exception
	{
		final IMember[] members;

		if (members1 == null)
			members = members0;
		else {
			members = new IMember[members0.length + members1.length];

			for (int i = 0; i < members0.length; i++)
				members[i] = members0[i];

			for (int i = 0; i < members1.length; i++)
				members[members0.length + i] = members1[i];
		}

		final PullUpRefactoringProcessor processor = new PullUpRefactoringProcessor(members, tproject.getCodeGenerationSettings());
		final Refactoring refactoring = new PullUpRefactoring(processor);
		processor.setDestinationType(this.dest_type.getIType());

		if (recurse) {
			final IMember[] more_members = processor.getAdditionalRequiredMembersToPullUp(new NullProgressMonitor());
			if (more_members != null
			    && more_members.length > 0)
				return buildWithMembers(members, more_members, true);
		}

		return processor;
	}

	public RefactoringProcessor
	buildEclipseRefactoringProcessor() throws Exception
	{
		final IMember[] members = new IMember[this.elements.size()];

		int i = 0;
		for (AElement elt : this.elements) {
			members[i++] = elt.getIMember();
			System.err.println("- Moving member " + elt + " (" + elt.getIMember() + ")");
		}
		System.err.println(" ==> " + this.dest_type + " (" + this.dest_type.getIType() + ")");

		return buildWithMembers(members, null, this.include_dependencies);
	}
}