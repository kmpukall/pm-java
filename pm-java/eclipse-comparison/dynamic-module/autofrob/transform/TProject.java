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
import pm_refactoring.*;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;
//import org.eclipse.cdt.core.model.Flags;

import autofrob.model.AType;

import java.util.*;
import java.io.*;

public class TProject
{
	private IProject project;
	private IJavaProject java_project;
	private ProjectBackup backup;
	private PMProject pm_project;
	private String path_offset;
	private static String junit_jar = null;

	private final static boolean DEBUG_CLONE_WITH_RENAME = false;
	private final static boolean DEBUG_TESTS = false;

	private final LinkedList<String> canonical_cu_order = new LinkedList<String>(); // for compilation units
	private final NullProgressMonitor npm = new NullProgressMonitor();

	private HashSet<TUnitLaunchConfig> launch_configurations = new HashSet<TUnitLaunchConfig>();

	public String
	getName()
	{
		return java_project.getPath().lastSegment();
	}

	private static void
	init_junit_jar()
	{
		if (junit_jar == null) {
			final String dir = "/usr/share/java/";
			File[] choices = new File(dir).listFiles();
			if (choices == null)
				throw new RuntimeException("Could not find files in `" + dir + "'");

			int score = 0;

			for (File f : choices) {
				String filename = f.getName();

				if (filename.startsWith("junit")
				    && filename.endsWith("jar")) { //contender!
					int localscore = 1;
					if (filename.startsWith("junit3")
					    || filename.startsWith("junit-3")
					    || filename.startsWith("junit_3"))
						localscore = 2;
					if (localscore > score) {
						score = localscore;
						junit_jar = dir + filename;
					}
				}
			}

			if (junit_jar == null)
				throw new RuntimeException("Could not find matching junit jar file in `" + dir + "'");
			System.err.println("[junit] Using jar file " + junit_jar);
		}
	}

	public void
	save()
	{
		for (ICompilationUnit icu : this.getCompilationUnits()) {
			try {
				icu.save(npm, true);
			} catch (Exception _) {};
		}
	}

	public IProject
	getProject()
	{
		return this.project;
	}

	public IJavaProject
	getJavaProject()
	{
		return this.java_project;
	}

	public PMProject
	getPMProject()
	{
		return this.pm_project;
	}

	public void
	restore()
	{
		backup.restore();
		this.refresh();
	}

	public void
	updateLaunchConfigurations()
	{
		this.launch_configurations.clear();
		this.generateJUnitLaunchConfigurations();
// 		final DebugPlugin dp = DebugPlugin.getDefault();
		
// 		final ILaunchConfigurationType configType = dp.getLaunchManager().getLaunchConfigurationType(JUnitLaunchConfigurationConstants.ID_JUNIT_APPLICATION);
// 		try {
// 			final ILaunchConfiguration[] configs = dp.getLaunchManager().getLaunchConfigurations(configType);
// 			for (ILaunchConfiguration ilc : configs)
// 				try {
// 					launch_configurations.add(new TUnitLaunchConfig(ilc));
// 				} catch (IllegalArgumentException e) {
// 					System.err.println("Could not add launch configuration " + ilc + ":");
// 					e.printStackTrace();
// 				}
// 		} catch (CoreException _) {}
	}

	public
	TProject(String p, IJavaProject project)
	{
		IWorkspace workspace = org.eclipse.core.resources.ResourcesPlugin.getWorkspace();
		IWorkspaceRoot root = workspace.getRoot();

		init_junit_jar();

		p = root.getLocation().makeAbsolute().toFile().toString();

		if (p.endsWith("/"))
			p = p.substring(0, p.length() - 1);
		this.path_offset = p;
		this.java_project = project;
		this.project = project.getProject();
		this.backup = new ProjectBackup(project);
		this.init();
		for (PMCompilationUnit u : this.pm_project.getPMCompilationUnits()) {
			canonical_cu_order.add(u.getICompilationUnit().getHandleIdentifier());
		}
	}

