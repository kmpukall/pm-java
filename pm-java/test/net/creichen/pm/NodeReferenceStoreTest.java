/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.junit.Test;

public class NodeReferenceStoreTest {

    @Test
    public void testNullingOutNodeRemovesReference() {

        final NodeReferenceStore store = new NodeReferenceStore();

        final AST ast = AST.newAST(AST.JLS4);

        ASTNode node = ast.newSimpleName("Foo");

        final NodeReference reference = store.getReferenceForNode(node);

        // ast = null; //apparaently ast doesn't keep a ref to its nodes
        node = null;

        System.gc();

        assertNull(store.getNodeForReference(reference));
    }

    @Test
    public void testNullingOutReferenceRemovesNode() {
        final NodeReferenceStore store = new NodeReferenceStore();

        final AST ast = AST.newAST(AST.JLS4);

        final ASTNode node = ast.newSimpleName("Foo");

        NodeReference reference = store.getReferenceForNode(node);

        final int hashCodeBefore = reference.hashCode();

        reference = null;

        System.gc();

        reference = store.getReferenceForNode(node);

        assertTrue(reference.hashCode() != hashCodeBefore);
    }

    @Test
    public void testReplaceNodeWithNodeWithExistingReferenceToNewNode() {
        final NodeReferenceStore store = new NodeReferenceStore();

        final AST ast = AST.newAST(AST.JLS4);

        final ASTNode node1 = ast.newSimpleName("Foo");

        final NodeReference reference1 = store.getReferenceForNode(node1);

        final ASTNode node2 = ast.newSimpleName("Bar");

        final NodeReference reference2 = store.getReferenceForNode(node1);

        store.replaceOldNodeVersionWithNewVersion(node1, node2);

        // Note: This means ptr equality of references won't work
        // You need to compare the nodes themselves

        assertSame(node2, reference1.getNode());

        assertSame(node2, reference2.getNode());
    }

    @Test
    public void testReplaceNodeWithNodeWithoutExistingReferenceToNewNode() {
        final NodeReferenceStore store = new NodeReferenceStore();

        final AST ast = AST.newAST(AST.JLS4);

        final ASTNode node1 = ast.newSimpleName("Foo");

        final NodeReference reference1 = store.getReferenceForNode(node1);

        final ASTNode node2 = ast.newSimpleName("Bar");

        store.replaceOldNodeVersionWithNewVersion(node1, node2);

        assertSame(node2, reference1.getNode());
    }

    @Test
    public void testStoreBasics() {

        final NodeReferenceStore store = new NodeReferenceStore();

        final AST ast = AST.newAST(AST.JLS4);

        final ASTNode node = ast.newSimpleName("Foo");

        final NodeReference reference = store.getReferenceForNode(node);

        assertNotNull(reference);
        assertTrue(store.getReferenceForNode(node) == reference);
        assertTrue(store.getNodeForReference(reference) == node);
        assertTrue(reference.getNode() == node);

    }

}
