/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm;

import net.creichen.pm.Workspace;

import org.eclipse.core.resources.*;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTMatcher;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.launching.JavaRuntime;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;

public class PMTest {
    private IProject iProject = null;
    private IJavaProject iJavaProject = null;

    public boolean compilationUnitSourceMatchesSource(final String source1, final String source2) {
        final CompilationUnit compilationUnit1 = toCompilationUnit(source1);
        final CompilationUnit compilationUnit2 = toCompilationUnit(source2);

        return compilationUnit1.subtreeMatch(new ASTMatcher(), compilationUnit2);
    }

    public ICompilationUnit createNewCompilationUnit(final String packageFragmentName,
            final String fileName, final String sourceText) {

        ICompilationUnit result = null;

        try {
            final IFolder folder = this.iProject.getFolder("src");

            if (!folder.exists()) {
                folder.create(true, true, null);
            }

            final IPackageFragmentRoot srcFolder = this.iJavaProject.getPackageFragmentRoot(folder);

            Assert.assertTrue(srcFolder.exists());

            final IPackageFragment fragment = srcFolder.createPackageFragment(packageFragmentName,
                    true, null);

            result = fragment.createCompilationUnit(fileName, sourceText, false, null);

        } catch (final Exception e) {
            throw new RuntimeException(e);
        }

        return result;
    }

    @Before
    public void createProject() {
        try {
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

            final IClasspathEntry[] buildPath = {
                    JavaCore.newSourceEntry(this.iProject.getFullPath().append("src")),
                    JavaRuntime.getDefaultJREContainerEntry() };

            this.iJavaProject.setRawClasspath(buildPath, this.iProject.getFullPath().append("bin"),
                    null);

        } catch (final Exception e) {
            e.printStackTrace();

            throw new RuntimeException(e);
        }
    }

    @After
    public void deleteProject() {
        try {

            Workspace.sharedWorkspace().removeProjectForIJavaProject(this.iJavaProject);

            this.iProject.delete(true, null);
        } catch (final Exception e) {
            e.printStackTrace();

            throw new RuntimeException(e);
        }

        this.iProject = null;
        this.iJavaProject = null;
    }

    protected IJavaProject getIJavaProject() {
        return this.iJavaProject;
    }

    public CompilationUnit toCompilationUnit(final String source) {
        return parseCompilationUnitFromSource(source, null);
    }

    public CompilationUnit parseCompilationUnitFromSource(final String source, final String unitName) {

        final ASTParser parser = ASTParser.newParser(AST.JLS4);

        if (unitName != null) {
            final ICompilationUnit iCompilationUnit = createNewCompilationUnit("", unitName, source);

            parser.setSource(iCompilationUnit);
        } else {
            parser.setSource(source.toCharArray());
        }

        parser.setResolveBindings(true);
        parser.setUnitName(unitName);
        parser.setProject(this.iJavaProject);

        return (CompilationUnit) parser.createAST(null);
    }

}
