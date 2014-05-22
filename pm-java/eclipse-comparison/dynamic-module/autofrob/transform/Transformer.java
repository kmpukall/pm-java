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

import java.util.*;
import java.io.File;

import autofrob.scoring.*;

public class Transformer
{
	TProject tproject;
	TransformationGenerator<?> last_transform_generator = null;

	public
	Transformer(TProject project)
	{
		this.tproject = project;
	}

	private void
	memStatus(String where)
	{
		long free_mem = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
		System.err.println("[MEM] Mem usage at " + where + ": " + (free_mem / 1024) + "k");
	}

	public <T extends Transformation> Result<T>
	doTransform(TransformationGenerator<T> tgen, Scoreboard scoreboard, String destdir_pfx)
	{
		if (tgen != last_transform_generator) {
			last_transform_generator = tgen;
			scoreboard.reportTransformGeneratorChange(tgen.getName());
		}

		scoreboard.reportStart('t');
		tgen.setProject(tproject);
		tproject.refresh();
		T t = tgen.generateTransformation();
		Result<T> r = new Result<T>(t);

 		for (Transformation.Kind kind : Transformation.Kind.values()) {
			memStatus("before " + kind);
			System.err.print("\n\n================================================================================");
			System.err.print("================================================================================");
			System.err.println("================================================================================");
			tproject.copyTo(destdir_pfx + "." + kind + ".before");
			scoreboard.doStart(kind);
			System.err.print("================================================================================");
			System.err.print("================================================================================");
			System.err.println("================================================================================");
			final long start_time = System.currentTimeMillis();
 			boolean transformation_success;
			try {
				transformation_success = t.transform(kind);
			} catch (Exception e) {
				e.printStackTrace();
				transformation_success = false;
			}
			final long stop_time = System.currentTimeMillis();
			scoreboard.doStop(kind);
			memStatus("after " + kind);

			try {
				tproject.save();
			} catch (Exception e) {
				e.printStackTrace();
				System.err.println(">> === COULD NOT SAVE! === <<");
			}
			try {
				tproject.refresh();
			} catch (Exception e) {
				e.printStackTrace();
				System.err.println(">> === COULD NOT REFRESH! === <<");
			}
			
			tproject.copyTo(destdir_pfx + "." + kind);
			memStatus("after copy for " + kind);

			if (transformation_success) {
				System.err.println("--diff:");
				if (!this.tproject.wasChanged()) {
					System.err.println("-- Mode (_): NO CHANGES in " + kind + "! Aborting...");
					scoreboard.countSkippedTransformation(kind);
					transformation_success = false;
				}
			}

 			r.setTransformationAllowed(kind, transformation_success);

 			if (transformation_success) {
 				r.setUnitTestsSucceeded(kind,
							this.tproject.staticTestsOK()
							&& this.tproject.unitTestsOK());
			}
			memStatus("after unit testing for " + kind);
			System.err.println("--Time: " + kind + " " + r.getResult(kind) + " " + (stop_time - start_time) + " ms");

			tproject.restore();
			memStatus("after restore for " + kind);
			if (!tproject.staticTestsOK()
			    || this.tproject.wasChanged()) {
				System.err.println("===> RESTORE FAILED! <===");
				throw new RuntimeException("FATAL: Restore failed!");
			}
 		}

		scoreboard.reportChar(r.getResult(Transformation.Kind.PM).getChar());
		scoreboard.reportChar(r.getResult(Transformation.Kind.ECLIPSE).getChar());
		scoreboard.reportChar(' ');

		return r;
	}


}
