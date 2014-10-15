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

    public Set<Node> definitionsForUse(final Node use) {
        return this.defsByUses.get(use);
    }

    public void addUseIfNotPresent(final Node use) {
        if (this.defsByUses.get(use) == null) {
            this.defsByUses.put(use, new HashSet<Node>());
        }
    }

    public boolean isUse(final ASTNode node) {
        final Node nameReference = NodeStore.getInstance().getReference(node);
        return this.defsByUses.containsKey(nameReference);
    }

    public void removeDefinition(final Node definition) {
        this.usesByDefs.remove(definition);
    }

    public Set<Node> usesForDefinition(final Node definition) {
        return this.usesByDefs.get(definition);
    }

    public void addDefinitionIfNotPresent(final Node definition) {
        if (this.usesByDefs.get(definition) == null) {
            this.usesByDefs.put(definition, new HashSet<Node>());
        }
    }
}