	private void
	do_copy(String path, IContainer c)
	{
		try {
			File dir = new File(path);

			if (!dir.mkdir()) {
				System.err.println("Could not back up project into `" + path + "'");
				return;
			}

			if (!path.endsWith(File.separator))
				path = path + File.separator;

			for (IResource r : c.members()) {
				if (r instanceof IFile) {
					IFile f = (IFile) r;

					if (!f.getName().endsWith(".java"))
						continue;

					InputStream is = f.getContents();
					byte[] data = new byte[is.available()];
					is.read(data, 0, data.length);

					File outfile = new File(path + f.getName());
					outfile.createNewFile();
					FileOutputStream fos = new FileOutputStream(outfile);
					fos.write(data);
					fos.close();

				} else if (r instanceof IContainer)
					do_copy(path + r.getName(), (IContainer) r);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void
	copyTo(String dest_dir)
	{
		do_copy(dest_dir, this.project);
	}

	private void
	init()
	{
		PMWorkspace.sharedWorkspace().removeProjectForIJavaProject(this.java_project); // ensure refresh
		this.pm_project = PMWorkspace.sharedWorkspace().projectForIJavaProject(this.java_project);
		//		this.pm_project.updateToNewVersionsOfICompilationUnits(true);
	}

	public LinkedList<IType>
	getTypes()
	{
		LinkedList<ICompilationUnit> icus = this.getCompilationUnits();
		LinkedList<IType> types = new LinkedList<IType>();

		for (ICompilationUnit icu : icus)
			try {
				for (IType t : icu.getAllTypes())
					types.add(t);
			} catch (Exception e) {
				e.printStackTrace();
			};

		return types;
	}

	/**
	 * Only yields supertypes that are part of the workspace
	 */
	public LinkedList<IType>
	getSupertypes(IType t)
	{
		LinkedList<IType> supertypes = new LinkedList<IType>();

		try {
			for (IType st : t.newSupertypeHierarchy(npm).getAllSupertypes(t))
				if (!st.isBinary() && !st.equals(t))
					supertypes.add(st);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return supertypes;
	}

	/**
	 * Only yields subtypes that are part of the workspace
	 */
	public LinkedList<IType>
	getSubtypes(IType t)
	{
		LinkedList<IType> subtypes = new LinkedList<IType>();

		try {
			for (IType st : t.newTypeHierarchy(this.java_project, npm).getAllSubtypes(t))
				if (!st.isBinary() && !st.equals(t))
					subtypes.add(st);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return subtypes;
	}

	/**
	 * Only yields subtypes that are part of the workspace
	 */
	public LinkedList<IType>
	getDirectSubtypes(IType t)
	{
		LinkedList<IType> subtypes = new LinkedList<IType>();

		try {
			for (IType st : t.newTypeHierarchy(this.java_project, npm).getSubtypes(t))
				if (!st.isBinary() && !st.equals(t))
					subtypes.add(st);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return subtypes;
	}

	public LinkedList<IField>
	getFields(IType t)
	{
		LinkedList<IField> fl = new LinkedList<IField>();
		try {
			for (IField f : t.getFields())
				fl.add(f);
		} catch (Exception _) {};

		return fl;
	}

	/**
	 * Excludes constructors
	 */
	public LinkedList<IMethod>
	getMethods(IType t)
	{
		LinkedList<IMethod> ml = new LinkedList<IMethod>();

		try {
			for (IMethod m : t.getMethods())
				try {
					if (!m.isConstructor())
						ml.add(m);
				} catch (Exception _) {};
		} catch (Exception _) {};

		return ml;
	}

	public LinkedList<ICompilationUnit>
	getCompilationUnits()
	{
		LinkedList<ICompilationUnit> cu = new LinkedList<ICompilationUnit>();
		for (String cu_name : this.canonical_cu_order) {
			ICompilationUnit next = null;

			for (PMCompilationUnit u : this.pm_project.getPMCompilationUnits())
				
				if (u.getICompilationUnit().getHandleIdentifier().equals(cu_name)) {
					if (next != null)
						throw new RuntimeException("Multiple compilation unit matches on " + cu_name);
					next = u.getICompilationUnit();
					//break; -- disabled for safety check
				}

			if (next == null)
				throw new RuntimeException("No compilation unit match on " + cu_name);

			cu.add(next);
		}

		return cu;
	}

	public boolean
	wasChanged()
	{
		final ProjectBackup fresh_state = new ProjectBackup(this.project);
		final boolean did_change = backup.diff(fresh_state);

		return did_change;
	}

	public ASTNode
	parseCompilationUnit(ICompilationUnit cu)
	{
		return this.pm_project.findASTRootForICompilationUnit(cu);
	}
		
	public void
	refresh()
	{
		try {
			this.project.close(npm);
		} catch (Exception _) {}
		try {
			this.project.refreshLocal(IProject.DEPTH_INFINITE, npm);
		} catch (Exception _) {}
		try {
			this.project.open(0, npm);
		} catch (Exception _) {}
		try {
			this.project.refreshLocal(IProject.DEPTH_INFINITE, npm);
		} catch (Exception _) {}
		init();
	}

	public CompilationUnit
	compilationUnit(ICompilationUnit icu)
	{
		return pm_project.getPMCompilationUnitForICompilationUnit(icu).getASTNode();
	}

	/** Is the program still well-formed and statically sound? */
	public boolean
	staticTestsOK()
	{
		boolean all_ok = true;
		for (PMCompilationUnit u : this.pm_project.getPMCompilationUnits()) {
			CompilationUnit cu = compilationUnit(u.getICompilationUnit());
			if (cu == null) {
				System.err.println("!!! Compilation unit not found: " + u);
				continue;
			}
			IProblem[] problems = cu.getProblems();
			boolean has_errors = false;
			if (problems != null)
				for (IProblem p : problems)
					if (p.isError())
						has_errors = true;

			if (has_errors) {
				System.err.println("Errors in\n" + cu);
				for (IProblem p : problems) {
					if (p.isError()) {
						System.err.println(" - " + p);
						all_ok = false;
					}
				}
			}
		}
		return all_ok;
	}


	public void
	generateJUnitLaunchConfigurations()
	{
		try {
			final IType tcase = this.java_project.findType("junit.framework.TestCase");
			final IType string = this.java_project.findType("java.lang.String");

			if (tcase == null) {
				System.err.println("### COULD NOT FIND TESTCASE");
				return;
			}

			final ITypeHierarchy hierarchy = tcase.newTypeHierarchy(this.java_project, npm);
			final IType[] potential_testcases = hierarchy.getAllSubtypes(tcase);

			final LinkedList<IType> testcases = new LinkedList<IType>();

			System.err.println("### Identified " + testcases.size() + " test case candidates");

			for (IType t : potential_testcases) {
				if (DEBUG_TESTS)
					System.err.print("### " + t);

				if (Flags.isAbstract(t.getFlags())) {
					if (DEBUG_TESTS)
						System.err.println(" REJECTED (abstract)");
					continue;
				}

				if (t.isMember()
				    && !Flags.isStatic(t.getFlags())) {
					if (DEBUG_TESTS)
						System.err.println(" REJECTED (nonstatic member)");
					continue;
				}

				if (t.getCompilationUnit() == null) {
					if (DEBUG_TESTS)
						System.err.println(" REJECTED (null compilation unit)");
					continue;
				}

				boolean is_testcase = false;
				boolean has_public_constructor = false;
				boolean has_constructor = false;

				for (IMethod m : t.getMethods()) {
					if (m.isConstructor())
						has_constructor = true;

					if (Flags.isPublic(m.getFlags())) {
						if (m.getElementName().startsWith("test"))
							is_testcase = true;
						if (m.isConstructor()) {
							final String[] parameters = m.getParameterTypes();
							if (parameters.length == 0)
								has_public_constructor = true;
							if (parameters.length == 1
							    && parameters[0].equals("QString;"))
								has_public_constructor = true;
						}
					}
				}
					

				if (!is_testcase) {
					if (DEBUG_TESTS)
						System.err.println(" REJECTED (contains no test methods)");
					continue;
				}

				if (has_constructor && !has_public_constructor) {
					if (DEBUG_TESTS)
						System.err.println(" REJECTED (no suitable public constructor)");
					continue;
				}

				if (DEBUG_TESTS)
					System.err.println("OK");
				testcases.add(t);
			}

			System.err.println("### Identified " + testcases.size() + " testcases");
			for (IType t : testcases) {
				//System.err.println(" - " + t + " from " + t.getCompilationUnit());
				this.launch_configurations.add(new TUnitLaunchConfig(t));
			}
		} catch (JavaModelException e) {
			System.err.println("### Aborting test case search");
			e.printStackTrace();
		}
	}

	public boolean
	unitTestsOK()
	{
		if (!staticTestsOK())
			return false;

		int test_count = 0;

		this.updateLaunchConfigurations();

		System.err.println("[UnitTest] Total of " + launch_configurations.size() + " launch test configurations");

		compile();

		for (TUnitLaunchConfig lconfig : this.launch_configurations) {
			System.err.println("[T:" + lconfig + "] Testing launch config: " + lconfig + ")");
			if (lconfig.isValid()) {
				if (!lconfig.run())
					return false;
				++test_count;
			} else System.err.println("[T:" + lconfig + "] invalid");
		}

		if (test_count == 0)
			System.err.println("[UnitTest] Could not run ANY tests, failing.");
		return test_count > 0;
	}


	private static final void dump_all_resources(IContainer c)
	{
		System.out.println (" - " + c);
		try {
		for (IResource r : c.members())
			if (r instanceof IContainer)
				dump_all_resources((IContainer) r);
			else
				System.out.println (" - " + r);
		} catch (CoreException _) {}
	}


	protected void
	compile()
	{
		final IProject p = project;
		try {
			p.build(IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}

	public void
	renameClass(IType type, String new_name_suffix)
	{
// 		String old_name = type.getFullyQualifiedName();
// 		String[] old_name_parts = old_name.split("\\.");
// 		String suffix = old_name_parts[old_name_parts.length - 1];
// 		String prefix = old_name.substring(0, old_name.length() - suffix.length());
// 		String new_name = prefix + new_name_suffix;

// 		if (DEBUG_CLONE_WITH_RENAME)
// 			System.err.println("Renaming type " + old_name + " to " + new_name);

// 		LinkedList<TUnitLaunchConfig> additions = new LinkedList<TUnitLaunchConfig>();

// 		for (TUnitLaunchConfig c : launch_configurations) {
// 			if (c.getMainClass().equals(old_name))
// 				additions.add(c.cloneWithRename(new_name));
// 			else if (DEBUG_CLONE_WITH_RENAME)
// 				System.err.println("  % " + old_name + " <> " + c.getMainClass());
// 		}

// 		if (DEBUG_CLONE_WITH_RENAME)
// 			System.err.println("Found " + additions.size() + " types to replace.");

// 		for (TUnitLaunchConfig addition : additions)
// 			launch_configurations.add(addition);
	}

	public CodeGenerationSettings
	getCodeGenerationSettings()
	{
		return JavaPreferencesSettings.getCodeGenerationSettings(this.java_project);
	}

	private class TUnitLaunchConfig
	{
		private String main_class_resource;
		private String main_class;
		private String [] classpath;
		private String wdir;
		private List<String> required_resources;

		private String
		build_resource_from_type(String type)
		{
			return project.getFile(type.replaceAll("\\.", "/")).getFullPath().toString() + ".java";
		}

		public String
		getMainClass()
		{
			return this.main_class;
		}

		public String
		getMainResource()
		{
			return this.main_class_resource;
		}

		public TUnitLaunchConfig(IType t)
		{
			try {
				final IClasspathEntry [] classpath_entries = java_project.getResolvedClasspath(true);
				final IPath output_classfiles_location = java_project.getOutputLocation();
				this.classpath = new String[classpath_entries.length + 1];
				this.classpath[0] = path_offset + output_classfiles_location.makeAbsolute().toFile().toString();

				for (int i = 0; i < classpath_entries.length; i++)
					this.classpath[i+1] = path_offset + classpath_entries[i].getPath().makeAbsolute().toFile().toString();

				this.wdir = path_offset + java_project.getPath().makeAbsolute().toFile().toString();

				this.required_resources = new LinkedList<String>(); // no longer needed
				this.main_class = t.getFullyQualifiedName();
			} catch (JavaModelException e) {
				throw new RuntimeException(e);
			}
		}

		public TUnitLaunchConfig(ILaunchConfiguration lconf)
		{
			try {
				final IProgressMonitor progress_monitor = new NullProgressMonitor();
				final ILaunchConfigurationWorkingCopy lc = lconf.getWorkingCopy();
				final ILaunchConfigurationType type = lc.getType();
				final JUnitLaunchConfigurationDelegate delegate = new JUnitLaunchConfigurationDelegate();

				this.classpath = delegate.getClasspath(lc);
				this.wdir = this.classpath[0];
				this.required_resources = (List<String>) lconf.getAttribute("org.eclipse.debug.core.MAPPED_RESOURCE_PATHS", (java.util.List) null);
				this.main_class = (String) lconf.getAttribute("org.eclipse.jdt.launching.MAIN_TYPE", (java.lang.String) null);
				this.main_class_resource = build_resource_from_type(this.main_class);
				//List vm_args = (List) lconf.getAttribute("org.eclipse.jdt.launching.VM_ARGUMENTS", (java.util.List) null);
				// FIXME:  Once we observe a use of vm_args, integrate them.

				if (DEBUG_CLONE_WITH_RENAME) {
					System.err.println("New test launch configuration based on `" + main_class_resource + "' depends on:");
					for (Object o : required_resources)
						System.err.println(" - " + o);
				}

			} catch (CoreException e) {
				throw new RuntimeException(e);
			}
		}

		private TUnitLaunchConfig(String mc, String[] cp, List<String> rr)
		{
			this.main_class = mc;
			this.classpath = cp;
			this.wdir = cp[0];
			this.required_resources = rr;
			this.main_class_resource = build_resource_from_type(this.main_class);
		}

		public TUnitLaunchConfig
		cloneWithRename(String new_main_class)
		{
			String new_main_resource = build_resource_from_type(new_main_class);

			List<String> new_required_resources = new LinkedList<String>();

			for (String s : this.required_resources) {
				if (s.equals(this.main_class_resource))
					new_required_resources.add(new_main_resource);
				else
					new_required_resources.add(s);
			}

			if (DEBUG_CLONE_WITH_RENAME) {
				System.err.println("Adding cloned new tester.  Old resources:");
				for (String s : this.required_resources)
					System.err.println(" - `" + s + "'");

				System.err.println("New resources (after replacing `" + this.main_class_resource + "' with `" + new_main_resource + "'):");

				for (String s : new_required_resources)
					System.err.println(" - `" + s + "'");
			}

			return new TUnitLaunchConfig(this.main_class, this.classpath, new_required_resources);
		}

		public boolean
		isValid()
		{
			if (this.main_class_resource != null) {
				boolean main_class_occurs = false;
				System.err.println("[T:" + this + "] Checking for main class resource `" +main_class_resource + "'");

				for (Object o : required_resources) {
					String resource_name = (String) o;
					IFile f = project.getFile(resource_name);
					f = project.getFile(f.getFullPath().removeFirstSegments(2));

					if (f.getFullPath().toString().equals(main_class_resource)
					    || resource_name.equals(main_class_resource))
						main_class_occurs = true;
					File file = new File(f.getLocationURI().getRawPath());
					System.err.println(resource_name + "(" + f + ") has file size " + file.length());
					if (!f.exists() || f.isPhantom() || !file.exists() || file.length() < 1) {
						System.err.println("[T:" + this + "] => Skipping test since resource IFile `" + f + "' (from `" + resource_name + "', URI-path " + f.getLocationURI().getRawPath() + ") is missing.  Resources we have are: ");
						dump_all_resources(project);
						return false;
					}
				}

				/* Broken build configuration? */
				if (!main_class_occurs) {
					System.err.println("[T:" + this + "] => Main class resource not found");
					return false;
				}
			}

			return true;
		}

		public boolean
		run()
		{
			String cpstring = "";
			for (int i = 0; i < classpath.length; i++) {
				if (i > 0)
					cpstring = cpstring + ":";
				cpstring = cpstring + classpath[i];
			}

			cpstring = cpstring + ":" + junit_jar;

			String [] args = new String[5];
			args[0] = "java";
			args[1] = "-classpath";
			args[2] = cpstring;
			args[3] = "junit.textui.TestRunner";
			args[4] = main_class;

			System.err.println(" <=> Running unit test with classpath `" + cpstring + "' for main class `" + main_class + "' at `" + this.wdir + "'");

			File cwd_file = new File(wdir);

			try {
				int retval = DebugPlugin.exec(args, cwd_file).waitFor();
				System.err.println(" => " + retval);
				return retval == 0;
			} catch (Exception e) {
				System.err.println("[T:" + this + ":run] Interrupted");
				e.printStackTrace();
				return false;
			}
		}

		public int
		hashCode()
		{
			return main_class.hashCode();
		}

		public boolean
		equals(Object o)
		{
			if (o instanceof TUnitLaunchConfig) {
				TUnitLaunchConfig other = (TUnitLaunchConfig) o;
				if (!this.main_class_resource.equals(other.main_class_resource))
					return false;
				if (!this.main_class.equals(other.main_class))
					return false;
				if (this.classpath.length != other.classpath.length)
					return false;

				for (int i = 0; i < this.classpath.length; i++)
					if (!this.classpath[i].equals(other.classpath[i]))
						return false;

				if (!this.required_resources.equals(other.required_resources))
					return false;

				return true;
			} else
				return false;
		}

		public String
		toString()
		{
			return main_class;
		}
	}
}
