/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm.models;

import java.util.*;

import net.creichen.pm.PMNodeReference;
import net.creichen.pm.PMProject;
import net.creichen.pm.Timer;
import net.creichen.pm.analysis.Def;
import net.creichen.pm.analysis.RDefsAnalysis;
import net.creichen.pm.analysis.Use;
import net.creichen.pm.inconsistencies.ExtraDefinition;
import net.creichen.pm.inconsistencies.Inconsistency;
import net.creichen.pm.inconsistencies.MissingDefinition;
import net.creichen.pm.inconsistencies.UnknownUse;

import org.eclipse.jdt.core.dom.*;

public class DefUseModel {
    private final PMProject project;

    // for now we only care about the defs that are used by our names

    private final ASTNode uninitializedMarkerNode;
    private final PMNodeReference uninitialized;

    private final Map<PMNodeReference, Set<PMNodeReference>> definitionIdentifiersByUseIdentifier;

    private final Map<PMNodeReference, Set<PMNodeReference>> useIdentifiersByDefinitionIdentifier;

    public DefUseModel(final PMProject project) {

        this.project = project;

        // this is such a hack; we create a random ast node and then get a
        // reference to it to
        // act as our uninitialized distinguished marker. We have to store this
        // node
        // so it isn't garbage collected out of the store (since the store uses
        // weak refs).

        final AST ast = AST.newAST(AST.JLS4);

        this.uninitializedMarkerNode = ast.newSimpleName("Foo");
        this.uninitialized = this.project.getReferenceForNode(this.uninitializedMarkerNode);

        this.definitionIdentifiersByUseIdentifier = new HashMap<PMNodeReference, Set<PMNodeReference>>();
        this.useIdentifiersByDefinitionIdentifier = new HashMap<PMNodeReference, Set<PMNodeReference>>();

        initializeModel();
    }

    public void addDefinitionIdentifierForName(final PMNodeReference definitionIdentifier,
            final PMNodeReference nameIdentifier) {
        definitionIdentifiersForName(nameIdentifier).add(definitionIdentifier);
        addUseForDefinition(nameIdentifier, definitionIdentifier);

    }

    private void addUseForDefinition(final PMNodeReference useIdentifier,
            final PMNodeReference definitionIdentifier) {
        usesForDefinition(definitionIdentifier).add(useIdentifier);
    }

    private void addUsesToModel(final Collection<Use> uses) {
        for (final Use use : uses) {
            addUseToModel(use);
        }
    }

    public void addUseToModel(final Use use) {
        final SimpleName name = use.getSimpleName();

        final PMNodeReference nameIdentifier = this.project.getReferenceForNode(name);

        definitionIdentifiersForName(nameIdentifier); // To add an empty entry
                                                      // to our store; gross.

        for (final Def def : use.getReachingDefinitions()) {

            PMNodeReference definitionIdentifier;

            if (def != null) {
                final ASTNode definingNode = def.getDefiningNode();
                definitionIdentifier = this.project.getReferenceForNode(definingNode);

                if (definitionIdentifier == null) {
                    throw new RuntimeException("Couldn't find identifier for defining node "
                            + definingNode);
                }
            } else {
                definitionIdentifier = this.uninitialized;
            }

            addDefinitionIdentifierForName(definitionIdentifier, nameIdentifier);
        }
    }

