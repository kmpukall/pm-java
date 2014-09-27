/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm.utils;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import net.creichen.pm.tests.PMTest;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.TypeParameter;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.junit.Test;

public class ASTQueryTest extends PMTest {

    private AST ast;
    private SimpleName simpleName;

    @Override
    protected void setUp() {
        this.ast = AST.newAST(AST.JLS4);
        this.simpleName = this.ast.newSimpleName("x");
    }

    @Test
    public void testFindClassByName() {
        final String source = "class S {int x; int f() {} } class T {} class S {int y; int f() {} }";
        final CompilationUnit compilationUnit = toCompilationUnit(source);
        final ASTNode expected = ASTQuery.findNodeForSelection(40, 28, compilationUnit);

        TypeDeclaration declaration = ASTQuery.findClassByName("S", 1, compilationUnit);

        assertThat(declaration, is(equalTo(expected)));
    }

    @Test
    public void testFindMethodByName() {
        final String source = "class S {int x; int f() {} } class S {int y; int f() {} int f() {} }";
        final CompilationUnit compilationUnit = toCompilationUnit(source);
        final ASTNode expected = ASTQuery.findNodeForSelection(56, 10, compilationUnit);

        MethodDeclaration method = ASTQuery.findMethodByName("f", 1, "S", 1, compilationUnit);

        assertThat(method, is(equalTo(expected)));
    }

    @Test
    public void testFindSimpleNameByIdentifier() {
        final String source = "class S {int x,y; int f(int x) {int x,y; try {x = y + 1;} catch(Exception x){} while(1) {int y,x; x--;} } }";
        final CompilationUnit compilationUnit = toCompilationUnit(source);
        final ASTNode expected = ASTQuery.findNodeForSelection(98, 1, compilationUnit);

        SimpleName simpleName = ASTQuery.findSimpleNameByIdentifier("x", 4, "f", 0, "S", 0, compilationUnit);

        assertThat(simpleName, is(equalTo(expected)));
    }

    @Test
    public void testGetSimpleName1() {
        VariableDeclarationFragment node = (VariableDeclarationFragment) this.ast
                .createInstance(VariableDeclarationFragment.class);
        node.setName(this.simpleName);
        assertThat(ASTQuery.getSimpleName(node), is(this.simpleName));
    }

    @Test
    public void testGetSimpleName2() {
        SingleVariableDeclaration node = (SingleVariableDeclaration) this.ast
                .createInstance(SingleVariableDeclaration.class);
        node.setName(this.simpleName);
        assertThat(ASTQuery.getSimpleName(node), is(this.simpleName));
    }

    @Test
    public void testGetSimpleName3() {
        TypeDeclaration node = (TypeDeclaration) this.ast.createInstance(TypeDeclaration.class);
        node.setName(this.simpleName);
        assertThat(ASTQuery.getSimpleName(node), is(this.simpleName));
    }

    @Test
    public void testGetSimpleName4() {
        MethodDeclaration node = (MethodDeclaration) this.ast.createInstance(MethodDeclaration.class);
        node.setName(this.simpleName);
        assertThat(ASTQuery.getSimpleName(node), is(this.simpleName));
    }

    @Test
    public void testGetSimpleName5() {
        TypeParameter node = (TypeParameter) this.ast.createInstance(TypeParameter.class);
        node.setName(this.simpleName);
        assertThat(ASTQuery.getSimpleName(node), is(this.simpleName));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetSimpleName6() {
        Assignment node = (Assignment) this.ast.createInstance(Assignment.class);
        ASTQuery.getSimpleName(node);
    }

    @Test(expected = NullPointerException.class)
    public void testGetSimpleName7() {
        ASTQuery.getSimpleName(null);
    }

}
