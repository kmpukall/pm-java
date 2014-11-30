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
import net.creichen.pm.api.Node;
import net.creichen.pm.utils.factories.ASTNodeFactory;

import org.eclipse.jdt.core.dom.ASTNode;
import org.junit.Test;

public class NodeStoreTest {

    private final NodeStore store = new NodeStore();

    @Test
    //TODO: this test fails randomly. improve it
    public void testNullingOutNodeRemovesReference() {
        ASTNode node = ASTNodeFactory.createSimpleName("Foo");
        final Node reference = this.store.getReference(node);

        // ast doesn't keep a ref to its nodes, so this is good enough
        node = null;
        System.gc();

        assertNull(this.store.getNode(reference));
    }

    @Test
    public void testNullingOutReferenceRemovesNode() {
        final ASTNode node = ASTNodeFactory.createSimpleName("Foo");
        Node reference = this.store.getReference(node);
        final int hashCodeBefore = reference.hashCode();

        reference = null;
        System.gc();

        reference = this.store.getReference(node);
        assertTrue(reference.hashCode() != hashCodeBefore);
    }

    @Test
    public void testReplaceNodeWithExistingReferenceToNewNode() {
        final ASTNode node1 = ASTNodeFactory.createSimpleName("Foo");
        final Node reference1 = this.store.getReference(node1);

        final ASTNode node2 = ASTNodeFactory.createSimpleName("Bar");
        final Node reference2 = this.store.getReference(node1);

        this.store.replaceNode(node1, node2);

        // Note: This means ptr equality of references won't work
        // You need to compare the nodes themselves

        assertSame(node2, reference1.getNode());
        assertSame(node2, reference2.getNode());
    }

    @Test
    public void testReplaceNodeWithoutExistingReferenceToNewNode() {
        final ASTNode node1 = ASTNodeFactory.createSimpleName("Foo");
        final Node reference1 = this.store.getReference(node1);

        final ASTNode node2 = ASTNodeFactory.createSimpleName("Bar");

        this.store.replaceNode(node1, node2);

        assertSame(node2, reference1.getNode());
    }

    @Test
    public void testStoreBasics() {
        final ASTNode node = ASTNodeFactory.createSimpleName("Foo");

        final Node reference = this.store.getReference(node);

        assertNotNull(reference);
        assertTrue(this.store.getReference(node) == reference);
        assertTrue(this.store.getNode(reference) == node);
        assertTrue(reference.getNode() == node);

    }

}
