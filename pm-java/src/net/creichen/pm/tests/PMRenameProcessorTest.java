/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

*******************************************************************************/

package net.creichen.pm.tests;

import static org.junit.Assert.*;

import java.util.Set;






import net.creichen.pm.PMASTQuery;
import net.creichen.pm.PMProject;
import net.creichen.pm.PMRenameProcessor;
import net.creichen.pm.PMWorkspace;
import net.creichen.pm.inconsistencies.PMInconsistency;
import net.creichen.pm.inconsistencies.PMMissingDefinition;
import net.creichen.pm.inconsistencies.PMNameCapture;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.junit.Test;
import org.eclipse.jface.text.TextSelection;


//Note: org.junit4 must be in your plugin dependencies
//otherwise you will get a "No runnable methods" error
//
public class PMRenameProcessorTest extends PMTest {
	
	
	

	
	

	

	
	@Test public void testCreateNewCompilationUnitInTest() {
		ICompilationUnit iCompilationUnit = createNewCompilationUnit("", "Foo.java", "public class Foo {}");
		
		PMProject pmProject = PMWorkspace.sharedWorkspace().projectForIJavaProject(_iJavaProject);
		
		assertTrue(pmProject.getPMCompilationUnitForICompilationUnit(iCompilationUnit) != null);
		
	}
	
	@Test public void testRenameLocalVariableViaDeclaration() throws JavaModelException {

		ICompilationUnit iCompilationUnit = createNewCompilationUnit("", "Foo.java", "public class Foo {void method() {int foo; foo = 5;} }");
		
				
		PMRenameProcessor renameFooToStan = new PMRenameProcessor(new TextSelection(37, 3), iCompilationUnit);
		
		renameFooToStan.setNewName("stan");
		
		PMProcessorDriver.drive(renameFooToStan);
		
		assertEquals("public class Foo {void method() {int stan; stan = 5;} }", iCompilationUnit.getSource());
		
		PMRenameProcessor renameStanToBar = new PMRenameProcessor(new TextSelection(37, 4), iCompilationUnit);
				
		renameStanToBar.setNewName("bar");
		
		PMProcessorDriver.drive(renameStanToBar);
		
		assertEquals("public class Foo {void method() {int bar; bar = 5;} }", iCompilationUnit.getSource());		
	}
	
	@Test public void testRenameLocalVariableViaUse() throws JavaModelException {

		ICompilationUnit iCompilationUnit = createNewCompilationUnit("", "Foo.java", "public class Foo {void method() {int foo; foo = 5;} }");
				
		PMRenameProcessor renameFooToStan = new PMRenameProcessor(new TextSelection(42, 3), iCompilationUnit);
		
		renameFooToStan.setNewName("stan");
		
		PMProcessorDriver.drive(renameFooToStan);
		
		assertEquals("public class Foo {void method() {int stan; stan = 5;} }", iCompilationUnit.getSource());
		
		PMRenameProcessor renameStanToBar = new PMRenameProcessor(new TextSelection(43, 4), iCompilationUnit);
		
		renameStanToBar.setNewName("bar");
		
		PMProcessorDriver.drive(renameStanToBar);
		
		assertEquals("public class Foo {void method() {int bar; bar = 5;} }", iCompilationUnit.getSource());		
	}
	
