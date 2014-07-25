package net.creichen.pm.selection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import net.creichen.pm.tests.PMTest;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.junit.Test;

public class InsertionPointFactoryTest extends PMTest {

	@Test
	public void testGetIndexAtBeginningOfBlock() {
		final CompilationUnit compilationUnit = toCompilationUnit("class S {void f() {int x,y; f(); x++;} }");

		final InsertionPoint insertionPoint = InsertionPointFactory.createInsertionPoint(
				compilationUnit, insertAfter("class S {void f() {"));

		assertTrue(insertionPoint.isValid());
		final ASTNode insertionParent = insertionPoint.getParent();
		assertTrue(insertionParent instanceof Block);
		assertEquals(0, insertionPoint.getIndex());
		assertEquals(Block.STATEMENTS_PROPERTY, insertionPoint.getProperty());
	}

	@Test
	public void testGetIndexAtBeginningOfBodyDeclarationsList() {
		final CompilationUnit compilationUnit = toCompilationUnit("class S {int a; void f() {int x,y; f(); x++;} int b;}");

		final InsertionPoint insertionPoint = InsertionPointFactory.createInsertionPoint(
				compilationUnit, insertAfter("class S {"));

		assertTrue(insertionPoint.isValid());
		final ASTNode insertionParent = insertionPoint.getParent();
		assertTrue(insertionParent instanceof AbstractTypeDeclaration);
		assertEquals(0, insertionPoint.getIndex());
		assertEquals(TypeDeclaration.BODY_DECLARATIONS_PROPERTY, insertionPoint.getProperty());
	}

	@Test
	public void testGetIndexAtEndOfBlock() {
		final CompilationUnit compilationUnit = toCompilationUnit("class S {void f() {int x,y; f(); x++;} }");

		final InsertionPoint insertionPoint = InsertionPointFactory.createInsertionPoint(
				compilationUnit, insertAfter("class S {void f() {int x,y; f(); x++;"));

		assertTrue(insertionPoint.isValid());
		final ASTNode insertionParent = insertionPoint.getParent();
		assertTrue(insertionParent instanceof Block);
		assertEquals(3, insertionPoint.getIndex());
		assertEquals(Block.STATEMENTS_PROPERTY, insertionPoint.getProperty());
	}

	@Test
	public void testGetIndexAtEndOfBodyDeclarationsList() {
		final CompilationUnit compilationUnit = toCompilationUnit("class S {int a; void f() {int x,y; f(); x++;} int b;}");

		final InsertionPoint insertionPoint = InsertionPointFactory.createInsertionPoint(
				compilationUnit,
				insertAfter("class S {int a; void f() {int x,y; f(); x++;} int b;"));

		assertTrue(insertionPoint.isValid());
		final ASTNode insertionParent = insertionPoint.getParent();
		assertTrue(insertionParent instanceof TypeDeclaration);
		assertEquals(3, insertionPoint.getIndex());
		assertEquals(TypeDeclaration.BODY_DECLARATIONS_PROPERTY, insertionPoint.getProperty());
	}

	@Test
	public void testGetIndexInBodyDeclarationsListNotDirectlyOnEdge1() {
		final CompilationUnit compilationUnit = toCompilationUnit("class S {  int a;  int b;  int c;  }");

		final InsertionPoint insertionPoint = InsertionPointFactory.createInsertionPoint(
				compilationUnit, insertAfter("class S { "));

		assertTrue(insertionPoint.isValid());
		assertTrue(insertionPoint.getParent() instanceof TypeDeclaration);
		assertEquals(0, insertionPoint.getIndex());
		assertEquals(TypeDeclaration.BODY_DECLARATIONS_PROPERTY, insertionPoint.getProperty());
	}

	@Test
	public void testGetIndexInBodyDeclarationsListNotDirectlyOnEdge2() {
		final CompilationUnit compilationUnit = toCompilationUnit("class S {  int a;  int b;  int c;  }");

		final InsertionPoint insertionPoint = InsertionPointFactory.createInsertionPoint(
				compilationUnit, insertAfter("class S {  int a; "));

		assertTrue(insertionPoint.isValid());
		assertTrue(insertionPoint.getParent() instanceof TypeDeclaration);
		assertEquals(1, insertionPoint.getIndex());
		assertEquals(TypeDeclaration.BODY_DECLARATIONS_PROPERTY, insertionPoint.getProperty());
	}

