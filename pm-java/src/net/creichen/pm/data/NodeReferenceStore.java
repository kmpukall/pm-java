/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm.data;

import java.lang.ref.WeakReference;
import java.util.WeakHashMap;

import net.creichen.pm.api.NodeReference;

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
public final class NodeReferenceStore {

    private static NodeReferenceStore instance;

    private final WeakHashMap<NodeReference, WeakReference<ASTNode>> nodesForReferences;
    private final WeakHashMap<ASTNode, WeakReference<NodeReference>> referencesForNodes;

    public NodeReferenceStore() {
        this.nodesForReferences = new WeakHashMap<NodeReference, WeakReference<ASTNode>>();

        this.referencesForNodes = new WeakHashMap<ASTNode, WeakReference<NodeReference>>();
    }

    public static NodeReferenceStore getInstance() {
        if (instance == null) {
            instance = new NodeReferenceStore();
        }
        return instance;
    }

    public ASTNode getNode(final NodeReference nodeIdentifier) {
        return this.nodesForReferences.get(nodeIdentifier).get();
    }

    public NodeReference getReference(final ASTNode node) {
        final WeakReference<NodeReference> weakReference = this.referencesForNodes.get(node);

        NodeReference reference;
        if (weakReference == null || weakReference.get() == null) {
            reference = createNodeReference(node);
        } else {
            reference = weakReference.get();
        }

        return reference;
    }

    private NodeReference createNodeReference(final ASTNode node) {
        NodeReference nodeReference = new NodeReference() {

            @Override
            public ASTNode getNode() {
                return NodeReferenceStore.this.getNode(this);
            }
        };
        this.nodesForReferences.put(nodeReference, new WeakReference<ASTNode>(node));
        this.referencesForNodes.put(node, new WeakReference<NodeReference>(nodeReference));
        return nodeReference;
    }

    public void replaceNode(final ASTNode oldNode, final ASTNode newNode) {
        final WeakReference<NodeReference> referenceWeakRef = this.referencesForNodes.get(oldNode);

        if (referenceWeakRef != null) {
            final NodeReference reference = referenceWeakRef.get();

            if (reference != null) {
                this.referencesForNodes.remove(oldNode);
                this.referencesForNodes.put(newNode, referenceWeakRef);
                this.nodesForReferences.put(reference, new WeakReference<ASTNode>(newNode));
            }
        }

    }

}