    public Collection<Inconsistency> calculateInconsistencies() {

        final Collection<Inconsistency> inconsistencies = new HashSet<Inconsistency>();

        final Collection<Use> uses = getCurrentUses();

        Timer.sharedTimer().start("INCONSISTENCIES");

        for (final Use use : uses) {

            final ASTNode usingNode = use.getSimpleName();

            final Collection<ASTNode> currentDefiningNodes = definingNodesForUse(use);

            final PMNodeReference useNameIdentifier = this.project.getReferenceForNode(usingNode);

            if (useNameIdentifier != null) {
                final Set<PMNodeReference> desiredDefinitionIdentifiers = this.definitionIdentifiersByUseIdentifier
                        .get(useNameIdentifier);

                if (desiredDefinitionIdentifiers != null) {
                    // find definitions that should reach and missing
                    // definitions

                    for (final PMNodeReference desiredDefinitionIdentifier : desiredDefinitionIdentifiers) {
                        ASTNode desiredDefiningNode;

                        if (!desiredDefinitionIdentifier.equals(this.uninitialized)) {
                            desiredDefiningNode = desiredDefinitionIdentifier.getNode();

                            if (desiredDefiningNode == null) {
                                throw new RuntimeException(
                                        "Couldn't find defining node for identifier:"
                                                + desiredDefinitionIdentifier + "for use "
                                                + usingNode);
                            }
                        } else {
                            desiredDefiningNode = null;
                        }

                        if (!currentDefiningNodes.contains(desiredDefiningNode)) {

                            inconsistencies.add(new MissingDefinition(this.project, this.project
                                    .findPMCompilationUnitForNode(usingNode), usingNode,
                                    desiredDefiningNode));

                        }
                    }

                } else {
                    inconsistencies.add(new UnknownUse(this.project, this.project
                            .findPMCompilationUnitForNode(usingNode), use.getSimpleName()));

                    continue;
                    // throw new
                    // RuntimeException("Couldn't find stored mappings for use:"
                    // + use.getSimpleName());
                }

                // Now check to make sure there aren't any extra defs
                // i.e. is every current defining node in the list of desired
                // efining nodes

                for (final ASTNode currentDefiningNode : currentDefiningNodes) {
                    PMNodeReference currentDefiningIdentifier = null;

                    if (currentDefiningNode != null) {
                        currentDefiningIdentifier = this.project
                                .getReferenceForNode(currentDefiningNode);

                        if (currentDefiningIdentifier == null) {
                            throw new RuntimeException(
                                    "Couldn't find  identifier for current defining node "
                                            + currentDefiningNode);
                        }
                    } else {
                        currentDefiningIdentifier = this.uninitialized;
                    }

                    if (!desiredDefinitionIdentifiers.contains(currentDefiningIdentifier)) {
                        inconsistencies.add(new ExtraDefinition(this.project, this.project
                                .findPMCompilationUnitForNode(usingNode), usingNode,
                                currentDefiningNode));

                    }
                }

            } else {
                throw new RuntimeException("Couldn't find use identifier for use:"
                        + use.getSimpleName());
            }
        }

        Timer.sharedTimer().stop("INCONSISTENCIES");

        return inconsistencies;
    }

    private Collection<ASTNode> definingNodesForUse(final Use use) {
        final HashSet<ASTNode> definingNodes = new HashSet<ASTNode>();

        for (final Def definition : use.getReachingDefinitions()) {
            if (definition != null) {
                definingNodes.add(definition.getDefiningNode());
            } else {
                definingNodes.add(null);
            }

        }

        return definingNodes;
    }

    public Set<PMNodeReference> definitionIdentifiersForName(final PMNodeReference nameIdentifier) {
        Set<PMNodeReference> definitionIdentifiers = this.definitionIdentifiersByUseIdentifier
                .get(nameIdentifier);

        if (definitionIdentifiers == null) {
            definitionIdentifiers = new HashSet<PMNodeReference>();
            this.definitionIdentifiersByUseIdentifier.put(nameIdentifier, definitionIdentifiers);
        }

        return definitionIdentifiers;
    }

    public void deleteDefinition(final PMNodeReference definition) {
        // delete all uses of the definition

        for (final PMNodeReference use : usesForDefinition(definition)) {
            removeDefinitionIdentifierForName(definition, use);
        }

        // delete list of uses for definition

        this.useIdentifiersByDefinitionIdentifier.remove(definition);
    }

    private Collection<Use> getCurrentUses() {

        Timer.sharedTimer().start("DUUD_CHAINS");

        final Collection<Use> uses = new HashSet<Use>();

        for (final ASTNode root : this.project.getASTRoots()) {

            root.accept(new ASTVisitor() {
                @Override
                public boolean visit(final MethodDeclaration methodDeclaration) {

                    // There is nothing to analyze if we have an interface or
                    // abstract method
                    if (methodDeclaration.getBody() != null) {
                        final RDefsAnalysis analysis = new RDefsAnalysis(methodDeclaration);

                        uses.addAll(analysis.getUses());
                    }

                    return false; // don't visit children
                }
            });

        }

        Timer.sharedTimer().stop("DUUD_CHAINS");
        return uses;
    }

    private void initializeModel() {
        addUsesToModel(getCurrentUses());

    }

    public boolean nameIsUse(final SimpleName name) {
        final PMNodeReference nameReference = this.project.getReferenceForNode(name);

        return this.definitionIdentifiersByUseIdentifier.containsKey(nameReference);
    }

    public void removeDefinitionIdentifierForName(final PMNodeReference definitionIdentifier,
            final PMNodeReference nameIdentifier) {
        definitionIdentifiersForName(nameIdentifier).remove(definitionIdentifier);
        removeUseForDefinition(nameIdentifier, definitionIdentifier);
    }

    private void removeUseForDefinition(final PMNodeReference useIdentifier,
            final PMNodeReference definitionIdentifier) {
        usesForDefinition(definitionIdentifier).remove(useIdentifier);
    }

    public Set<PMNodeReference> usesForDefinition(final PMNodeReference definitionIdentifier) {
        Set<PMNodeReference> useIdentifiers = this.useIdentifiersByDefinitionIdentifier
                .get(definitionIdentifier);

        if (useIdentifiers == null) {
            useIdentifiers = new HashSet<PMNodeReference>();
            this.useIdentifiersByDefinitionIdentifier.put(definitionIdentifier, useIdentifiers);
        }

        return useIdentifiers;
    }
}
