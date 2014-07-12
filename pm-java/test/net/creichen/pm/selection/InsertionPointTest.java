package net.creichen.pm.selection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import net.creichen.pm.tests.PMTest;

import org.eclipse.jdt.core.dom.*;
import org.junit.Test;

public class InsertionPointTest extends PMTest {

    @Test
    public void testGetInsertionIndexAtBeginningOfBlock() {
        final CompilationUnit compilationUnit = toCompilationUnit("class S {void f() {int x,y; f(); x++;} }");

        final InsertionPoint insertionPoint = new InsertionPoint(compilationUnit,
                insertAfter("class S {void f() {"));

        assertTrue(insertionPoint.isValid());
        final ASTNode insertionParent = insertionPoint.getInsertionParent();
        assertTrue(insertionParent instanceof Block);
        assertEquals(0, insertionPoint.getInsertionIndex());
        assertEquals(Block.STATEMENTS_PROPERTY, insertionPoint.getInsertionProperty());
    }

    @Test
    public void testGetInsertionIndexAtBeginningOfBodyDeclarationsList() {
        final CompilationUnit compilationUnit = toCompilationUnit("class S {int a; void f() {int x,y; f(); x++;} int b;}");

        final InsertionPoint insertionPoint = new InsertionPoint(compilationUnit,
                insertAfter("class S {"));

        assertTrue(insertionPoint.isValid());
        final ASTNode insertionParent = insertionPoint.getInsertionParent();
        assertTrue(insertionParent instanceof AbstractTypeDeclaration);
        assertEquals(0, insertionPoint.getInsertionIndex());
        assertEquals(TypeDeclaration.BODY_DECLARATIONS_PROPERTY,
                insertionPoint.getInsertionProperty());
    }

    @Test
    public void testGetInsertionIndexAtEndOfBlock() {
        final CompilationUnit compilationUnit = toCompilationUnit("class S {void f() {int x,y; f(); x++;} }");

        final InsertionPoint insertionPoint = new InsertionPoint(compilationUnit,
                insertAfter("class S {void f() {int x,y; f(); x++;"));

        assertTrue(insertionPoint.isValid());
        final ASTNode insertionParent = insertionPoint.getInsertionParent();
        assertTrue(insertionParent instanceof Block);
        assertEquals(3, insertionPoint.getInsertionIndex());
        assertEquals(Block.STATEMENTS_PROPERTY, insertionPoint.getInsertionProperty());
    }

    @Test
    public void testGetInsertionIndexAtEndOfBodyDeclarationsList() {
        final CompilationUnit compilationUnit = toCompilationUnit("class S {int a; void f() {int x,y; f(); x++;} int b;}");

        final InsertionPoint insertionPoint = new InsertionPoint(compilationUnit,
                insertAfter("class S {int a; void f() {int x,y; f(); x++;} int b;"));

        assertTrue(insertionPoint.isValid());
        final ASTNode insertionParent = insertionPoint.getInsertionParent();
        assertTrue(insertionParent instanceof TypeDeclaration);
        assertEquals(3, insertionPoint.getInsertionIndex());
        assertEquals(TypeDeclaration.BODY_DECLARATIONS_PROPERTY,
                insertionPoint.getInsertionProperty());
    }

    @Test
    public void testGetInsertionIndexInBodyDeclarationsListNotDirectlyOnEdge1() {
        final CompilationUnit compilationUnit = toCompilationUnit("class S {  int a;  int b;  int c;  }");

        final InsertionPoint insertionPoint = new InsertionPoint(compilationUnit,
                insertAfter("class S { "));

        assertTrue(insertionPoint.isValid());
        assertTrue(insertionPoint.getInsertionParent() instanceof TypeDeclaration);
        assertEquals(0, insertionPoint.getInsertionIndex());
        assertEquals(TypeDeclaration.BODY_DECLARATIONS_PROPERTY,
                insertionPoint.getInsertionProperty());
    }

