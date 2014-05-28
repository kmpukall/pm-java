/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

*******************************************************************************/

package net.creichen.pm.selection;

import net.creichen.pm.selection.PMInsertionPoint;
import net.creichen.pm.selection.PMSelection;
import net.creichen.pm.tests.PMTest;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.WhileStatement;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertFalse;

public class PMSelectionTest extends PMTest {

	@Test public void testSelectStatement() {
		String source = "class S {int x,y; int f() {int x,y; while(1) {x = 5; y = x +1;} } }";
		
		CompilationUnit compilationUnit = parseCompilationUnitFromSource(source, "S.java");
		
		PMSelection whileSelection = new PMSelection(compilationUnit, 62 - 26, 89 - 62);
		
		ASTNode selectedNode = whileSelection.singleSelectedNode();
		
		
		assertTrue(selectedNode != null);
		
		assertTrue(selectedNode instanceof WhileStatement);
	}
	
	@Test public void testSelectStatements() {
		String source = "class S {void f(int i) {int x,y; f(x); x++; } }";
		
		CompilationUnit compilationUnit = parseCompilationUnitFromSource(source, "S.java");
		
		PMSelection selectionObject = new PMSelection(compilationUnit, 59 - 26, 69 - 59);
		
		assertNull(selectionObject.singleSelectedNode());
		
		assertTrue(selectionObject.selectedNodeParent() instanceof Block);
		assertEquals(Block.STATEMENTS_PROPERTY, selectionObject.selectedNodeParentProperty());
		
		assertEquals(1, selectionObject.selectedNodeParentPropertyListOffset());
		assertEquals(2, selectionObject.selectedNodeParentPropertyListLength());
		
		assertTrue(selectionObject.isListSelection());
		assertTrue(selectionObject.isMultipleSelection());
		
	}
	
	@Test public void testSelectMethodInvocation() {
		String source = "class S {void f(int i) {int x,y; f(x); } }";
		
		CompilationUnit compilationUnit = parseCompilationUnitFromSource(source, "S.java");
		
		PMSelection selectionObject = new PMSelection(compilationUnit, 59 - 26, 4);
		
		ASTNode selectedNode = selectionObject.singleSelectedNode();
		
		assertTrue(selectedNode != null);
		
		assertTrue(selectedNode instanceof MethodInvocation);
	}
	
	@Test public void testSelectMemberDeclaration() {
		String source = "class S {int x,y; void f(int i) {int x,y; f(x); } int z; }";
		
		CompilationUnit compilationUnit = parseCompilationUnitFromSource(source, "S.java");
		
		PMSelection selectionObject = new PMSelection(compilationUnit, 35 - 26, 43 - 35);
		
		ASTNode selectedNode = selectionObject.singleSelectedNode();
		
		assertTrue(selectedNode instanceof FieldDeclaration);
		
		
		
		selectionObject = new PMSelection(compilationUnit, 44 - 26, 75 - 44);
		
		selectedNode = selectionObject.singleSelectedNode();	
		
		assertTrue(selectedNode instanceof MethodDeclaration);
		
		
		
		selectionObject = new PMSelection(compilationUnit, 76 - 26, 82 - 76);
		
		selectedNode = selectionObject.singleSelectedNode();	
		
		assertTrue(selectedNode instanceof FieldDeclaration);
	}
	
	@Test public void testSelectMemberDeclarations() {
		String source = "class S {int x; void f(int i) {int x,y; f(x); x++; } int y;}";
		
		CompilationUnit compilationUnit = parseCompilationUnitFromSource(source, "S.java");
		
		PMSelection selectionObject = new PMSelection(compilationUnit, 35 - 26, 79 - 35);
		
		assertNull(selectionObject.singleSelectedNode());
		
		assertTrue(selectionObject.selectedNodeParent() instanceof TypeDeclaration);
		assertEquals(TypeDeclaration.BODY_DECLARATIONS_PROPERTY, selectionObject.selectedNodeParentProperty());
		
		assertEquals(0, selectionObject.selectedNodeParentPropertyListOffset());
		assertEquals(2, selectionObject.selectedNodeParentPropertyListLength());
		
		assertTrue(selectionObject.isListSelection());
		assertTrue(selectionObject.isMultipleSelection());
		
	}
	
	
	@Test public void testSelectStatementWithSurroundingWhitespace() {
		String source = "class S {void f() {int x,y; f(); x++;} }";
		
		CompilationUnit compilationUnit = parseCompilationUnitFromSource(source, "S.java");
		
		PMSelection selectionObject = new PMSelection(compilationUnit, 54 - 26, 59 - 54);
		
		ASTNode selectedNode = selectionObject.singleSelectedNode();
		
		assertTrue(selectedNode instanceof Statement);
	}
	
	
	@Test public void testNoneSaneSelection() {
		String source = "class S {void f() {int x,y; f(); x++;} }";
		
		CompilationUnit compilationUnit = parseCompilationUnitFromSource(source, "S.java");
		
		PMSelection selectionObject = new PMSelection(compilationUnit, 59 - 26, 61 - 59);
		
		assertNull(selectionObject.singleSelectedNode());
		
		assertFalse(selectionObject.isSaneSelection());
	}
	
