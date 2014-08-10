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
import net.creichen.pm.data.NodeReferenceStore;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.SimpleName;

public class DefUseModel {

    // for now we only care about the defs that are used by our names

    private final ASTNode uninitializedMarkerNode;
    private final NodeReference uninitialized;

    private final Map<NodeReference, Set<NodeReference>> definitionIdentifiersByUseIdentifier;

    private final Map<NodeReference, Set<NodeReference>> useIdentifiersByDefinitionIdentifier;

    public DefUseModel(final Collection<Use> currentUses) {

        // this is such a hack; we create a random ast node and then get a
        // reference to it to
        // act as our uninitialized distinguished marker. We have to store this
        // node
        // so it isn't garbage collected out of the store (since the store uses
        // weak refs).

        final AST ast = AST.newAST(AST.JLS4);

        this.uninitializedMarkerNode = ast.newSimpleName("Foo");
        this.uninitialized = NodeReferenceStore.getInstance().getReference(this.uninitializedMarkerNode);

        this.definitionIdentifiersByUseIdentifier = new HashMap<NodeReference, Set<NodeReference>>();
        this.useIdentifiersByDefinitionIdentifier = new HashMap<NodeReference, Set<NodeReference>>();

        addUsesToModel(currentUses);
    }

    public void addDefinitionIdentifierForName(final NodeReference definitionIdentifier,
            final NodeReference nameIdentifier) {
        definitionIdentifiersForName(nameIdentifier).add(definitionIdentifier);
        addUseForDefinition(nameIdentifier, definitionIdentifier);

    }

    private void addUseForDefinition(final NodeReference useIdentifier, final NodeReference definitionIdentifier) {
        usesForDefinition(definitionIdentifier).add(useIdentifier);
    }

    private void addUsesToModel(final Collection<Use> uses) {
        for (final Use use : uses) {
            addUseToModel(use);
        }
    }

    public void addUseToModel(final Use use) {
        final SimpleName name = use.getSimpleName();

        final NodeReference nameIdentifier = NodeReferenceStore.getInstance().getReference(name);

        definitionIdentifiersForName(nameIdentifier); // To add an empty entry
        // to our store; gross.

        for (final Def def : use.getReachingDefinitions()) {

            NodeReference definitionIdentifier;

            if (def != null) {
                final ASTNode definingNode = def.getDefiningNode();
                definitionIdentifier = NodeReferenceStore.getInstance().getReference(definingNode);

                if (definitionIdentifier == null) {
                    throw new RuntimeException("Couldn't find identifier for defining node " + definingNode);
                }
            } else {
                definitionIdentifier = this.uninitialized;
            }

            addDefinitionIdentifierForName(definitionIdentifier, nameIdentifier);
        }
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

    public Set<NodeReference> definitionIdentifiersForName(final NodeReference nameIdentifier) {
        Set<NodeReference> definitionIdentifiers = this.definitionIdentifiersByUseIdentifier.get(nameIdentifier);

        if (definitionIdentifiers == null) {
            definitionIdentifiers = new HashSet<NodeReference>();
            this.definitionIdentifiersByUseIdentifier.put(nameIdentifier, definitionIdentifiers);
        }

        return definitionIdentifiers;
    }

    public void deleteDefinition(final NodeReference definition) {
        // delete all uses of the definition

        for (final NodeReference use : usesForDefinition(definition)) {
            removeDefinitionIdentifierForName(definition, use);
        }

        // delete list of uses for definition

        this.useIdentifiersByDefinitionIdentifier.remove(definition);
    }

    public boolean nameIsUse(final SimpleName name) {
        final NodeReference nameReference = NodeReferenceStore.getInstance().getReference(name);

        return this.definitionIdentifiersByUseIdentifier.containsKey(nameReference);
    }

    public void removeDefinitionIdentifierForName(final NodeReference definitionIdentifier,
            final NodeReference nameIdentifier) {
        definitionIdentifiersForName(nameIdentifier).remove(definitionIdentifier);
        removeUseForDefinition(nameIdentifier, definitionIdentifier);
    }

    private void removeUseForDefinition(final NodeReference useIdentifier, final NodeReference definitionIdentifier) {
        usesForDefinition(definitionIdentifier).remove(useIdentifier);
    }

    public Set<NodeReference> usesForDefinition(final NodeReference definitionIdentifier) {
        Set<NodeReference> useIdentifiers = this.useIdentifiersByDefinitionIdentifier.get(definitionIdentifier);

        if (useIdentifiers == null) {
            useIdentifiers = new HashSet<NodeReference>();
            this.useIdentifiersByDefinitionIdentifier.put(definitionIdentifier, useIdentifiers);
        }

        return useIdentifiers;
    }

    public Set<NodeReference> getDefinitionByUse(final NodeReference useNameIdentifier) {
        return this.definitionIdentifiersByUseIdentifier.get(useNameIdentifier);
    }

    public boolean isUninitialized(final NodeReference desiredDefinitionIdentifier) {
        return this.uninitialized.equals(desiredDefinitionIdentifier);
    }

    public NodeReference getUninitialized() {
        return this.uninitialized;
    }
}
