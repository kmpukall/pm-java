/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm.models.defuse;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import net.creichen.pm.api.Node;
import net.creichen.pm.data.NodeStore;
import net.creichen.pm.utils.factories.ASTNodeFactory;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.SimpleName;

public class DefUseModel {

    private final ASTNode uninitializedMarkerNode;
    private final Node uninitialized;

    private final DefUseStore store = new DefUseStore();

    public DefUseModel(final Collection<Use> uses) {
        // this is such a hack; we create a random ast node and then get a
        // reference to it to act as our uninitialized distinguished marker. We have to store this
        // node so it isn't garbage collected out of the store (since the store uses weak refs).
        this.uninitializedMarkerNode = ASTNodeFactory.createSimpleName("Foo");
        this.uninitialized = NodeStore.getInstance().getReference(this.uninitializedMarkerNode);

        for (final Use use : uses) {
            addUse(use);
        }
    }

    public void addMapping(final Node def, final Node use) {
        ensureUseIsPresent(use);
        ensureDefinitionIsPresent(def);

        this.store.definitionsForUse(use).add(def);
        this.store.usesForDefinition(def).add(use);
    }

    public void removeMapping(final Node def, final Node use) {
        this.store.definitionsForUse(use).remove(def);
        this.store.usesForDefinition(def).remove(use);
    }

    public void addUse(final Use use) {
        final SimpleName name = use.getSimpleName();
        final Node useReference = NodeStore.getInstance().getReference(name);
        this.store.addUseIfNotPresent(useReference);
        for (final Def def : use.getReachingDefinitions()) {
            Node defReference = createDefNode(def);
            ensureDefinitionIsPresent(defReference);
            addMapping(defReference, useReference);
        }
    }

    public void ensureDefinitionIsPresent(Node definition) {
        this.store.addDefinitionIfNotPresent(definition);
    }

    public void ensureUseIsPresent(Node definition) {
        this.store.addUseIfNotPresent(definition);
    }

    private Node createDefNode(final Def def) {
        Node defReference;
        if (def != null) {
            final ASTNode definingNode = def.getDefiningNode();
            defReference = NodeStore.getInstance().getReference(definingNode);
        } else {
            defReference = this.uninitialized;
        }
        return defReference;
    }

    public Collection<ASTNode> getDefiningNodesForUse(final Use use) {
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

    public Set<Node> getDefinitionsForUse(Node reference) {
        return this.store.definitionsForUse(reference);
    }

    public void deleteDefinition(final Node definition) {
        // delete all uses of the definition
        for (final Node use : this.store.usesForDefinition(definition)) {
            removeMapping(definition, use);
        }
        // delete list of uses for definition
        this.store.removeDefinition(definition);
    }

    public Node getUninitialized() {
        return this.uninitialized;
    }

    public boolean isUninitialized(final Node definition) {
        return this.uninitialized.equals(definition);
    }

    public boolean isUse(final ASTNode node) {
        return this.store.isUse(node);
    }

    public Set<Node> getUsesByDefinition(Node def) {
        return this.store.usesForDefinition(def);
    }
}
