/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm.selection;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import net.creichen.pm.tests.PMTest;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.WhileStatement;
import org.junit.Test;

public class SelectionTest extends PMTest {

    @Test
    public void testNoneSaneSelection() {
        final ICompilationUnit compilationUnit = createCompilationUnit("", "S.java",
                "class S {void f() {int x,y; f(); x++;} }");

        final Selection selectionObject = new Selection(getProject().getPMCompilationUnit(compilationUnit),
                startingAfter("class S {void f() {int x,y; f(); "), selecting("x+"));

        assertThat(selectionObject.singleSelectedNode(), is(nullValue(ASTNode.class)));
        assertThat(selectionObject.isSaneSelection(), is(false));
    }

    @Test
    public void testSelectMemberDeclaration1() {
        final ICompilationUnit compilationUnit = createCompilationUnit("", "S.java",
                "class S {int x,y; void f(int i) {int x,y; f(x); } int z; }");

        final Selection selectionObject = new Selection(getProject().getPMCompilationUnit(compilationUnit),
                startingAfter("class S {"), selecting("int x,y;"));

        assertThat(selectionObject.singleSelectedNode(), is(instanceOf(FieldDeclaration.class)));
    }

    @Test
    public void testSelectMemberDeclaration2() {
        final ICompilationUnit compilationUnit = createCompilationUnit("", "S.java",
                "class S {int x,y; void f(int i) {int x,y; f(x); } int z; }");

        final Selection selectionObject = new Selection(getProject().getPMCompilationUnit(compilationUnit),
                startingAfter("class S {int x,y; "), selecting("void f(int i) {int x,y; f(x); }"));

        assertThat(selectionObject.singleSelectedNode(), is(instanceOf(MethodDeclaration.class)));
    }

    @Test
    public void testSelectMemberDeclaration3() {
        final ICompilationUnit compilationUnit = createCompilationUnit("", "S.java",
                "class S {int x,y; void f(int i) {int x,y; f(x); } int z; }");

        final Selection selectionObject = new Selection(getProject().getPMCompilationUnit(compilationUnit),
                startingAfter("class S {int x,y; void f(int i) {int x,y; f(x); } "), selecting("int z;"));

        assertThat(selectionObject.singleSelectedNode(), is(instanceOf(FieldDeclaration.class)));
    }

    @Test
    public void testSelectMemberDeclarations() {
        final ICompilationUnit compilationUnit = createCompilationUnit("", "S.java",
                "class S {int x; void f(int i) {int x,y; f(x); x++; } int y;}");

        final Selection selectionObject = new Selection(getProject().getPMCompilationUnit(compilationUnit),
                startingAfter("class S {"), selecting("int x; void f(int i) {int x,y; f(x); x++; } "));

        assertThat(selectionObject.singleSelectedNode(), is(nullValue(ASTNode.class)));
        assertThat(selectionObject.selectedNodeParent(), is(instanceOf(TypeDeclaration.class)));
        assertThat(selectionObject.selectedNodeParentProperty(),
                is((StructuralPropertyDescriptor) TypeDeclaration.BODY_DECLARATIONS_PROPERTY));
        assertThat(selectionObject.selectedNodeParentPropertyListOffset(), is(0));
        assertThat(selectionObject.selectedNodeParentPropertyListLength(), is(2));
        assertThat(selectionObject.isListSelection(), is(true));
        assertThat(selectionObject.isMultipleSelection(), is(true));

    }

    @Test
    public void testSelectMethodInvocation() {
        final ICompilationUnit compilationUnit = createCompilationUnit("", "S.java",
                "class S {void f(int i) {int x,y; f(x); } }");

        final Selection selectionObject = new Selection(getProject().getPMCompilationUnit(compilationUnit),
                startingAfter("class S {void f(int i) {int x,y; "), selecting("f(x);"));

        assertThat(selectionObject.singleSelectedNode(), is(instanceOf(ASTNode.class)));
    }

    @Test
    public void testSelectSimpleName() {
        final ICompilationUnit compilationUnit = createCompilationUnit("", "S.java",
                "class S {void f() {int x,y; f(); x++;} }");

        final Selection selectionObject = new Selection(getProject().getPMCompilationUnit(compilationUnit),
                startingAfter("class S {void f() {int "), selecting("x"));

        assertThat(selectionObject.singleSelectedNode(), is(instanceOf(SimpleName.class)));
        assertThat(selectionObject.isSaneSelection(), is(true));

    }

    @Test
    public void testSelectStatement() {
        final ICompilationUnit compilationUnit = createCompilationUnit("", "S.java",
                "class S {int x,y; int f() {int x,y; while(1) {x = 5; y = x +1;} } }");

        final Selection whileSelection = new Selection(getProject().getPMCompilationUnit(compilationUnit),
                startingAfter("class S {int x,y; int f() {int x,y; "), selecting("while(1) {x = 5; y = x +1;}"));

        assertThat(whileSelection.singleSelectedNode(), is(instanceOf(WhileStatement.class)));
    }

    @Test
    public void testSelectStatements() {
        final ICompilationUnit compilationUnit = createCompilationUnit("", "S.java",
                "class S {void f(int i) {int x,y; f(x); x++; } }");

        final Selection selectionObject = new Selection(getProject().getPMCompilationUnit(compilationUnit),
                startingAfter("class S {void f(int i) {int x,y; "), selecting("f(x); x++;"));

        assertThat(selectionObject.singleSelectedNode(), is(nullValue(ASTNode.class)));
        assertThat(selectionObject.selectedNodeParent(), is(instanceOf(Block.class)));
        assertThat(selectionObject.selectedNodeParentProperty(),
                is((StructuralPropertyDescriptor) Block.STATEMENTS_PROPERTY));
        assertThat(selectionObject.selectedNodeParentPropertyListOffset(), is(1));
        assertThat(selectionObject.selectedNodeParentPropertyListLength(), is(2));
        assertThat(selectionObject.isListSelection(), is(true));
        assertThat(selectionObject.isMultipleSelection(), is(true));

    }

    @Test
    public void testSelectStatementWithSurroundingWhitespace() {
        final ICompilationUnit compilationUnit = createCompilationUnit("", "S.java",
                "class S {void f() {int x,y; f(); x++;} }");

        final Selection selectionObject = new Selection(getProject().getPMCompilationUnit(compilationUnit),
                startingAfter("class S {void f() {int x,y;"), selecting(" f(); "));

        assertThat(selectionObject.singleSelectedNode(), is(instanceOf(Statement.class)));
    }

    private int selecting(final String selection) {
        return selection.length();
    }

    private int startingAfter(final String prefix) {
        return prefix.length();
    }

}
