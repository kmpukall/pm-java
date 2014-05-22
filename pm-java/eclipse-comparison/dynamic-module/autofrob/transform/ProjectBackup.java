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
import java.io.*;

public final class ProjectBackup
{
	private HashMap<IPath, ByteArrayInputStream> backups = new HashMap<IPath, ByteArrayInputStream>();
	private IProject project;
	private final NullProgressMonitor npm = new NullProgressMonitor();

	private final boolean DEBUG = false;

	public
	ProjectBackup(IProject project)
	{
		genBackup(project);
	}

	public
	ProjectBackup(IJavaProject ijp)
	{
		genBackup(ijp.getProject());
	}

	private static String
	mk_string(InputStream is) throws IOException
	{
		byte [] data = new byte[is.available()];
		is.read(data);
		is.close();

		return new String(data);
	}

	private static String
	run_shell(String command) throws IOException
	{
		StringBuffer accumulator = new StringBuffer();
		final Process p = Runtime.getRuntime().exec(command);
		try {
			final InputStream os = p.getInputStream();
			final BufferedReader input = new BufferedReader(new InputStreamReader(os));
			String line;
			while ((line = input.readLine()) != null) {
				accumulator.append(line);
				accumulator.append('\n');
			}
			input.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return accumulator.toString();
	}

	private String
	getPidString() throws IOException
	{
		return run_shell("/bin/echo $$").trim();
	}

	private void
	doDiff(final ByteArrayInputStream old_file, final ByteArrayInputStream new_file) throws IOException
	{
		old_file.reset();
		byte [] old_data = new byte[old_file.available()];
		old_file.read(old_data);
		new_file.reset();
		byte [] new_data = new byte[new_file.available()];
		new_file.read(new_data);

		String pid = getPidString();

		String old_file_name = "/tmp/old." + pid;
		String new_file_name = "/tmp/new." + pid;

		FileOutputStream old_file_stream = new FileOutputStream(old_file_name);
		FileOutputStream new_file_stream = new FileOutputStream(new_file_name);

		old_file_stream.write(old_data);
		old_file_stream.flush();
		old_file_stream.close();
		new_file_stream.write(new_data);
		new_file_stream.flush();
		new_file_stream.close();

		System.err.println(run_shell("/usr/bin/diff -u " + old_file_name + " " + new_file_name));
	}

	public boolean // true iff there is a difference; prints out all diffs
	diff(ProjectBackup other)
	{
		boolean has_changed = false;

		/*
		System.err.println("$ DIFF");
		for (IPath p : this.backups.keySet())
			System.err.println("$ orig " + p + ": " + ((other.backups.containsKey(p))? "shared" : "solo"));
		for (IPath p : other.backups.keySet())
			System.err.println("$ upd " + p + ": " + ((this.backups.containsKey(p))? "shared" : "solo"));
		*/

		for (IPath source_p : this.backups.keySet()) {
			if (other.backups.containsKey(source_p)) {
				try {
					final ByteArrayInputStream source_bis = backups.get(source_p);
					final ByteArrayInputStream dest_bis = other.backups.get(source_p);

					source_bis.reset();
					dest_bis.reset();

					final String new_contents = mk_string(source_bis);
					final String old_contents = mk_string(dest_bis);

					if (new_contents.equals(old_contents))
						continue;

					System.err.println("|-- diff in " + source_p);
					has_changed = true;
					doDiff(source_bis, dest_bis);
				} catch (IOException e) {
					System.err.println("|-- IO exn while diffing " + source_p);
					e.printStackTrace();
					has_changed = true;
				}
			} else {
				System.err.println("|-- Lost file: " + source_p);
				has_changed = true;
			}
		}

		for (IPath dest_p : other.backups.keySet())
			if (!this.backups.containsKey(dest_p)) {
				System.err.println("|-- New file: " + dest_p);
				has_changed = true;
			}

		return has_changed;
	}

	public int
	size()
	{
		return backups.size();
	}

	private void
	backup_recursively(IContainer c)
	{
		try {
			for (IResource r : c.members()) {
				if (r instanceof IFile) {
					IFile f = (IFile) r;

					if (!f.getName().endsWith(".java"))
						continue;

					InputStream is = f.getContents();
					byte[] data = new byte[is.available()];
					is.read(data, 0, data.length);
					is.close();
					ByteArrayInputStream backup = new ByteArrayInputStream(data);
					if (DEBUG)
						System.err.println("[# BACKING UP file " + f.getFullPath().makeRelative() + "]");

					backups.put(f.getFullPath(), backup);

				} else if (r instanceof IContainer)
					backup_recursively((IContainer) r);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private void
	genBackup(IProject p)
	{
		this.project = p;
		backup_recursively(p);
	}

	private void
	restore_recursively(IContainer c, HashSet<IPath> restored_files)
		throws CoreException
	{
		for (IResource r : c.members())
			if (r instanceof IFile) {
				IFile f = (IFile) r;
				if (backups.containsKey(f.getFullPath())) {

					if (!f.getName().endsWith(".java"))
						continue;

					restored_files.add(f.getFullPath());

					if (DEBUG)
						System.err.println("[# Restoring file " + f.getName() + "]");
					ByteArrayInputStream data = backups.get(f.getFullPath());
					data.reset();
					f.setContents(data, true, false, npm);
				} else {
					if (!f.getName().endsWith(".java"))
						continue;

					/* File was newly added? */
					if (DEBUG)
						System.err.println("[# DELETING file " + f.getFullPath() + "]");
					for (IPath ffds : backups.keySet())
						System.err.println("+ " + ffds);
					f.delete(true, false, npm);
				}
			} else if (r instanceof IContainer)
				restore_recursively((IContainer) r, restored_files);
	}

	private void
	create_path(IPath path)
	{
		if (path == null
		    || path.isRoot()
		    || path.isEmpty())
			return;

		final IFolder folder = this.project.getFolder(path);
		if (folder.exists())
			return;

		if (DEBUG)
			System.err.println("Must create path " + path);

		create_path(path.removeLastSegments(1));
		try {
			folder.create(true, true, npm);
		} catch (Exception e) {}
	}

	public void
	restore()
	{
		try {
			HashSet<IPath> restored_files = new HashSet<IPath>();
			restore_recursively(project, restored_files);

			/* Now check for files that we deleted and must restore in their entirety */
			for (IPath fname : backups.keySet())
				if (!restored_files.contains(fname)) {
					ByteArrayInputStream data = backups.get(fname);
					data.reset();
					IPath proper_path = fname.removeFirstSegments(1).makeAbsolute();
					IFile f = project.getFile(proper_path);
					if (DEBUG)
						System.err.println("[# Recovering file " + f.getName() + " at path " + f.getFullPath() + "]");
					System.err.println("Old fname: " + fname);
					System.err.println("Abs fname: " + proper_path);
					System.err.println("New fname: " + f.getFullPath());
					System.err.println("Base path: " + f.getFullPath().removeLastSegments(1));
					System.err.println("Backup path: " + f.getFullPath().removeLastSegments(1).removeFirstSegments(1));
					create_path(f.getFullPath().removeLastSegments(1).removeFirstSegments(1));
					
					f.create(data, true, npm);
					//System.exit(0);
				}

			project.refreshLocal(IProject.DEPTH_INFINITE, new NullProgressMonitor());

		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}
