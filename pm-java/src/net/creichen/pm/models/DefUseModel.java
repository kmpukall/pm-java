/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm.models;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.creichen.pm.api.NodeReference;
import net.creichen.pm.core.PMException;
import net.creichen.pm.data.NodeReferenceStore;
import net.creichen.pm.utils.factories.ASTNodeFactory;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.SimpleName;

public class DefUseModel {

    private static class DefUseStore {
        private final Map<NodeReference, Set<NodeReference>> defsByUses = new HashMap<NodeReference, Set<NodeReference>>();
        private final Map<NodeReference, Set<NodeReference>> usesByDefs = new HashMap<NodeReference, Set<NodeReference>>();

        public Set<NodeReference> definitionsForUse(final NodeReference nameIdentifier) {
            if (this.defsByUses.get(nameIdentifier) == null) {
                this.defsByUses.put(nameIdentifier, new HashSet<NodeReference>());
            }
            return this.defsByUses.get(nameIdentifier);
        }

        public boolean isUse(final ASTNode node) {
            final NodeReference nameReference = NodeReferenceStore.getInstance().getReference(node);
            return this.defsByUses.containsKey(nameReference);
        }

        public void removeDefinition(final NodeReference definition) {
            this.usesByDefs.remove(definition);
        }

        public Set<NodeReference> usesForDefinition(final NodeReference definition) {
            if (this.usesByDefs.get(definition) == null) {
                this.usesByDefs.put(definition, new HashSet<NodeReference>());
            }
            return this.usesByDefs.get(definition);
        }
    }

    private final ASTNode uninitializedMarkerNode;
    private final NodeReference uninitialized;

    private final DefUseStore store = new DefUseStore();

    public DefUseModel(final Collection<Use> currentUses) {
        // this is such a hack; we create a random ast node and then get a
        // reference to it to
        // act as our uninitialized distinguished marker. We have to store this
        // node
        // so it isn't garbage collected out of the store (since the store uses
        // weak refs).
        this.uninitializedMarkerNode = ASTNodeFactory.createSimpleName("Foo");
        this.uninitialized = NodeReferenceStore.getInstance().getReference(this.uninitializedMarkerNode);
        for (final Use use : currentUses) {
            add(use);
        }
    }

    public void add(final Use use) {
        final SimpleName name = use.getSimpleName();
        final NodeReference useReference = NodeReferenceStore.getInstance().getReference(name);
        for (final Def def : use.getReachingDefinitions()) {
            NodeReference defReference;
            if (def != null) {
                final ASTNode definingNode = def.getDefiningNode();
                defReference = NodeReferenceStore.getInstance().getReference(definingNode);
                if (defReference == null) {
                    throw new PMException("Couldn't find identifier for defining node " + definingNode);
                }
            } else {
                defReference = this.uninitialized;
            }
            addDefinition(defReference, useReference);
        }
    }

    public void addDefinition(final NodeReference def, final NodeReference use) {
        this.store.definitionsForUse(use).add(def);
        this.store.usesForDefinition(def).add(use);

    }

    public Collection<ASTNode> definingNodesForUse(final Use use) {
        final Set<ASTNode> definingNodes = new HashSet<ASTNode>();
        for (final Def definition : use.getReachingDefinitions()) {
            if (definition != null) {
                definingNodes.add(definition.getDefiningNode());
            } else {
                definingNodes.add(null);
            }
        }
        return definingNodes;
    }

    public Set<NodeReference> definitionsForUse(NodeReference reference) {
        return this.store.definitionsForUse(reference);
    }

    public void deleteDefinition(final NodeReference definition) {
        // delete all uses of the definition
        for (final NodeReference use : this.store.usesForDefinition(definition)) {
            removeDefinitionIdentifierForName(definition, use);
        }
        // delete list of uses for definition
        this.store.removeDefinition(definition);
    }

    public Set<NodeReference> getDefinitionByUse(final NodeReference useNameIdentifier) {
        return this.store.definitionsForUse(useNameIdentifier);
    }

    public NodeReference getUninitialized() {
        return this.uninitialized;
    }

    public boolean isUninitialized(final NodeReference definition) {
        return this.uninitialized.equals(definition);
    }

    public boolean isUse(final ASTNode node) {
        return this.store.isUse(node);
    }

    public void removeDefinitionIdentifierForName(final NodeReference def, final NodeReference use) {
        this.store.definitionsForUse(use).remove(def);
        this.store.usesForDefinition(def).remove(use);
    }

    public Set<NodeReference> usesForDefinition(NodeReference reference) {
        return this.store.usesForDefinition(reference);
    }
}
