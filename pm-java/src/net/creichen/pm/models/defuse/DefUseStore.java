package net.creichen.pm.models.defuse;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.creichen.pm.api.Node;
import net.creichen.pm.data.NodeStore;

import org.eclipse.jdt.core.dom.ASTNode;

class DefUseStore {
    private final Map<Node, Set<Node>> defsByUses = new HashMap<Node, Set<Node>>();
    private final Map<Node, Set<Node>> usesByDefs = new HashMap<Node, Set<Node>>();

    public Set<Node> definitionsForUse(final Node nameIdentifier) {
        if (this.defsByUses.get(nameIdentifier) == null) {
            this.defsByUses.put(nameIdentifier, new HashSet<Node>());
        }
        return this.defsByUses.get(nameIdentifier);
    }

    public boolean isUse(final ASTNode node) {
        final Node nameReference = NodeStore.getInstance().getReference(node);
        return this.defsByUses.containsKey(nameReference);
    }

    public void removeDefinition(final Node definition) {
        this.usesByDefs.remove(definition);
    }

    public Set<Node> usesForDefinition(final Node definition) {
        if (this.usesByDefs.get(definition) == null) {
            this.usesByDefs.put(definition, new HashSet<Node>());
        }
        return this.usesByDefs.get(definition);
    }
}