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

import java.util.*;

public abstract class Transformation
{
	protected static final boolean DEBUG_CAUSE = true; // Why the transformation failed

	public enum Kind {
		PM(0),
		ECLIPSE(1);

		int index;
		Kind(int index)
		{
			this.index = index;
		}

		public int
		getIndex()
		{
			return this.index;
		}
	};

	private String name;
	protected TProject tproject;
	protected static final NullProgressMonitor npm = new NullProgressMonitor();

	public
	Transformation(String name, TProject tp)
	{
		this.name = name;
		this.tproject = tp;
		System.err.println("TRANSFORMING: " + name);
	}

	public TProject
	getTProject()
	{
		return this.tproject;
	}

	public String
	getName()
	{
		return this.name;
	}

	/**
	 * Transforms the current IJavaProject according to the specified parameters
	 */
	public boolean
	transform(Kind kind)
	{
		try {
			System.err.println("[transformation] Startup...");
			if (kind == Kind.PM)
				return transformPM();
			else
				return transformEclipse();
		} catch (Exception e) {
			System.err.println("[transformation] Aborted on exception...");
			e.printStackTrace();
			return false;
		}
	}

	protected abstract void
	doTransformPM(PMProject pm_project);

	public final boolean
	transformPM()
	{
		try {
			PMProject pm_project = tproject.getPMProject();

			doTransformPM(pm_project);

			Set<PMInconsistency> inconsistencies = pm_project.allInconsistencies();
			System.err.println("incons #" + inconsistencies.size());
			for (PMInconsistency i : inconsistencies)
				System.err.println("incons : " + i);

			try {
				tproject.save();
			} catch (Exception e) {
				e.printStackTrace();
				System.err.println(" (COULD NOT SAVE!)");
			}
			try {
				tproject.refresh();
			} catch (Exception e) {
				e.printStackTrace();
				System.err.println(" (COULD NOT REFRESH!)");
			}
			
			return inconsistencies.size() == 0 && tproject.staticTestsOK();
		} catch (RuntimeException t) {
			t.printStackTrace();
			throw t;
		}

	}

	protected static class NOPException extends RuntimeException {};

	protected abstract Refactoring
	buildEclipseRefactoring();

	public boolean
	transformEclipse()
	{
		try {
			final Refactoring refactoring = this.buildEclipseRefactoring();

			if (refactoring == null)
				return false;

			final RefactoringStatus status = refactoring.checkAllConditions(npm);
			if (status.hasError()) {
				if (DEBUG_CAUSE)
					System.out.print("[ECLIPSE] After checking conditions:  errors: " + status);
				return false;
			}


			final Change change = refactoring.createChange(npm);
			try {
				change.initializeValidationData(npm);

				if (!change.isEnabled()) {
					if (DEBUG_CAUSE)
						System.out.print("[ECLIPSE] Change is not enabled: " + change);
					return false;
				}

				final RefactoringStatus valid = change.isValid(npm);
				if (valid.hasError()) {
					if (DEBUG_CAUSE)
						System.out.print("[ECLIPSE] Change validity has errors: " + valid);
					return false;
				}

				Object o = change.perform(npm);

				if (o == null && DEBUG_CAUSE)
					System.out.print("[ECLIPSE] Applying the change failed");

				return (o != null);
			} finally {
				change.dispose();
			}
		} catch (NOPException _) {
			return true; // Skip this transformation
		} catch (Throwable e) {
			System.err.println("Exception " + e + " in " + this);
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * Stringifies the transformation.  Should be overridden to also encompass parameters.
	 */
	public String
	toString()
	{
		return getName();
	}
}