package net.creichen.pm.utils;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.TypeParameter;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.junit.Before;
import org.junit.Test;

public class ASTUtilTest {

    private AST ast;
    private SimpleName simpleName;

    @Before
    public void setUp() {
        this.ast = AST.newAST(AST.JLS4);
        this.simpleName = this.ast.newSimpleName("x");
    }

    @Test
    public void testSimpleNameForNode1() {
        VariableDeclarationFragment node = (VariableDeclarationFragment) this.ast
                .createInstance(VariableDeclarationFragment.class);
        node.setName(this.simpleName);
        assertThat(ASTUtil.simpleNameForNode(node), is(this.simpleName));
    }

    @Test
    public void testSimpleNameForNode2() {
        SingleVariableDeclaration node = (SingleVariableDeclaration) this.ast
                .createInstance(SingleVariableDeclaration.class);
        node.setName(this.simpleName);
        assertThat(ASTUtil.simpleNameForNode(node), is(this.simpleName));
    }

    @Test
    public void testSimpleNameForNode3() {
        TypeDeclaration node = (TypeDeclaration) this.ast.createInstance(TypeDeclaration.class);
        node.setName(this.simpleName);
        assertThat(ASTUtil.simpleNameForNode(node), is(this.simpleName));
    }

    @Test
    public void testSimpleNameForNode4() {
        MethodDeclaration node = (MethodDeclaration) this.ast.createInstance(MethodDeclaration.class);
        node.setName(this.simpleName);
        assertThat(ASTUtil.simpleNameForNode(node), is(this.simpleName));
    }

    @Test
    public void testSimpleNameForNode5() {
        TypeParameter node = (TypeParameter) this.ast.createInstance(TypeParameter.class);
        node.setName(this.simpleName);
        assertThat(ASTUtil.simpleNameForNode(node), is(this.simpleName));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSimpleNameForNode6() {
        Assignment node = (Assignment) this.ast.createInstance(Assignment.class);
        ASTUtil.simpleNameForNode(node);
    }

    @Test(expected = NullPointerException.class)
    public void testSimpleNameForNode7() {
        ASTUtil.simpleNameForNode(null);
    }
}
