/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

*******************************************************************************/

package net.creichen.pm.tests;

import net.creichen.pm.PMWorkspace;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
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

public class PMTest {
	protected IProject _iProject = null;
	protected IJavaProject _iJavaProject = null;
	
	@After public void deleteProject() {
		try {
			
			PMWorkspace.sharedWorkspace().removeProjectForIJavaProject(_iJavaProject);
			
			
			_iProject.delete(true, null);		
		} catch (Exception e) {
			e.printStackTrace();
			
			throw new RuntimeException(e);
		}
	
		_iProject = null;
		_iJavaProject = null;
	}
	
	@Before public void createProject() {
		try {
			IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot(); 
			_iProject = root.getProject("pm_test_project_name"); 
			
			if (_iProject.exists()) {
				_iProject.delete(true, null);
			}
			
			_iProject.create(null); 
			_iProject.open(null); 
		
			IProjectDescription description = _iProject.getDescription(); 
			description.setNatureIds(new String[] { JavaCore.NATURE_ID }); 
			_iProject.setDescription(description, null); 
			
			_iJavaProject = JavaCore.create(_iProject); 
			 
			IClasspathEntry[] buildPath= { 
			JavaCore.newSourceEntry(_iProject.getFullPath().append("src")), 
			JavaRuntime.getDefaultJREContainerEntry() 
			}; 
			
			_iJavaProject.setRawClasspath(buildPath, _iProject.getFullPath().append("bin"), null);

			
			
			
		} catch (Exception e) {
			e.printStackTrace();
			
			throw new RuntimeException(e);
		}
	}
	
	public ICompilationUnit createNewCompilationUnit(String packageFragmentName, String fileName, String sourceText) {
		
		ICompilationUnit result = null;
		
		try {
			IFolder folder = _iProject.getFolder("src"); 
			
			if (!folder.exists())
				folder.create(true, true, null); 
			
			IPackageFragmentRoot srcFolder = _iJavaProject.getPackageFragmentRoot(folder); 
			
			Assert.assertTrue(srcFolder.exists()); 
			
			
			IPackageFragment fragment = srcFolder.createPackageFragment(packageFragmentName, true, null); 
			
			result = fragment.createCompilationUnit(fileName, sourceText, false, null); 
			
			
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		
	

		return result;
	}
	
	public CompilationUnit parseCompilationUnitFromSource(String source, String unitName) {
		
		ASTParser parser = ASTParser.newParser(AST.JLS3); 
		
		if (unitName != null) {
			ICompilationUnit iCompilationUnit = createNewCompilationUnit("", unitName, source);
			
			parser.setSource(iCompilationUnit);
		} else {
			parser.setSource(source.toCharArray());
		}
		
		
		
		parser.setResolveBindings(true);
		parser.setUnitName(unitName);
		parser.setProject(_iJavaProject);
		
		return (CompilationUnit) parser.createAST(null);
	}
	
	public CompilationUnit parseCompilationUnitFromSource(String source) {
		return parseCompilationUnitFromSource(source, null);
	}
	
	public boolean compilationUnitSourceMatchesSource(String source1, String source2) {
		CompilationUnit compilationUnit1 = parseCompilationUnitFromSource(source1);	
		CompilationUnit compilationUnit2 = parseCompilationUnitFromSource(source2);
		
		return compilationUnit1.subtreeMatch(new ASTMatcher(), compilationUnit2);
	}

}