	@Test
	public void testGetIndexInBodyDeclarationsListNotDirectlyOnEdge3() {
		final CompilationUnit compilationUnit = toCompilationUnit("class S {  int a;  int b;  int c;  }");

		final InsertionPoint insertionPoint = InsertionPointFactory.createInsertionPoint(
				compilationUnit, insertAfter("class S {  int a;  int b; "));

		assertTrue(insertionPoint.isValid());
		assertTrue(insertionPoint.getParent() instanceof TypeDeclaration);
		assertEquals(2, insertionPoint.getIndex());
		assertEquals(TypeDeclaration.BODY_DECLARATIONS_PROPERTY, insertionPoint.getProperty());
	}

	@Test
	public void testGetIndexInBodyDeclarationsListNotDirectlyOnEdge4() {
		final CompilationUnit compilationUnit = toCompilationUnit("class S {  int a;  int b;  int c;  }");

		final InsertionPoint insertionPoint = InsertionPointFactory.createInsertionPoint(
				compilationUnit, insertAfter("class S {  int a;  int b;  int c; "));

		assertTrue(insertionPoint.isValid());
		assertTrue(insertionPoint.getParent() instanceof TypeDeclaration);
		assertEquals(3, insertionPoint.getIndex());
		assertEquals(TypeDeclaration.BODY_DECLARATIONS_PROPERTY, insertionPoint.getProperty());

	}

	@Test
	public void testGetIndexInMiddleOfBlock() {
		final CompilationUnit compilationUnit = toCompilationUnit("class S {void f() {int x,y; f(); x++;} }");

		final InsertionPoint insertionPoint = InsertionPointFactory.createInsertionPoint(
				compilationUnit, insertAfter("class S {void f() {int x,y;"));

		assertTrue(insertionPoint.isValid());
		final ASTNode insertionParent = insertionPoint.getParent();
		assertTrue(insertionParent instanceof Block);
		assertEquals(1, insertionPoint.getIndex());
		assertEquals(Block.STATEMENTS_PROPERTY, insertionPoint.getProperty());
	}

	@Test
	public void testGetIndexInMiddleOfBodyDeclarationsList() {
		final CompilationUnit compilationUnit = toCompilationUnit("class S {int a; void f() {int x,y; f(); x++;} int b;}");

		final InsertionPoint insertionPoint = InsertionPointFactory.createInsertionPoint(
				compilationUnit, insertAfter("class S {int a;"));

		assertTrue(insertionPoint.isValid());
		final ASTNode insertionParent = insertionPoint.getParent();
		assertTrue(insertionParent instanceof TypeDeclaration);
		assertEquals(1, insertionPoint.getIndex());
		assertEquals(TypeDeclaration.BODY_DECLARATIONS_PROPERTY, insertionPoint.getProperty());
	}

	@Test
	public void testNonSaneInsertionPointInIfGuardCondition() {
		final CompilationUnit compilationUnit = toCompilationUnit("class S { void m() {if (true) { } }  }");

		final InsertionPoint insertionPoint = InsertionPointFactory.createInsertionPoint(
				compilationUnit, insertAfter("class S { void m() {if (tr"));

		assertFalse(insertionPoint.isValid());
	}

	@Test
	public void testNonSaneInsertionPointInMiddleOfStatement() {
		final CompilationUnit compilationUnit = toCompilationUnit("class S {void f() {int x,y; f(); x++;} }");

		final InsertionPoint insertionPoint = InsertionPointFactory.createInsertionPoint(
				compilationUnit, insertAfter("class S {void f() {int x,y; f(); x+"));

		assertFalse(insertionPoint.isValid());
		assertNull(insertionPoint.getParent());
		assertEquals(-1, insertionPoint.getIndex());
		assertNull(insertionPoint.getProperty());
	}

	private int insertAfter(final String prefix) {
		return prefix.length();
	}

}
