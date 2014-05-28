/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

*******************************************************************************/

package net.creichen.pm;

import java.lang.ref.WeakReference;
import java.util.UUID;
import java.util.WeakHashMap;

import org.eclipse.jdt.core.dom.ASTNode;



//We use weak references to store references to nodes
//that will automatically disappear if no one holds the reference object any more




public class PMNodeReferenceStore {

	//Strictly speaking, we don't need to have the node in a weak reference
	//as long as we make sure that there are no cycles from the node to
	//the store (such as, perhaps, via the node's properties)
	
	WeakHashMap<PMNodeReference, WeakReference<ASTNode>> _nodesForReferences;
	
	WeakHashMap<ASTNode, WeakReference<PMNodeReference>> _referencesForNodes;
	
	
	public PMNodeReferenceStore() {
		_nodesForReferences = new WeakHashMap<PMNodeReference, WeakReference<ASTNode>>();
		
		_referencesForNodes = new WeakHashMap<ASTNode, WeakReference<PMNodeReference>>();
	}
	
	
	
	public ASTNode getNodeForReference(PMNodeReference nodeIdentifier) {
		return _nodesForReferences.get(nodeIdentifier).get();
	}
	
	public PMNodeReference getReferenceForNode(ASTNode node) {
		
		WeakReference<PMNodeReference> weakReference = _referencesForNodes.get(node);
		
		
		
		PMNodeReference reference;
	
		if (weakReference == null || weakReference.get() == null) {
			reference = new PMUUIDNodeReference(node); 
			
			_nodesForReferences.put(reference, new WeakReference(node));
			_referencesForNodes.put(node, new WeakReference(reference));
		} else {
			reference =  weakReference.get();
		}
		
		return reference;
	}
	
	public void replaceOldNodeVersionWithNewVersion(ASTNode oldNode, ASTNode newNode) {
		WeakReference<PMNodeReference> referenceWeakRef = _referencesForNodes.get(oldNode);
		
		if (referenceWeakRef != null) {
			PMNodeReference reference = referenceWeakRef.get();
			
			if (reference != null) {
				_referencesForNodes.remove(oldNode);
				_referencesForNodes.put(newNode, referenceWeakRef);
				_nodesForReferences.put(reference, new WeakReference(newNode));
			}
		}
		
		
	}
	
	
	private static String generatedIdentifierForNode(ASTNode node) {
		//Note: the actual class implementing particular node might change
		//across different versions of eclipse or even runs -- might be better
		//to use node.getNodeType() here instead of node.getClas()
		
		
		return node.getClass().getName().toString() + ":" + UUID.randomUUID().toString();
	}
	
	
	
	//Note that this is a non-static inner class
	
	public class PMUUIDNodeReference implements PMNodeReference {
		
		String _description;
		
		//This provides a way of referring to nodes without using the node ptr
		//This is useful to maintain references across reparses (which will create new nodes)
		//and may make it easier to ensure that we're not keeping an entire AST in memory
		//when we don't need to
		
		private PMUUIDNodeReference(ASTNode node) {
			//Note: We require that the project does NOT already have a PMNodeIdentifier for this node
			//otherwise we will loose uniqueness of PMNodeIdentifier and ptr comparison
			
			
			_description = PMNodeReferenceStore.generatedIdentifierForNode(node);
			
		}
		
		
		
		
		
		
		
		public ASTNode getNode() {
			return PMNodeReferenceStore.this.getNodeForReference(this);
		}
	}
	
	
	
}
