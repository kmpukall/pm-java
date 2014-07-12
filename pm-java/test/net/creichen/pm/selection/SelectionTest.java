/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm.selection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import net.creichen.pm.tests.PMTest;

import org.eclipse.jdt.core.dom.*;
import org.junit.Ignore;
import org.junit.Test;

public class SelectionTest extends PMTest {

    @Test
    public void testNoneSaneSelection() {
        final String source = "class S {void f() {int x,y; f(); x++;} }";

        final CompilationUnit compilationUnit = parseCompilationUnitFromSource(source, "S.java");

        final Selection selectionObject = new Selection(compilationUnit, 59 - 26, 61 - 59);

        assertNull(selectionObject.singleSelectedNode());

        assertFalse(selectionObject.isSaneSelection());
    }

    @Test
    public void testSelectMemberDeclaration() {
        final String source = "class S {int x,y; void f(int i) {int x,y; f(x); } int z; }";

        final CompilationUnit compilationUnit = parseCompilationUnitFromSource(source, "S.java");

        Selection selectionObject = new Selection(compilationUnit, 35 - 26, 43 - 35);

        ASTNode selectedNode = selectionObject.singleSelectedNode();

        assertTrue(selectedNode instanceof FieldDeclaration);

        selectionObject = new Selection(compilationUnit, 44 - 26, 75 - 44);

        selectedNode = selectionObject.singleSelectedNode();

        assertTrue(selectedNode instanceof MethodDeclaration);

        selectionObject = new Selection(compilationUnit, 76 - 26, 82 - 76);

        selectedNode = selectionObject.singleSelectedNode();

        assertTrue(selectedNode instanceof FieldDeclaration);
    }

    @Test
    public void testSelectMemberDeclarations() {
        final String source = "class S {int x; void f(int i) {int x,y; f(x); x++; } int y;}";

        final CompilationUnit compilationUnit = parseCompilationUnitFromSource(source, "S.java");

        final Selection selectionObject = new Selection(compilationUnit, 35 - 26, 79 - 35);

        assertNull(selectionObject.singleSelectedNode());

        assertTrue(selectionObject.selectedNodeParent() instanceof TypeDeclaration);
        assertEquals(TypeDeclaration.BODY_DECLARATIONS_PROPERTY,
                selectionObject.selectedNodeParentProperty());

        assertEquals(0, selectionObject.selectedNodeParentPropertyListOffset());
        assertEquals(2, selectionObject.selectedNodeParentPropertyListLength());

        assertTrue(selectionObject.isListSelection());
        assertTrue(selectionObject.isMultipleSelection());

    }

    @Test
    public void testSelectMethodInvocation() {
        final String source = "class S {void f(int i) {int x,y; f(x); } }";

        final CompilationUnit compilationUnit = parseCompilationUnitFromSource(source, "S.java");

        final Selection selectionObject = new Selection(compilationUnit, 59 - 26, 4);

        final ASTNode selectedNode = selectionObject.singleSelectedNode();

        assertTrue(selectedNode != null);

        assertTrue(selectedNode instanceof MethodInvocation);
    }

    @Test
    @Ignore
    public void testSelectSimpleName() {
        final String source = "class S {void f() {int x,y; f(); x++;} }";

        final CompilationUnit compilationUnit = parseCompilationUnitFromSource(source, "S.java");

        final Selection selectionObject = new Selection(compilationUnit, 49 - 26, 1);

        assertTrue(selectionObject.singleSelectedNode() instanceof SimpleName);

        assertFalse(selectionObject.isSaneSelection());

    }

    @Test
    public void testSelectStatement() {
        final String source = "class S {int x,y; int f() {int x,y; while(1) {x = 5; y = x +1;} } }";

        final CompilationUnit compilationUnit = parseCompilationUnitFromSource(source, "S.java");

        final Selection whileSelection = new Selection(compilationUnit, 62 - 26, 89 - 62);

        final ASTNode selectedNode = whileSelection.singleSelectedNode();

        assertTrue(selectedNode != null);

        assertTrue(selectedNode instanceof WhileStatement);
    }

    @Test
    public void testSelectStatements() {
        final String source = "class S {void f(int i) {int x,y; f(x); x++; } }";

        final CompilationUnit compilationUnit = parseCompilationUnitFromSource(source, "S.java");

        final Selection selectionObject = new Selection(compilationUnit, 59 - 26, 69 - 59);

        assertNull(selectionObject.singleSelectedNode());

        assertTrue(selectionObject.selectedNodeParent() instanceof Block);
        assertEquals(Block.STATEMENTS_PROPERTY, selectionObject.selectedNodeParentProperty());

        assertEquals(1, selectionObject.selectedNodeParentPropertyListOffset());
        assertEquals(2, selectionObject.selectedNodeParentPropertyListLength());

        assertTrue(selectionObject.isListSelection());
        assertTrue(selectionObject.isMultipleSelection());

    }

    @Test
    public void testSelectStatementWithSurroundingWhitespace() {
        final String source = "class S {void f() {int x,y; f(); x++;} }";

        final CompilationUnit compilationUnit = parseCompilationUnitFromSource(source, "S.java");

        final Selection selectionObject = new Selection(compilationUnit, 54 - 26, 59 - 54);

        final ASTNode selectedNode = selectionObject.singleSelectedNode();

        assertTrue(selectedNode instanceof Statement);
    }

}
