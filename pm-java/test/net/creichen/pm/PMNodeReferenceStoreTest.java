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

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.junit.Test;

public class PMNodeReferenceStoreTest {

    @Test
    public void testNullingOutNodeRemovesReference() {

        final PMNodeReferenceStore store = new PMNodeReferenceStore();

        final AST ast = AST.newAST(AST.JLS4);

        ASTNode node = ast.newSimpleName("Foo");

        final PMNodeReference reference = store.getReferenceForNode(node);

        // ast = null; //apparaently ast doesn't keep a ref to its nodes
        node = null;

        System.gc();

        assertNull(store.getNodeForReference(reference));
    }

    @Test
    public void testNullingOutReferenceRemovesNode() {
        final PMNodeReferenceStore store = new PMNodeReferenceStore();

        final AST ast = AST.newAST(AST.JLS4);

        final ASTNode node = ast.newSimpleName("Foo");

        PMNodeReference reference = store.getReferenceForNode(node);

        final int hashCodeBefore = reference.hashCode();

        reference = null;

        System.gc();

        reference = store.getReferenceForNode(node);

        assert (reference.hashCode() != hashCodeBefore);
    }

    @Test
    public void testReplaceNodeWithNodeWithExistingReferenceToNewNode() {
        final PMNodeReferenceStore store = new PMNodeReferenceStore();

        final AST ast = AST.newAST(AST.JLS4);

        final ASTNode node1 = ast.newSimpleName("Foo");

        final PMNodeReference reference1 = store.getReferenceForNode(node1);

        final ASTNode node2 = ast.newSimpleName("Bar");

        final PMNodeReference reference2 = store.getReferenceForNode(node1);

        store.replaceOldNodeVersionWithNewVersion(node1, node2);

        // Note: This means ptr equality of references won't work
        // You need to compare the nodes themselves

        assertSame(node2, reference1.getNode());

        assertSame(node2, reference2.getNode());
    }

    @Test
    public void testReplaceNodeWithNodeWithoutExistingReferenceToNewNode() {
        final PMNodeReferenceStore store = new PMNodeReferenceStore();

        final AST ast = AST.newAST(AST.JLS4);

        final ASTNode node1 = ast.newSimpleName("Foo");

        final PMNodeReference reference1 = store.getReferenceForNode(node1);

        final ASTNode node2 = ast.newSimpleName("Bar");

        store.replaceOldNodeVersionWithNewVersion(node1, node2);

        assertSame(node2, reference1.getNode());
    }

    @Test
    public void testStoreBasics() {

        final PMNodeReferenceStore store = new PMNodeReferenceStore();

        final AST ast = AST.newAST(AST.JLS4);

        final ASTNode node = ast.newSimpleName("Foo");

        final PMNodeReference reference = store.getReferenceForNode(node);

        assertNotNull(reference);

        assert (store.getReferenceForNode(node) == reference);

        assert (store.getNodeForReference(reference) == reference);

        assert (reference.getNode() == node);

    }

}
