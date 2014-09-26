/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm.tests;

import net.creichen.pm.consistency.ConsistencyValidator;
import net.creichen.pm.core.Project;
import net.creichen.pm.core.Workspace;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTMatcher;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.launching.JavaRuntime;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;

public abstract class PMTest {
    private IProject iProject = null;
    private IJavaProject iJavaProject = null;

    protected Project getProject() {
        return Workspace.getInstance().getProject(getIJavaProject());
    }

    @After
    public final void after() throws CoreException {
        deleteProject();
        tearDown();
    }

    @Before
    public void before() throws CoreException {
        createProject();
        setUp();
    }

    private void createProject() throws CoreException {
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
    }

    private void deleteProject() throws CoreException {
        ConsistencyValidator.getInstance().reset();
        Workspace.getInstance().removeProject(this.iJavaProject);
        this.iProject.delete(true, null);
        this.iProject = null;
        this.iJavaProject = null;
    }

    protected boolean matchesSource(final String source1, final String source2) {
        final CompilationUnit compilationUnit1 = toCompilationUnit(source1);
        final CompilationUnit compilationUnit2 = toCompilationUnit(source2);
        return compilationUnit1.subtreeMatch(new ASTMatcher(), compilationUnit2);
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
            throw new RuntimeException(e);
        }

        return result;
    }

    protected final IJavaProject getIJavaProject() {
        return this.iJavaProject;
    }

    protected CompilationUnit parseCompilationUnitFromSource(final String source, final String unitName) {
        final ASTParser parser = ASTParser.newParser(AST.JLS4);
        if (unitName != null) {
            final ICompilationUnit iCompilationUnit = createCompilationUnit("", unitName, source);
            parser.setSource(iCompilationUnit);
        } else {
            parser.setSource(source.toCharArray());
        }
        parser.setResolveBindings(true);
        parser.setUnitName(unitName);
        parser.setProject(this.iJavaProject);
        return (CompilationUnit) parser.createAST(null);
    }

    protected void setUp() {
        // can be overwritten by subclasses
    }

    protected void tearDown() {
        // can be overwritten by subclasses
    }

    protected CompilationUnit toCompilationUnit(final String source) {
        return parseCompilationUnitFromSource(source, null);
    }
}
