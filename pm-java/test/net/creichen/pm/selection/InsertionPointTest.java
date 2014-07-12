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
        final String source = "class S {void f() {int x,y; f(); x++;} }";
        final CompilationUnit compilationUnit = parseCompilationUnitFromSource(source);

        final InsertionPoint insertionPointDescriptor = new InsertionPoint(compilationUnit, 45 - 26);

        assertTrue(insertionPointDescriptor.isSaneInsertionPoint());
        final ASTNode insertionParent = insertionPointDescriptor.insertionParent();
        assertTrue(insertionParent instanceof Block);
        assertEquals(0, insertionPointDescriptor.insertionIndex());
        assertEquals(Block.STATEMENTS_PROPERTY, insertionPointDescriptor.insertionProperty());
    }

    @Test
    public void testInsertionIndexAtBeginningOfBodyDeclarationsList() {
        final String source = "class S {int a; void f() {int x,y; f(); x++;} int b;}";

        final CompilationUnit compilationUnit = parseCompilationUnitFromSource(source);

        final InsertionPoint insertionPointDescriptor = new InsertionPoint(compilationUnit, 35 - 26);

        assertTrue(insertionPointDescriptor.isSaneInsertionPoint());

        final ASTNode insertionParent = insertionPointDescriptor.insertionParent();

        assertTrue(insertionParent instanceof AbstractTypeDeclaration);

        assertEquals(0, insertionPointDescriptor.insertionIndex());
        assertEquals(TypeDeclaration.BODY_DECLARATIONS_PROPERTY,
                insertionPointDescriptor.insertionProperty());
    }

    @Test
    public void testInsertionIndexAtEndOfBlock() {
        final String source = "class S {void f() {int x,y; f(); x++;} }";

        final CompilationUnit compilationUnit = parseCompilationUnitFromSource(source);

        final InsertionPoint insertionPointDescriptor = new InsertionPoint(compilationUnit, 63 - 26);

        assertTrue(insertionPointDescriptor.isSaneInsertionPoint());

        final ASTNode insertionParent = insertionPointDescriptor.insertionParent();

        assertTrue(insertionParent instanceof Block);

        assertEquals(3, insertionPointDescriptor.insertionIndex());
        assertEquals(Block.STATEMENTS_PROPERTY, insertionPointDescriptor.insertionProperty());
    }

    @Test
    public void testInsertionIndexAtEndOfBodyDeclarationsList() {
        final String source = "class S {int a; void f() {int x,y; f(); x++;} int b;}";

        final CompilationUnit compilationUnit = parseCompilationUnitFromSource(source);

        final InsertionPoint insertionPointDescriptor = new InsertionPoint(compilationUnit, 78 - 26);

        assertTrue(insertionPointDescriptor.isSaneInsertionPoint());

        final ASTNode insertionParent = insertionPointDescriptor.insertionParent();

        assertTrue(insertionParent instanceof TypeDeclaration);

        assertEquals(3, insertionPointDescriptor.insertionIndex());
        assertEquals(TypeDeclaration.BODY_DECLARATIONS_PROPERTY,
                insertionPointDescriptor.insertionProperty());
    }

    @Test
    public void testInsertionIndexInBodyDeclarationsListNotDirectlyOnEdge() {
        final String source = "class S {  int a;  int b;  int c;  }";

        final CompilationUnit compilationUnit = parseCompilationUnitFromSource(source);

        InsertionPoint insertionPointDescriptor = new InsertionPoint(compilationUnit, 36 - 26);

        assertTrue(insertionPointDescriptor.isSaneInsertionPoint());
        assertTrue(insertionPointDescriptor.insertionParent() instanceof TypeDeclaration);
        assertEquals(0, insertionPointDescriptor.insertionIndex());
        assertEquals(TypeDeclaration.BODY_DECLARATIONS_PROPERTY,
                insertionPointDescriptor.insertionProperty());

        insertionPointDescriptor = new InsertionPoint(compilationUnit, 44 - 26);

        assertTrue(insertionPointDescriptor.isSaneInsertionPoint());
        assertTrue(insertionPointDescriptor.insertionParent() instanceof TypeDeclaration);
        assertEquals(1, insertionPointDescriptor.insertionIndex());
        assertEquals(TypeDeclaration.BODY_DECLARATIONS_PROPERTY,
                insertionPointDescriptor.insertionProperty());

        insertionPointDescriptor = new InsertionPoint(compilationUnit, 52 - 26);

        assertTrue(insertionPointDescriptor.isSaneInsertionPoint());
        assertTrue(insertionPointDescriptor.insertionParent() instanceof TypeDeclaration);
        assertEquals(2, insertionPointDescriptor.insertionIndex());
        assertEquals(TypeDeclaration.BODY_DECLARATIONS_PROPERTY,
                insertionPointDescriptor.insertionProperty());

        insertionPointDescriptor = new InsertionPoint(compilationUnit, 60 - 26);

        assertTrue(insertionPointDescriptor.isSaneInsertionPoint());
        assertTrue(insertionPointDescriptor.insertionParent() instanceof TypeDeclaration);
        assertEquals(3, insertionPointDescriptor.insertionIndex());
        assertEquals(TypeDeclaration.BODY_DECLARATIONS_PROPERTY,
                insertionPointDescriptor.insertionProperty());

    }

    @Test
    public void testInsertionIndexInMiddleOfBlock() {
        final String source = "class S {void f() {int x,y; f(); x++;} }";

        final CompilationUnit compilationUnit = parseCompilationUnitFromSource(source);

        final InsertionPoint insertionPointDescriptor = new InsertionPoint(compilationUnit, 53 - 26);

        assertTrue(insertionPointDescriptor.isSaneInsertionPoint());

        final ASTNode insertionParent = insertionPointDescriptor.insertionParent();

        assertTrue(insertionParent instanceof Block);

        assertEquals(1, insertionPointDescriptor.insertionIndex());
        assertEquals(Block.STATEMENTS_PROPERTY, insertionPointDescriptor.insertionProperty());
    }

    @Test
    public void testInsertionIndexInMiddleOfBodyDeclarationsList() {
        final String source = "class S {int a; void f() {int x,y; f(); x++;} int b;}";

        final CompilationUnit compilationUnit = parseCompilationUnitFromSource(source);

        final InsertionPoint insertionPointDescriptor = new InsertionPoint(compilationUnit, 41 - 26);

        assertTrue(insertionPointDescriptor.isSaneInsertionPoint());

        final ASTNode insertionParent = insertionPointDescriptor.insertionParent();

        assertTrue(insertionParent instanceof TypeDeclaration);

        assertEquals(1, insertionPointDescriptor.insertionIndex());
        assertEquals(TypeDeclaration.BODY_DECLARATIONS_PROPERTY,
                insertionPointDescriptor.insertionProperty());
    }

    @Test
    public void testNonSaneInsertionPointInIfGuardCondition() {
        final String source = "class S { void m() {if (true) { } }  }";

        final CompilationUnit compilationUnit = parseCompilationUnitFromSource(source);

        final InsertionPoint insertionPointDescriptor = new InsertionPoint(compilationUnit, 52 - 26);

        assertFalse(insertionPointDescriptor.isSaneInsertionPoint());
    }

    @Test
    public void testNonSaneInsertionPointInMiddleOfStatement() {
        final String source = "class S {void f() {int x,y; f(); x++;} }";

        final CompilationUnit compilationUnit = parseCompilationUnitFromSource(source);

        final InsertionPoint insertionPointDescriptor = new InsertionPoint(compilationUnit, 61 - 26);

        assertTrue(!insertionPointDescriptor.isSaneInsertionPoint());

        assertNull(insertionPointDescriptor.insertionParent());

        assertEquals(-1, insertionPointDescriptor.insertionIndex());
        assertNull(insertionPointDescriptor.insertionProperty());
    }
}
