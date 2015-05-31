/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm.utils;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import net.creichen.pm.tests.PMTest;

import org.eclipse.jdt.core.dom.*;
import org.junit.Before;
import org.junit.Test;

public class ASTQueryTest extends PMTest {

    private AST ast;
    private SimpleName simpleName;

    @Before
    public void setUp() {
        this.ast = AST.newAST(AST.JLS8);
        this.simpleName = this.ast.newSimpleName("x");
    }

    @Test
    public void testResolveSimpleName1() {
        VariableDeclarationFragment node = (VariableDeclarationFragment) this.ast
                .createInstance(VariableDeclarationFragment.class);
        node.setName(this.simpleName);
        assertThat(ASTQuery.resolveSimpleName(node), is(this.simpleName));
    }

    @Test
    public void testResolveSimpleName2() {
        SingleVariableDeclaration node = (SingleVariableDeclaration) this.ast
                .createInstance(SingleVariableDeclaration.class);
        node.setName(this.simpleName);
        assertThat(ASTQuery.resolveSimpleName(node), is(this.simpleName));
    }

    @Test
    public void testResolveSimpleName3() {
        TypeDeclaration node = (TypeDeclaration) this.ast.createInstance(TypeDeclaration.class);
        node.setName(this.simpleName);
        assertThat(ASTQuery.resolveSimpleName(node), is(this.simpleName));
    }

    @Test
    public void testResolveSimpleName4() {
        MethodDeclaration node = (MethodDeclaration) this.ast.createInstance(MethodDeclaration.class);
        node.setName(this.simpleName);
        assertThat(ASTQuery.resolveSimpleName(node), is(this.simpleName));
    }

    @Test
    public void testResolveSimpleName5() {
        TypeParameter node = (TypeParameter) this.ast.createInstance(TypeParameter.class);
        node.setName(this.simpleName);
        assertThat(ASTQuery.resolveSimpleName(node), is(this.simpleName));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testResolveSimpleName6() {
        Assignment node = (Assignment) this.ast.createInstance(Assignment.class);
        ASTQuery.resolveSimpleName(node);
    }

    @Test(expected = NullPointerException.class)
    public void testResolveSimpleName7() {
        ASTQuery.resolveSimpleName(null);
    }

}
