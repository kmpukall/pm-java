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
    public void testInsertionIndexAtBeginningOfBlock() {
        final CompilationUnit compilationUnit = toCompilationUnit("class S {void f() {int x,y; f(); x++;} }");

        final InsertionPoint insertionPointDescriptor = new InsertionPoint(compilationUnit,
                insertAfter("class S {void f() {"));

        assertTrue(insertionPointDescriptor.isSaneInsertionPoint());
        final ASTNode insertionParent = insertionPointDescriptor.insertionParent();
        assertTrue(insertionParent instanceof Block);
        assertEquals(0, insertionPointDescriptor.insertionIndex());
        assertEquals(Block.STATEMENTS_PROPERTY, insertionPointDescriptor.insertionProperty());
    }

    @Test
    public void testInsertionIndexAtBeginningOfBodyDeclarationsList() {
        final CompilationUnit compilationUnit = toCompilationUnit("class S {int a; void f() {int x,y; f(); x++;} int b;}");

        final InsertionPoint insertionPointDescriptor = new InsertionPoint(compilationUnit,
                insertAfter("class S {"));

        assertTrue(insertionPointDescriptor.isSaneInsertionPoint());
        final ASTNode insertionParent = insertionPointDescriptor.insertionParent();
        assertTrue(insertionParent instanceof AbstractTypeDeclaration);
        assertEquals(0, insertionPointDescriptor.insertionIndex());
        assertEquals(TypeDeclaration.BODY_DECLARATIONS_PROPERTY,
                insertionPointDescriptor.insertionProperty());
    }

    @Test
    public void testInsertionIndexAtEndOfBlock() {
        final CompilationUnit compilationUnit = toCompilationUnit("class S {void f() {int x,y; f(); x++;} }");

        final InsertionPoint insertionPointDescriptor = new InsertionPoint(compilationUnit,
                insertAfter("class S {void f() {int x,y; f(); x++;"));

        assertTrue(insertionPointDescriptor.isSaneInsertionPoint());
        final ASTNode insertionParent = insertionPointDescriptor.insertionParent();
        assertTrue(insertionParent instanceof Block);
        assertEquals(3, insertionPointDescriptor.insertionIndex());
        assertEquals(Block.STATEMENTS_PROPERTY, insertionPointDescriptor.insertionProperty());
    }

    @Test
    public void testInsertionIndexAtEndOfBodyDeclarationsList() {
        final CompilationUnit compilationUnit = toCompilationUnit("class S {int a; void f() {int x,y; f(); x++;} int b;}");

        final InsertionPoint insertionPointDescriptor = new InsertionPoint(compilationUnit,
                insertAfter("class S {int a; void f() {int x,y; f(); x++;} int b;"));

        assertTrue(insertionPointDescriptor.isSaneInsertionPoint());
        final ASTNode insertionParent = insertionPointDescriptor.insertionParent();
        assertTrue(insertionParent instanceof TypeDeclaration);
        assertEquals(3, insertionPointDescriptor.insertionIndex());
        assertEquals(TypeDeclaration.BODY_DECLARATIONS_PROPERTY,
                insertionPointDescriptor.insertionProperty());
    }

    @Test
    public void testInsertionIndexInBodyDeclarationsListNotDirectlyOnEdge1() {
        final CompilationUnit compilationUnit = toCompilationUnit("class S {  int a;  int b;  int c;  }");

        final InsertionPoint insertionPointDescriptor = new InsertionPoint(compilationUnit,
                insertAfter("class S { "));

        assertTrue(insertionPointDescriptor.isSaneInsertionPoint());
        assertTrue(insertionPointDescriptor.insertionParent() instanceof TypeDeclaration);
        assertEquals(0, insertionPointDescriptor.insertionIndex());
        assertEquals(TypeDeclaration.BODY_DECLARATIONS_PROPERTY,
                insertionPointDescriptor.insertionProperty());
    }

    @Test
    public void testInsertionIndexInBodyDeclarationsListNotDirectlyOnEdge2() {
        final CompilationUnit compilationUnit = toCompilationUnit("class S {  int a;  int b;  int c;  }");

        final InsertionPoint insertionPointDescriptor = new InsertionPoint(compilationUnit,
                insertAfter("class S {  int a; "));

        assertTrue(insertionPointDescriptor.isSaneInsertionPoint());
        assertTrue(insertionPointDescriptor.insertionParent() instanceof TypeDeclaration);
        assertEquals(1, insertionPointDescriptor.insertionIndex());
        assertEquals(TypeDeclaration.BODY_DECLARATIONS_PROPERTY,
                insertionPointDescriptor.insertionProperty());
    }

    @Test
    public void testInsertionIndexInBodyDeclarationsListNotDirectlyOnEdge3() {
        final CompilationUnit compilationUnit = toCompilationUnit("class S {  int a;  int b;  int c;  }");

        final InsertionPoint insertionPointDescriptor = new InsertionPoint(compilationUnit,
                insertAfter("class S {  int a;  int b; "));

        assertTrue(insertionPointDescriptor.isSaneInsertionPoint());
        assertTrue(insertionPointDescriptor.insertionParent() instanceof TypeDeclaration);
        assertEquals(2, insertionPointDescriptor.insertionIndex());
        assertEquals(TypeDeclaration.BODY_DECLARATIONS_PROPERTY,
                insertionPointDescriptor.insertionProperty());
    }

    @Test
    public void testInsertionIndexInBodyDeclarationsListNotDirectlyOnEdge4() {
        final CompilationUnit compilationUnit = toCompilationUnit("class S {  int a;  int b;  int c;  }");

        final InsertionPoint insertionPointDescriptor = new InsertionPoint(compilationUnit,
                insertAfter("class S {  int a;  int b;  int c; "));

        assertTrue(insertionPointDescriptor.isSaneInsertionPoint());
        assertTrue(insertionPointDescriptor.insertionParent() instanceof TypeDeclaration);
        assertEquals(3, insertionPointDescriptor.insertionIndex());
        assertEquals(TypeDeclaration.BODY_DECLARATIONS_PROPERTY,
                insertionPointDescriptor.insertionProperty());

    }

    @Test
    public void testInsertionIndexInMiddleOfBlock() {
        final CompilationUnit compilationUnit = toCompilationUnit("class S {void f() {int x,y; f(); x++;} }");

        final InsertionPoint insertionPointDescriptor = new InsertionPoint(compilationUnit,
                insertAfter("class S {void f() {int x,y;"));

        assertTrue(insertionPointDescriptor.isSaneInsertionPoint());
        final ASTNode insertionParent = insertionPointDescriptor.insertionParent();
        assertTrue(insertionParent instanceof Block);
        assertEquals(1, insertionPointDescriptor.insertionIndex());
        assertEquals(Block.STATEMENTS_PROPERTY, insertionPointDescriptor.insertionProperty());
    }

    @Test
    public void testInsertionIndexInMiddleOfBodyDeclarationsList() {
        final CompilationUnit compilationUnit = toCompilationUnit("class S {int a; void f() {int x,y; f(); x++;} int b;}");

        final InsertionPoint insertionPointDescriptor = new InsertionPoint(compilationUnit,
                insertAfter("class S {int a;"));

        assertTrue(insertionPointDescriptor.isSaneInsertionPoint());
        final ASTNode insertionParent = insertionPointDescriptor.insertionParent();
        assertTrue(insertionParent instanceof TypeDeclaration);
        assertEquals(1, insertionPointDescriptor.insertionIndex());
        assertEquals(TypeDeclaration.BODY_DECLARATIONS_PROPERTY,
                insertionPointDescriptor.insertionProperty());
    }

    @Test
    public void testNonSaneInsertionPointInIfGuardCondition() {
        final CompilationUnit compilationUnit = toCompilationUnit("class S { void m() {if (true) { } }  }");

        final InsertionPoint insertionPointDescriptor = new InsertionPoint(compilationUnit,
                insertAfter("class S { void m() {if (tr"));

        assertFalse(insertionPointDescriptor.isSaneInsertionPoint());
    }

    @Test
    public void testNonSaneInsertionPointInMiddleOfStatement() {
        final CompilationUnit compilationUnit = toCompilationUnit("class S {void f() {int x,y; f(); x++;} }");

        final InsertionPoint insertionPointDescriptor = new InsertionPoint(compilationUnit,
                insertAfter("class S {void f() {int x,y; f(); x+"));

        assertFalse(insertionPointDescriptor.isSaneInsertionPoint());
        assertNull(insertionPointDescriptor.insertionParent());
        assertEquals(-1, insertionPointDescriptor.insertionIndex());
        assertNull(insertionPointDescriptor.insertionProperty());
    }

    private int insertAfter(final String prefix) {
        return prefix.length();
    }

}