	@Test public void testSelectSimpleName() {
		String source = "class S {void f() {int x,y; f(); x++;} }";
		
		CompilationUnit compilationUnit = parseCompilationUnitFromSource(source, "S.java");
		
		PMSelection selectionObject = new PMSelection(compilationUnit, 49 - 26, 1);
		
		assertTrue(selectionObject.singleSelectedNode() instanceof SimpleName);
		
		assertFalse(selectionObject.isSaneSelection());
		
	}
	
	
	@Test public void testInsertionIndexAtBeginningOfBlock() {
		String source = "class S {void f() {int x,y; f(); x++;} }";
		
		CompilationUnit compilationUnit = parseCompilationUnitFromSource(source);
		
		PMInsertionPoint insertionPointDescriptor = new PMInsertionPoint(compilationUnit, 45 - 26);
		
		
		assertTrue(insertionPointDescriptor.isSaneInsertionPoint());
		
		ASTNode insertionParent = insertionPointDescriptor.insertionParent();
		
		assertTrue(insertionParent instanceof Block);
		
		assertEquals(0, insertionPointDescriptor.insertionIndex());
		assertEquals(Block.STATEMENTS_PROPERTY, insertionPointDescriptor.insertionProperty());		
	}
	
	@Test public void testInsertionIndexInMiddleOfBlock() {
		String source = "class S {void f() {int x,y; f(); x++;} }";
		
		CompilationUnit compilationUnit = parseCompilationUnitFromSource(source);
		
		PMInsertionPoint insertionPointDescriptor = new PMInsertionPoint(compilationUnit, 53 - 26);
		
		
		assertTrue(insertionPointDescriptor.isSaneInsertionPoint());
		
		ASTNode insertionParent = insertionPointDescriptor.insertionParent();
		
		assertTrue(insertionParent instanceof Block);
		
		assertEquals(1, insertionPointDescriptor.insertionIndex());
		assertEquals(Block.STATEMENTS_PROPERTY, insertionPointDescriptor.insertionProperty());
	}
	
	@Test public void testInsertionIndexAtEndOfBlock() {
		String source = "class S {void f() {int x,y; f(); x++;} }";
		
		CompilationUnit compilationUnit = parseCompilationUnitFromSource(source);
		
		PMInsertionPoint insertionPointDescriptor = new PMInsertionPoint(compilationUnit, 63 - 26);
		
		
		assertTrue(insertionPointDescriptor.isSaneInsertionPoint());
		
		ASTNode insertionParent = insertionPointDescriptor.insertionParent();
		
		
		
		assertTrue(insertionParent instanceof Block);
		
		assertEquals(3, insertionPointDescriptor.insertionIndex());
		assertEquals(Block.STATEMENTS_PROPERTY, insertionPointDescriptor.insertionProperty());
	}
	
	@Test public void testNonSaneInsertionPointInMiddleOfStatement() {
		String source = "class S {void f() {int x,y; f(); x++;} }";
		
		CompilationUnit compilationUnit = parseCompilationUnitFromSource(source);
		
		PMInsertionPoint insertionPointDescriptor = new PMInsertionPoint(compilationUnit, 61 - 26);
		
		assertTrue(!insertionPointDescriptor.isSaneInsertionPoint());
				
		assertNull(insertionPointDescriptor.insertionParent());
		
		assertEquals(-1, insertionPointDescriptor.insertionIndex());
		assertNull(insertionPointDescriptor.insertionProperty());
	}
	
	
	@Test public void testInsertionIndexAtBeginningOfBodyDeclarationsList() {
		String source = "class S {int a; void f() {int x,y; f(); x++;} int b;}";
		
		CompilationUnit compilationUnit = parseCompilationUnitFromSource(source);
		
		PMInsertionPoint insertionPointDescriptor = new PMInsertionPoint(compilationUnit, 35 - 26);
		
		
		assertTrue(insertionPointDescriptor.isSaneInsertionPoint());
		
		ASTNode insertionParent = insertionPointDescriptor.insertionParent();
		
		assertTrue(insertionParent instanceof AbstractTypeDeclaration);
		
		assertEquals(0, insertionPointDescriptor.insertionIndex());
		assertEquals(TypeDeclaration.BODY_DECLARATIONS_PROPERTY, insertionPointDescriptor.insertionProperty());		
	}
	
