/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm.data;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import net.creichen.pm.api.NodeReference;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.junit.Test;

public class NodeReferenceStoreTest {

    private final NodeReferenceStore store = new NodeReferenceStore();

    @Test
    public void testNullingOutNodeRemovesReference() {
        ASTNode node = AST.newAST(AST.JLS4).newSimpleName("Foo");
        final NodeReference reference = this.store.getReferenceForNode(node);

        // ast doesn't keep a ref to its nodes, so this is good enough
        node = null;
        System.gc();

        assertNull(this.store.getNodeForReference(reference));
    }

    @Test
    public void testNullingOutReferenceRemovesNode() {
        final ASTNode node = AST.newAST(AST.JLS4).newSimpleName("Foo");
        NodeReference reference = this.store.getReferenceForNode(node);
        final int hashCodeBefore = reference.hashCode();

        reference = null;
        System.gc();

        reference = this.store.getReferenceForNode(node);
        assertTrue(reference.hashCode() != hashCodeBefore);
    }

    @Test
    public void testReplaceNodeWithExistingReferenceToNewNode() {
        final AST ast = AST.newAST(AST.JLS4);

        final ASTNode node1 = ast.newSimpleName("Foo");
        final NodeReference reference1 = this.store.getReferenceForNode(node1);

        final ASTNode node2 = ast.newSimpleName("Bar");
        final NodeReference reference2 = this.store.getReferenceForNode(node1);

        this.store.replaceNode(node1, node2);

        // Note: This means ptr equality of references won't work
        // You need to compare the nodes themselves

        assertSame(node2, reference1.getNode());
        assertSame(node2, reference2.getNode());
    }

    @Test
    public void testReplaceNodeWithoutExistingReferenceToNewNode() {
        final AST ast = AST.newAST(AST.JLS4);

        final ASTNode node1 = ast.newSimpleName("Foo");
        final NodeReference reference1 = this.store.getReferenceForNode(node1);

        final ASTNode node2 = ast.newSimpleName("Bar");

        this.store.replaceNode(node1, node2);

        assertSame(node2, reference1.getNode());
    }

    @Test
    public void testStoreBasics() {
        final ASTNode node = AST.newAST(AST.JLS4).newSimpleName("Foo");

        final NodeReference reference = this.store.getReferenceForNode(node);

        assertNotNull(reference);
        assertTrue(this.store.getReferenceForNode(node) == reference);
        assertTrue(this.store.getNodeForReference(reference) == node);
        assertTrue(reference.getNode() == node);

    }

}
