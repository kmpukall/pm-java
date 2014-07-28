/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm.analysis;

import java.lang.ref.WeakReference;
import java.util.WeakHashMap;

import net.creichen.pm.api.NodeReference;

import org.eclipse.jdt.core.dom.ASTNode;

//We use weak references to store references to nodes
//that will automatically disappear if no one holds the reference object any more

public final class NodeReferenceStore {

    // Strictly speaking, we don't need to have the node in a weak reference
    // as long as we make sure that there are no cycles from the node to
    // the store (such as, perhaps, via the node's properties)

    private final class PMUUIDNodeReference implements NodeReference {

        // This provides a way of referring to nodes without using the node ptr
        // This is useful to maintain references across reparses (which will
        // create new nodes)
        // and may make it easier to ensure that we're not keeping an entire AST
        // in memory
        // when we don't need to

        private PMUUIDNodeReference(final ASTNode node) {
            // Note: We require that the project does NOT already have a
            // PMNodeIdentifier for this node
            // otherwise we will loose uniqueness of PMNodeIdentifier and ptr
            // comparison
        }

        @Override
        public ASTNode getNode() {
            return getNodeForReference(this);
        }
    }

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

    public ASTNode getNodeForReference(final NodeReference nodeIdentifier) {
        return this.nodesForReferences.get(nodeIdentifier).get();
    }

    public NodeReference getReferenceForNode(final ASTNode node) {

        final WeakReference<NodeReference> weakReference = this.referencesForNodes.get(node);

        NodeReference reference;

        if (weakReference == null || weakReference.get() == null) {
            reference = new PMUUIDNodeReference(node);

            this.nodesForReferences.put(reference, new WeakReference<ASTNode>(node));
            this.referencesForNodes.put(node, new WeakReference<NodeReference>(reference));
        } else {
            reference = weakReference.get();
        }

        return reference;
    }

    // Note that this is a non-static inner class

    public void replaceOldNodeVersionWithNewVersion(final ASTNode oldNode, final ASTNode newNode) {
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
