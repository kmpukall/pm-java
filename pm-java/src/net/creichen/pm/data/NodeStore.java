/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm.data;

import java.lang.ref.WeakReference;
import java.util.WeakHashMap;

import net.creichen.pm.api.Node;

import org.eclipse.jdt.core.dom.ASTNode;

//We use weak references to store references to nodes
//that will automatically disappear if no one holds the reference object any more

// Strictly speaking, we don't need to have the node in a weak reference
// as long as we make sure that there are no cycles from the node to
// the store (such as, perhaps, via the node's properties)

// This provides a way of referring to nodes without using the node ptr
// This is useful to maintain references across reparses (which will
// create new nodes)
// and may make it easier to ensure that we're not keeping an entire AST
// in memory
// when we don't need to

// Note: We require that the project does NOT already have a
// PMNodeIdentifier for this node
// otherwise we will loose uniqueness of PMNodeIdentifier and ptr
// comparison
public final class NodeStore {

    private static NodeStore instance;

    private final WeakHashMap<Node, WeakReference<ASTNode>> nodesForReferences;
    private final WeakHashMap<ASTNode, WeakReference<Node>> referencesForNodes;

    public NodeStore() {
        this.nodesForReferences = new WeakHashMap<Node, WeakReference<ASTNode>>();
        this.referencesForNodes = new WeakHashMap<ASTNode, WeakReference<Node>>();
    }

    public static NodeStore getInstance() {
        if (instance == null) {
            instance = new NodeStore();
        }
        return instance;
    }

    public ASTNode getNode(final Node nodeIdentifier) {
        return this.nodesForReferences.get(nodeIdentifier).get();
    }

    /**
     *
     * @param node
     * @return will never return null.
     */
    public Node getReference(final ASTNode node) {
        final WeakReference<Node> weakReference = this.referencesForNodes.get(node);

        Node reference;
        if (weakReference == null || weakReference.get() == null) {
            reference = createNodeReference(node);
        } else {
            reference = weakReference.get();
        }

        return reference;
    }

    private Node createNodeReference(final ASTNode node) {
        Node nodeReference = new Node() {

            @Override
            public ASTNode getNode() {
                return NodeStore.this.getNode(this);
            }
        };
        this.nodesForReferences.put(nodeReference, new WeakReference<ASTNode>(node));
        this.referencesForNodes.put(node, new WeakReference<Node>(nodeReference));
        return nodeReference;
    }

    public void replaceNode(final ASTNode oldNode, final ASTNode newNode) {
        final WeakReference<Node> referenceWeakRef = this.referencesForNodes.get(oldNode);

        if (referenceWeakRef != null) {
            final Node reference = referenceWeakRef.get();

            if (reference != null) {
                this.referencesForNodes.remove(oldNode);
                this.referencesForNodes.put(newNode, referenceWeakRef);
                this.nodesForReferences.put(reference, new WeakReference<ASTNode>(newNode));
            }
        }

    }

}