    @Test
    public void testGetInsertionIndexInBodyDeclarationsListNotDirectlyOnEdge2() {
        final CompilationUnit compilationUnit = toCompilationUnit("class S {  int a;  int b;  int c;  }");

        final InsertionPoint insertionPoint = new InsertionPoint(compilationUnit,
                insertAfter("class S {  int a; "));

        assertTrue(insertionPoint.isValid());
        assertTrue(insertionPoint.getInsertionParent() instanceof TypeDeclaration);
        assertEquals(1, insertionPoint.getInsertionIndex());
        assertEquals(TypeDeclaration.BODY_DECLARATIONS_PROPERTY,
                insertionPoint.getInsertionProperty());
    }

    @Test
    public void testGetInsertionIndexInBodyDeclarationsListNotDirectlyOnEdge3() {
        final CompilationUnit compilationUnit = toCompilationUnit("class S {  int a;  int b;  int c;  }");

        final InsertionPoint insertionPoint = new InsertionPoint(compilationUnit,
                insertAfter("class S {  int a;  int b; "));

        assertTrue(insertionPoint.isValid());
        assertTrue(insertionPoint.getInsertionParent() instanceof TypeDeclaration);
        assertEquals(2, insertionPoint.getInsertionIndex());
        assertEquals(TypeDeclaration.BODY_DECLARATIONS_PROPERTY,
                insertionPoint.getInsertionProperty());
    }

    @Test
    public void testGetInsertionIndexInBodyDeclarationsListNotDirectlyOnEdge4() {
        final CompilationUnit compilationUnit = toCompilationUnit("class S {  int a;  int b;  int c;  }");

        final InsertionPoint insertionPoint = new InsertionPoint(compilationUnit,
                insertAfter("class S {  int a;  int b;  int c; "));

        assertTrue(insertionPoint.isValid());
        assertTrue(insertionPoint.getInsertionParent() instanceof TypeDeclaration);
        assertEquals(3, insertionPoint.getInsertionIndex());
        assertEquals(TypeDeclaration.BODY_DECLARATIONS_PROPERTY,
                insertionPoint.getInsertionProperty());

    }

    @Test
    public void testGetInsertionIndexInMiddleOfBlock() {
        final CompilationUnit compilationUnit = toCompilationUnit("class S {void f() {int x,y; f(); x++;} }");

        final InsertionPoint insertionPoint = new InsertionPoint(compilationUnit,
                insertAfter("class S {void f() {int x,y;"));

        assertTrue(insertionPoint.isValid());
        final ASTNode insertionParent = insertionPoint.getInsertionParent();
        assertTrue(insertionParent instanceof Block);
        assertEquals(1, insertionPoint.getInsertionIndex());
        assertEquals(Block.STATEMENTS_PROPERTY, insertionPoint.getInsertionProperty());
    }

    @Test
    public void testGetInsertionIndexInMiddleOfBodyDeclarationsList() {
        final CompilationUnit compilationUnit = toCompilationUnit("class S {int a; void f() {int x,y; f(); x++;} int b;}");

        final InsertionPoint insertionPoint = new InsertionPoint(compilationUnit,
                insertAfter("class S {int a;"));

        assertTrue(insertionPoint.isValid());
        final ASTNode insertionParent = insertionPoint.getInsertionParent();
        assertTrue(insertionParent instanceof TypeDeclaration);
        assertEquals(1, insertionPoint.getInsertionIndex());
        assertEquals(TypeDeclaration.BODY_DECLARATIONS_PROPERTY,
                insertionPoint.getInsertionProperty());
    }

    @Test
    public void testNonSaneInsertionPointInIfGuardCondition() {
        final CompilationUnit compilationUnit = toCompilationUnit("class S { void m() {if (true) { } }  }");

        final InsertionPoint insertionPoint = new InsertionPoint(compilationUnit,
                insertAfter("class S { void m() {if (tr"));

        assertFalse(insertionPoint.isValid());
    }

    @Test
    public void testNonSaneInsertionPointInMiddleOfStatement() {
        final CompilationUnit compilationUnit = toCompilationUnit("class S {void f() {int x,y; f(); x++;} }");

        final InsertionPoint insertionPoint = new InsertionPoint(compilationUnit,
                insertAfter("class S {void f() {int x,y; f(); x+"));

        assertFalse(insertionPoint.isValid());
        assertNull(insertionPoint.getInsertionParent());
        assertEquals(-1, insertionPoint.getInsertionIndex());
        assertNull(insertionPoint.getInsertionProperty());
    }

    private int insertAfter(final String prefix) {
        return prefix.length();
    }

}