	@Test public void testRenameIVarCapturesLocal() throws JavaModelException {
		ICompilationUnit iCompilationUnit = createNewCompilationUnit("", "Foo.java", "public class Foo {int foo; void method() {int bar; foo = 5;} }");
		
		PMProject pmProject = PMWorkspace.sharedWorkspace().projectForIJavaProject(_iJavaProject);
		
				
		PMRenameProcessor renameIvarToBar = new PMRenameProcessor(new TextSelection(22, 3), iCompilationUnit);
		
		renameIvarToBar.setNewName("bar");
		
		PMProcessorDriver.drive(renameIvarToBar);
		
		assertEquals("Foo.java", "public class Foo {int bar; void method() {int bar; bar = 5;} }", iCompilationUnit.getSource());
		
		Set<PMInconsistency> inconsistencies = pmProject.allInconsistencies();
		
		
		
		
		assertEquals(1, inconsistencies.size());
		
		PMNameCapture nameCapture = (PMNameCapture)inconsistencies.toArray()[0];
		
		CompilationUnit parsedCompilationUnit = pmProject.parsedCompilationUnitForICompilationUnit(iCompilationUnit);
		
		ASTNode expectedCapturedNode = PMASTQuery.simpleNameWithIdentifierInMethodInClassInCompilationUnit("bar", 1, "method", 0, "Foo", 0, parsedCompilationUnit);
		assertEquals(expectedCapturedNode, nameCapture.getNode());
		
		
		//ASTNode expectedExpectedDeclaringNode = PMASTQuery.fieldWithNameInClassInCompilationUnit("bar", 0, "Foo", 0, parsedCompilationUnit);
		
		//ASTNode actualExpectedDeclaringNode = nameCapture.getExpectedDeclaration();
		
		
		//!!! we don't actuall provide this in the implementation yet
		// should probably leave failing test in, but . . .
		//assertEquals(expectedExpectedDeclaringNode, actualExpectedDeclaringNode);
		
		ASTNode expectedCapturingNode = PMASTQuery.simpleNameWithIdentifierInMethodInClassInCompilationUnit("bar", 0, "method", 0, "Foo", 0, parsedCompilationUnit).getParent();
		
		ASTNode actualCapturingNode = nameCapture.getActualDeclaration();
		
		assertEquals(expectedCapturingNode, actualCapturingNode);

	}
	
	
	@Test public void testRenameThroughCapture() throws JavaModelException {
		ICompilationUnit iCompilationUnit = createNewCompilationUnit("", "Foo.java", "public class Foo {void method() {int foo; int bar; foo = 5; bar = 6;} }");
		
		PMProject pmProject = PMWorkspace.sharedWorkspace().projectForIJavaProject(_iJavaProject);
		
				
		PMRenameProcessor renameFooToBar = new PMRenameProcessor(new TextSelection(37, 3), iCompilationUnit);
		
		renameFooToBar.setNewName("bar");
		
		PMProcessorDriver.drive(renameFooToBar);
		
		assertEquals("public class Foo {void method() {int bar; int bar; bar = 5; bar = 6;} }", iCompilationUnit.getSource());
		
		Set<PMInconsistency> inconsistencies = pmProject.allInconsistencies();
		
		assertTrue(inconsistencies.size() == 1);
		
		PMNameCapture nameCapture = (PMNameCapture)inconsistencies.toArray()[0];
		
		CompilationUnit parsedCompilationUnit = pmProject.parsedCompilationUnitForICompilationUnit(iCompilationUnit);
		
		ASTNode expectedCapturedNode = pmProject.nodeForSelection(new TextSelection(51, 3), iCompilationUnit);
		assertEquals(expectedCapturedNode, nameCapture.getNode());
		
		ASTNode expectedCapturingNode = PMASTQuery.simpleNameWithIdentifierInMethodInClassInCompilationUnit("bar", 1, "method", 0, "Foo", 0, parsedCompilationUnit).getParent();
		
		ASTNode actualCapturingNode = nameCapture.getActualDeclaration();
		
		assertEquals(expectedCapturingNode, actualCapturingNode);
		
		
		PMRenameProcessor renameBarToFoo = new PMRenameProcessor(new TextSelection(60, 3), iCompilationUnit);
		
		renameBarToFoo.setNewName("foo");
		
		PMProcessorDriver.drive(renameBarToFoo);
		
		assertEquals("public class Foo {void method() {int bar; int foo; bar = 5; foo = 6;} }", iCompilationUnit.getSource());
		
		inconsistencies = pmProject.allInconsistencies();
		
		assertTrue(inconsistencies.size() == 0);
		
	}
	
	@Test public void testRenameInMultipleFiles() throws JavaModelException {
		
		
		ICompilationUnit unit1 = createNewCompilationUnit("", "Unit1.java", "public class Unit1 { public int x; void method() {x++;} }");
		ICompilationUnit unit2 = createNewCompilationUnit("", "Unit2.java", "public class Unit2 { void method() {Unit1 unit1 = new Unit1(); unit1.x--;} }");
	

		
		PMProject pmProject = PMWorkspace.sharedWorkspace().projectForIJavaProject(_iJavaProject);
		
		
		PMRenameProcessor renameXToY = new PMRenameProcessor(new TextSelection(32, 1), unit1);
		
		renameXToY.setNewName("y");
		

		
		PMProcessorDriver.drive(renameXToY);
		
		assertEquals("public class Unit1 { public int y; void method() {y++;} }", unit1.getSource());
		assertEquals("public class Unit2 { void method() {Unit1 unit1 = new Unit1(); unit1.y--;} }", unit2.getSource());

		
		for (PMInconsistency inconsistency: pmProject.allInconsistencies()) {
			System.out.println(inconsistency.getHumanReadableDescription());
			
			if (inconsistency instanceof PMMissingDefinition)
			System.out.println("For definition of class: " + ((PMMissingDefinition)inconsistency).getDefiningNode().getClass());
		}
		
		assertTrue(pmProject.allInconsistencies().size() == 0);
		
	}

	@Test public void testAcceptNameCapture() throws JavaModelException {
		ICompilationUnit iCompilationUnit = createNewCompilationUnit("", "Foo.java", "public class Foo {int foo; void method() {int bar; foo = 5;} }");
		
		PMProject pmProject = PMWorkspace.sharedWorkspace().projectForIJavaProject(_iJavaProject);
		
				
		PMRenameProcessor renameIvarToBar = new PMRenameProcessor(new TextSelection(22, 3), iCompilationUnit);
		
		renameIvarToBar.setNewName("bar");
		
		PMProcessorDriver.drive(renameIvarToBar);
		
		assertEquals("Foo.java", "public class Foo {int bar; void method() {int bar; bar = 5;} }", iCompilationUnit.getSource());
		
		Set<PMInconsistency> inconsistencies = pmProject.allInconsistencies();
		
		
		
		
		assertTrue(inconsistencies.size() == 1);
		
		PMNameCapture nameCapture = (PMNameCapture)inconsistencies.toArray()[0];
		
		CompilationUnit parsedCompilationUnit = pmProject.parsedCompilationUnitForICompilationUnit(iCompilationUnit);
		
		ASTNode expectedCapturedNode = PMASTQuery.simpleNameWithIdentifierInMethodInClassInCompilationUnit("bar", 1, "method", 0, "Foo", 0, parsedCompilationUnit);
		assertEquals(expectedCapturedNode, nameCapture.getNode());
		
		
		nameCapture.acceptBehavioralChange();
		
		
		Set<PMInconsistency> newInconsistencies = pmProject.allInconsistencies();
		
		assertEquals(0, newInconsistencies.size());
	}
	
}
