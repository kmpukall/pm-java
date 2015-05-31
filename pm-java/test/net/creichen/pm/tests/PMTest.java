/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm.tests;

import net.creichen.pm.consistency.ConsistencyValidator;
import net.creichen.pm.core.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.launching.JavaRuntime;
import org.junit.*;

public abstract class PMTest {
    private IProject iProject = null;
    private IJavaProject iJavaProject = null;

    protected Project getProject() {
        return Workspace.getInstance().getProject(this.iJavaProject);
    }

    @Before
    public void before() throws CoreException {
        final IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        this.iProject = root.getProject("pm_test_project_name");
        if (this.iProject.exists()) {
            this.iProject.delete(true, null);
        }
        this.iProject.create(null);
        this.iProject.open(null);
        final IProjectDescription description = this.iProject.getDescription();
        description.setNatureIds(new String[] { JavaCore.NATURE_ID });
        this.iProject.setDescription(description, null);
        this.iJavaProject = JavaCore.create(this.iProject);
        final IClasspathEntry[] buildPath = { JavaCore.newSourceEntry(this.iProject.getFullPath().append("src")),
                JavaRuntime.getDefaultJREContainerEntry() };
        this.iJavaProject.setRawClasspath(buildPath, this.iProject.getFullPath().append("bin"), null);

        ConsistencyValidator.getInstance().reset();
    }

    protected ICompilationUnit createCompilationUnit(final String packageFragmentName, final String fileName,
            final String sourceText) {
        ICompilationUnit result = null;
        try {
            final IFolder folder = this.iProject.getFolder("src");
            if (!folder.exists()) {
                folder.create(true, true, null);
            }
            final IPackageFragmentRoot srcFolder = this.iJavaProject.getPackageFragmentRoot(folder);
            Assert.assertTrue(srcFolder.exists());
            final IPackageFragment fragment = srcFolder.createPackageFragment(packageFragmentName, true, null);
            result = fragment.createCompilationUnit(fileName, sourceText, false, null);
        } catch (final CoreException e) {
            throw new PMException(e);
        }

        return result;
    }

    @After
    public final void after() throws CoreException {
        Workspace.getInstance().removeProject(this.iJavaProject);
        this.iProject.delete(true, null);
        this.iProject = null;
        this.iJavaProject = null;
    }

}
