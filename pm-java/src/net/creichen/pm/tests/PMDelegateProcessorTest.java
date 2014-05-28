/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

*******************************************************************************/

package net.creichen.pm.tests;



import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import java.util.Set;

import net.creichen.pm.PMDelegateProcessor;
import net.creichen.pm.PMProject;
import net.creichen.pm.PMWorkspace;
import net.creichen.pm.inconsistencies.PMInconsistency;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.junit.Test;

public class PMDelegateProcessorTest extends PMTest {
	@Test public void testAddDelegateToSimpleMethodInvocation() throws JavaModelException {
		String source = "public class S {void m(){S s = new S();s.getClass(); m();}}";
		
		ICompilationUnit compilationUnit = createNewCompilationUnit("", "S.java", source);

		
		PMDelegateProcessor delegateProcessor = new PMDelegateProcessor(new TextSelection(79 - 26, 3), compilationUnit);
		
		delegateProcessor.setDelegateIdentifier("s");
		
		PMProject pmProject = PMWorkspace.sharedWorkspace().projectForIJavaProject(_iJavaProject);
	
		
		PMProcessorDriver.drive(delegateProcessor);
		
				
		assertTrue(compilationUnitSourceMatchesSource("public class S {void m(){S s = new S();s.getClass(); s.m();}}", compilationUnit.getSource()));
	

		Set<PMInconsistency> inconsistencies = pmProject.allInconsistencies();

		
		
		assertEquals(0, inconsistencies.size());
	}
	
	@Test public void testAddSuperDelegateToSimpleMethodInvocation() throws JavaModelException {
		String source = "public class S {void m(){S s; m();}}";
		
		ICompilationUnit compilationUnit = createNewCompilationUnit("", "S.java", source);

		
		PMDelegateProcessor delegateProcessor = new PMDelegateProcessor(new TextSelection(56 - 26, 59 - 56), compilationUnit);
		
		delegateProcessor.setDelegateIdentifier("super");
		
		PMProject pmProject = PMWorkspace.sharedWorkspace().projectForIJavaProject(_iJavaProject);
	
		
		PMProcessorDriver.drive(delegateProcessor);
		
				
		assertTrue(compilationUnitSourceMatchesSource("public class S {void m(){S s; super.m();}}", compilationUnit.getSource()));
	

		Set<PMInconsistency> inconsistencies = pmProject.allInconsistencies();

				
		assertEquals(0, inconsistencies.size());
	}
	
	
	@Test public void testRemoveDelegateFromSimpleMethodInvocation() throws JavaModelException {
		String source = "public class S {void m(){S s; s.m();}}";
		
		ICompilationUnit compilationUnit = createNewCompilationUnit("", "S.java", source);

		
		PMDelegateProcessor delegateProcessor = new PMDelegateProcessor(new TextSelection(56 - 26, 61 - 56), compilationUnit);
		
		delegateProcessor.setDelegateIdentifier("");
		
		PMProject pmProject = PMWorkspace.sharedWorkspace().projectForIJavaProject(_iJavaProject);
	
		
		RefactoringStatus status = PMProcessorDriver.drive(delegateProcessor);
		
				assertTrue(status.getSeverity() < RefactoringStatus.ERROR);
		
		
				
		assertTrue(compilationUnitSourceMatchesSource("public class S {void m(){S s; m();}}", compilationUnit.getSource()));
	

		Set<PMInconsistency> inconsistencies = pmProject.allInconsistencies();

				
		assertEquals(0, inconsistencies.size());
	}
	
	@Test public void testDelegateToField() throws JavaModelException {
		String source = "public class S {S s; void m(){s.getClass(); m();}}";
		
		ICompilationUnit compilationUnit = createNewCompilationUnit("", "S.java", source);

		
		PMDelegateProcessor delegateProcessor = new PMDelegateProcessor(new TextSelection(70 - 26, 3), compilationUnit);
		
		delegateProcessor.setDelegateIdentifier("s");
		
		PMProject pmProject = PMWorkspace.sharedWorkspace().projectForIJavaProject(_iJavaProject);
	
		
		PMProcessorDriver.drive(delegateProcessor);
		
				
		assertTrue(compilationUnitSourceMatchesSource("public class S {S s; void m(){s.getClass(); s.m();}}", compilationUnit.getSource()));
	

		Set<PMInconsistency> inconsistencies = pmProject.allInconsistencies();

				
		assertEquals(0, inconsistencies.size());
	}
	
	@Test public void testDelegateToFieldInSuperClassWithPackages() throws JavaModelException {
		//String superSource = "package t; public class Super {Super s; void m() { } }";
		
		String subSource = "package t; public class Sub extends Super {void g() {m();}}";
		
		//ICompilationUnit superCompilationUnit = createNewCompilationUnit("t", "Super.java", superSource);
		ICompilationUnit subCompilationUnit = createNewCompilationUnit("t", "Sub.java", subSource);
		
		PMDelegateProcessor delegateProcessor = new PMDelegateProcessor(new TextSelection(82 - 29, 3), subCompilationUnit);
		
		delegateProcessor.setDelegateIdentifier("s");
		
		PMProject pmProject = PMWorkspace.sharedWorkspace().projectForIJavaProject(_iJavaProject);
	
		
		PMProcessorDriver.drive(delegateProcessor);
				
		assertTrue(compilationUnitSourceMatchesSource("package t; public class Sub extends Super {void g() {s.m();}}", subCompilationUnit.getSource()));
	

		Set<PMInconsistency> inconsistencies = pmProject.allInconsistencies();

				
		assertEquals(0, inconsistencies.size());
	}
}
