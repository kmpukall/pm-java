/*******************************************************************************
  Copyright (C) 2008 Christoph Reichenbach

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

*******************************************************************************/

package autofrob;

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

import autofrob.transform.*;
import autofrob.scoring.*;

import java.util.*;
import java.io.*;

class DoFrob implements FrobInterface
{
	IProject []
	getProjects()
	{
		IWorkspace workspace = org.eclipse.core.resources.ResourcesPlugin.getWorkspace();
		IWorkspaceRoot root = workspace.getRoot();
		return root.getProjects();
	}

	public void
	run(IApplication master, IApplicationContext ctx, String [] args)
	{
		IProject [] projects = getProjects();
		System.out.println("[AutoFrob] Initialising (" + args[0] + ")...");
		try {
			new AptPlugin().start(ctx.getBrandingBundle().getBundleContext());
		} catch (Exception e) {
			e.printStackTrace();
		}

                System.out.println("[AutoFrob] Initialised; " + projects.length + " projects.");

		if (projects.length == 0) {
			System.out.println("[AutoFrob] No projects found, aborting.");
			return;
		}

		if (projects.length > 1) {
			System.out.println("[AutoFrob] More than one project found, aborting.");
			return;
		}


		IJavaProject ijp = null;
		try {
			projects[0].refreshLocal(IProject.DEPTH_INFINITE, new NullProgressMonitor());
			projects[0].open(0, new NullProgressMonitor());

			if (projects[0].getNature(JavaCore.NATURE_ID) instanceof IJavaProject) {
				ijp = (IJavaProject)projects[0].getNature(JavaCore.NATURE_ID);
				final TProject project = new TProject(args[0], ijp);

				System.err.println("[AutoFrob] Initial unit test run...");
				if (!project.unitTestsOK()) {
					System.err.println("[AutoFrob] Initial unit test run failed.\nAborting...");
					System.exit(1);
				}
				System.err.println("[AutoFrob] Initial unit tests OK.");

				runTests(project);
			} else
				System.out.println("[AutoFrob] Project is not a Java project.");

		} catch (CoreException e) { e.printStackTrace(); }
	}


	void
	runTests(TProject tp)
	{
		final Transformer transformer = new Transformer(tp);

		final Scoreboard<?> scoreboards[] = new Scoreboard<?>[] {
			new Scoreboard(new PullUpFieldTransformationGenerator(),
				       200),
			new Scoreboard(new PullUpMethodTransformationGenerator(),
				       200),
			new Scoreboard(new PushDownFieldTransformationGenerator(),
				       0),
			new Scoreboard(new PushDownMethodTransformationGenerator(),
				       0),
			new Scoreboard(new RenameTransformationGenerator(new EitherNameGenerator(0.5f,
												 new FreshNameGenerator("x"),
												 new CompilationUnitNameGenerator())),
				       200)
		};

		// Run all tests below
		for (Scoreboard<?> scoreboard : scoreboards) {
			scoreboard.setTransformer(transformer);
			scoreboard.run();
			scoreboard.flushOutput();
		}

		// Print scoreboards
		PrintStream result_file = null;
		try {
			result_file = new PrintStream("log/scores");
		} catch (Exception _) {}

		Scoreboard.printScoreboardHeader(System.out);
		if (result_file != null)
			Scoreboard.printScoreboardHeader(result_file);

		for (Scoreboard<?> scoreboard : scoreboards) {
			scoreboard.printScoreboard(System.out);
			if (result_file != null)
				scoreboard.printScoreboard(result_file);
		}
		result_file.close();
	}

}
