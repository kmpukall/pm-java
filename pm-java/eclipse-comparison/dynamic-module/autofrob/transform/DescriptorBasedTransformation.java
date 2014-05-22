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

import pm_refactoring.*;
import pm_refactoring.steps.*;
import pm_refactoring.inconsistencies.*;

import autofrob.model.*;

import java.util.*;

public abstract class DescriptorBasedTransformation extends Transformation
{
	public
	DescriptorBasedTransformation(String name, TProject tp)
	{
		super(name, tp);
	}

	protected JavaRefactoringDescriptor
	getDescriptor(String kind)
	{
		final RefactoringContribution refactoring_contribution = RefactoringCore.getRefactoringContribution(kind);

		if (refactoring_contribution == null) {
			System.err.println("[ECLIPSE] No refactoring contribution!");
			throw new RuntimeException("No refactoring contribution");
		}

		final JavaRefactoringDescriptor descriptor = (JavaRefactoringDescriptor) refactoring_contribution.createDescriptor();

		if (descriptor == null) {
			System.err.println("[ECLIPSE] No refactoring descriptor for `" + kind + "'!");
			throw new RuntimeException("No refactoring descriptor for `" + kind + "'");
		}

		return descriptor;
	}

	protected abstract JavaRefactoringDescriptor
	buildEclipseDescriptor() throws Exception;

	@Override
	protected Refactoring
	buildEclipseRefactoring()
	{
		try {
			JavaRefactoringDescriptor descriptor = buildEclipseDescriptor();

			if (descriptor.validateDescriptor().hasFatalError()) {
				System.err.println("Missing args!\n");
				throw new RuntimeException("Fatal: Not all relevant parameters set for refactoring!");
			}

			final RefactoringStatus init_status = new RefactoringStatus();
			final Refactoring refactoring = descriptor.createRefactoring(init_status);

			if (init_status.hasError()) {
				if (DEBUG_CAUSE)
					System.out.print("[ECLIPSE] Initial status has errors: " + init_status);
				throw new RuntimeException("Aborting refactoring due to initial status failure");
			}

			return refactoring;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