	@Test public void testInsertionIndexInMiddleOfBodyDeclarationsList() {
		String source = "class S {int a; void f() {int x,y; f(); x++;} int b;}";
		
		CompilationUnit compilationUnit = parseCompilationUnitFromSource(source);
		
		PMInsertionPoint insertionPointDescriptor = new PMInsertionPoint(compilationUnit, 41 - 26);
		
		
		assertTrue(insertionPointDescriptor.isSaneInsertionPoint());
		
		ASTNode insertionParent = insertionPointDescriptor.insertionParent();
		
		assertTrue(insertionParent instanceof TypeDeclaration);
		
		assertEquals(1, insertionPointDescriptor.insertionIndex());
		assertEquals(TypeDeclaration.BODY_DECLARATIONS_PROPERTY, insertionPointDescriptor.insertionProperty());
	}
	
	@Test public void testInsertionIndexAtEndOfBodyDeclarationsList() {
		String source = "class S {int a; void f() {int x,y; f(); x++;} int b;}";
		
		CompilationUnit compilationUnit = parseCompilationUnitFromSource(source);
		
		PMInsertionPoint insertionPointDescriptor = new PMInsertionPoint(compilationUnit, 78 - 26);
		
		
		assertTrue(insertionPointDescriptor.isSaneInsertionPoint());
		
		ASTNode insertionParent = insertionPointDescriptor.insertionParent();
		
		
		
		assertTrue(insertionParent instanceof TypeDeclaration);
		
		assertEquals(3, insertionPointDescriptor.insertionIndex());
		assertEquals(TypeDeclaration.BODY_DECLARATIONS_PROPERTY, insertionPointDescriptor.insertionProperty());
	}
	
	
	@Test public void testInsertionIndexInBodyDeclarationsListNotDirectlyOnEdge() {
		String source = "class S {  int a;  int b;  int c;  }";
		
		CompilationUnit compilationUnit = parseCompilationUnitFromSource(source);
		
		PMInsertionPoint insertionPointDescriptor = new PMInsertionPoint(compilationUnit, 36 - 26);
			
		assertTrue(insertionPointDescriptor.isSaneInsertionPoint());
		assertTrue(insertionPointDescriptor.insertionParent() instanceof TypeDeclaration);
		assertEquals(0, insertionPointDescriptor.insertionIndex());
		assertEquals(TypeDeclaration.BODY_DECLARATIONS_PROPERTY, insertionPointDescriptor.insertionProperty());
		
		
		insertionPointDescriptor = new PMInsertionPoint(compilationUnit, 44 - 26);
		
		assertTrue(insertionPointDescriptor.isSaneInsertionPoint());
		assertTrue(insertionPointDescriptor.insertionParent() instanceof TypeDeclaration);
		assertEquals(1, insertionPointDescriptor.insertionIndex());
		assertEquals(TypeDeclaration.BODY_DECLARATIONS_PROPERTY, insertionPointDescriptor.insertionProperty());
		
		insertionPointDescriptor = new PMInsertionPoint(compilationUnit, 52 - 26);
		
		assertTrue(insertionPointDescriptor.isSaneInsertionPoint());
		assertTrue(insertionPointDescriptor.insertionParent() instanceof TypeDeclaration);
		assertEquals(2, insertionPointDescriptor.insertionIndex());
		assertEquals(TypeDeclaration.BODY_DECLARATIONS_PROPERTY, insertionPointDescriptor.insertionProperty());
		
		insertionPointDescriptor = new PMInsertionPoint(compilationUnit, 60 - 26);
		
		assertTrue(insertionPointDescriptor.isSaneInsertionPoint());
		assertTrue(insertionPointDescriptor.insertionParent() instanceof TypeDeclaration);
		assertEquals(3, insertionPointDescriptor.insertionIndex());
		assertEquals(TypeDeclaration.BODY_DECLARATIONS_PROPERTY, insertionPointDescriptor.insertionProperty());
		
		
	}
	
	
	@Test public void testNonSaneInsertionPointInIfGuardCondition() {
		String source = "class S { void m() {if (true) { } }  }";
		
		CompilationUnit compilationUnit = parseCompilationUnitFromSource(source);
		
		PMInsertionPoint insertionPointDescriptor = new PMInsertionPoint(compilationUnit, 52 - 26);
		
		assertFalse(insertionPointDescriptor.isSaneInsertionPoint());
	}
	
}
