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
import org.eclipse.jdt.internal.corext.refactoring.structure.PushDownRefactoringProcessor;
import org.eclipse.jdt.internal.corext.refactoring.structure.PushDownRefactoring;
import org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor;

import pm_refactoring.*;
import pm_refactoring.steps.*;
import pm_refactoring.inconsistencies.*;

import autofrob.model.*;
import autofrob.analysis.*;

import java.util.*;
import java.io.File;

public class PushDownTransformation extends HierarchyTransformation
{
	public
	PushDownTransformation(TProject tp, ASuperSubtypePair typair, List<AElement> elements, boolean include_dependencies)
	{
		super("push-down", tp, typair, elements, include_dependencies, new UsedByDependencyComputer());
	}

	public void
	init(ASuperSubtypePair typair)
	{
		this.source_type = typair.getSupertype();
		this.dest_type = typair.getSubtype();
	}


	/*  PushDownRefactoringProcessor:
        public void computeAdditionalRequiredMembersToPushDown(IProgressMonitor monitor) throws JavaModelException {
	*/


	public RefactoringProcessor
	buildEclipseRefactoringProcessor() throws Exception
	{
		final IMember[] members = new IMember[this.elements.size()];

		int i = 0;
		for (AElement elt : this.elements) {
			members[i++] = elt.getIMember();
			System.err.println("- Moving member " + elt + " (" + elt.getIMember() + ")");
		}
		System.err.println(" ==> " + members.length + " into " + this.dest_type + " (" + this.dest_type.getIType() + ")");

		final PushDownRefactoringProcessor processor = new PushDownRefactoringProcessor(members);
		final Refactoring _ = new PushDownRefactoring(processor);

		final JavaRefactoringArguments arg = new JavaRefactoringArguments(tproject.getName());

		int j = 1;
		for (IMember member : members) {
			arg.setAttribute(JDTRefactoringDescriptor.ATTRIBUTE_ELEMENT + j,
					 JDTRefactoringDescriptor.elementToHandle(this.tproject.getName(), member));
			arg.setAttribute("push" + j, "true");
			++j;
		}

		final RefactoringStatus init_status = processor.initialize(arg);

		if (init_status.hasError()) {
			if (DEBUG_CAUSE)
				System.out.print("[ECLIPSE] Initialisation has errors: " + init_status);
			throw new RuntimeException("Aborting on failed initialisation");
		}

		if (this.include_dependencies)
			processor.computeAdditionalRequiredMembersToPushDown(new NullProgressMonitor());

		return processor;
	}


}