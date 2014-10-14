/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm.steps;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.creichen.pm.api.Node;
import net.creichen.pm.core.Project;
import net.creichen.pm.data.NodeStore;
import net.creichen.pm.data.Pasteboard;
import net.creichen.pm.models.defuse.DefUseModel;
import net.creichen.pm.models.name.NameModel;
import net.creichen.pm.utils.ASTQuery;
import net.creichen.pm.utils.visitors.DefinitionCollector;
import net.creichen.pm.utils.visitors.IdentifierAssigner;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.ASTMatcher;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

public class CopyStep extends Step {
    private final List<ASTNode> selectedNodes;

    public CopyStep(final Project project, final ASTNode node) {
        super(project);
        this.selectedNodes = new ArrayList<ASTNode>();
        this.selectedNodes.add(node);
    }

    CopyStep(final Project project, final List<ASTNode> selectedNodes) {
        super(project);
        this.selectedNodes = selectedNodes;
    }

    @Override
    public Map<ICompilationUnit, ASTRewrite> calculateTextualChange() {
        final Map<ICompilationUnit, ASTRewrite> result = new HashMap<ICompilationUnit, ASTRewrite>();

        return result;
    }

    // need method to test for errors before asking for changes

    private void copyNameModel(final List<ASTNode> originalRootNodes, final List<ASTNode> copiedRootNodes) {
        /*
         * Generate fresh identifiers for all copied declarations (and definitions) and keep a mapping from the original
         * identifiers to the new ones so we can later fix up references. We have to do this in two passes because a
         * reference may come before a declaration in the AST.
         */

        final NameModel nameModel = getProject().getNameModel();

        final Map<String, String> copyNameIdentifiersForOriginals = new HashMap<String, String>();

        final ASTMatcher matcher = new ASTMatcher() {
            @Override
            public boolean match(final SimpleName originalName, final Object copyNameObject) {
                final SimpleName copyName = (SimpleName) copyNameObject;

                if (ASTQuery.getSimpleName(getProject().findDeclaringNodeForName(originalName)) == originalName) {
                    // Generate fresh identifier for node with name model

                    final String freshNameModelIdentifier = IdentifierAssigner.generateNewIdentifier();
                    nameModel.setIdentifier(freshNameModelIdentifier, copyName);

                    copyNameIdentifiersForOriginals.put(nameModel.getIdentifier(originalName),
                            freshNameModelIdentifier);
                } else {
                    nameModel.setIdentifier(nameModel.getIdentifier(originalName), copyName);
                }

                return true;
            }
        };

        for (int i = 0; i < originalRootNodes.size(); i++) {
            final ASTNode original = originalRootNodes.get(i);
            final ASTNode copy = copiedRootNodes.get(i);

            original.subtreeMatch(matcher, copy);

        }

        /*
         * Now that we've generated fresh identifiers for all copied declarations, we go through and fix up references
         * to the old identifiers in the copies to now point to the new identifiers
         */

        final ASTVisitor fixupReferenceVisitor = new ASTVisitor() {
            @Override
            public boolean visit(final SimpleName name) {
                final String nameIdentifier = nameModel.getIdentifier(name);

                if (copyNameIdentifiersForOriginals.containsKey(nameIdentifier)) {
                    nameModel.setIdentifier(copyNameIdentifiersForOriginals.get(nameIdentifier), name);
                }
                return true;
            }
        };

        for (final ASTNode copiedPasteboardRoot : copiedRootNodes) {
            copiedPasteboardRoot.accept(fixupReferenceVisitor);
        }
    }

    private void copyUDModel(final List<ASTNode> originalRootNodes, final List<ASTNode> copiedRootNodes) {
        // find all definitions in the copy
        // and keep a mapping from the new definition to the old

        // find all uses in the copy and keep a mapping from the new use to the
        // old

        // for each each in the copy, if it's definition is internal,

        final DefUseModel udModel = getProject().getUDModel();

        final Map<ASTNode, ASTNode> originalUsingNodesForCopiedUsingNodes = new HashMap<ASTNode, ASTNode>();
        final Map<ASTNode, ASTNode> copiedUsingNodesForOriginalUsingNodes = new HashMap<ASTNode, ASTNode>();

        final ASTMatcher nameMatcher = new ASTMatcher() {
            @Override
            public boolean match(final SimpleName originalName, final Object copyNameObject) {
                final SimpleName copyName = (SimpleName) copyNameObject;

                if (udModel.isUse(originalName)) {
                    originalUsingNodesForCopiedUsingNodes.put(copyName, originalName);
                    copiedUsingNodesForOriginalUsingNodes.put(originalName, copyName);
                }

                return true;
            }
        };

        final Map<ASTNode, ASTNode> originalDefiningNodesForCopiedDefiningNodes = new HashMap<ASTNode, ASTNode>();
        final Map<ASTNode, ASTNode> copiedDefiningNodesForCopiedOriginalDefiningNodes = new HashMap<ASTNode, ASTNode>();

        for (int rootNodeIndex = 0; rootNodeIndex < originalRootNodes.size(); rootNodeIndex++) {
            final ASTNode originalRootNode = originalRootNodes.get(rootNodeIndex);
            final ASTNode copyRootNode = copiedRootNodes.get(rootNodeIndex);
            final DefinitionCollector originalVisitor = new DefinitionCollector();
            originalRootNode.accept(originalVisitor);
            final List<ASTNode> originalDefiningNodes = originalVisitor.getResults();

            final DefinitionCollector visitor = new DefinitionCollector();
            copyRootNode.accept(visitor);
            final List<ASTNode> copyDefiningNodes = visitor.getResults();

            for (int definingNodeIndex = 0; definingNodeIndex < originalDefiningNodes.size(); definingNodeIndex++) {
                final ASTNode originalDefiningNode = originalDefiningNodes.get(definingNodeIndex);
                final ASTNode copyDefiningNode = copyDefiningNodes.get(definingNodeIndex);

                originalDefiningNodesForCopiedDefiningNodes.put(copyDefiningNode, originalDefiningNode);
                copiedDefiningNodesForCopiedOriginalDefiningNodes.put(originalDefiningNode, copyDefiningNode);
            }

            originalRootNode.subtreeMatch(nameMatcher, copyRootNode);

        }

        /*
         * Now that we have the mappings:
         *
         * For each copied definition, find the original definition and get the original uses for it for each original
         * use, if it is external add it as a use for the copy if it is internal, generate a new identifier for the copy
         * use and add it to the uses for the copied definition
         */

        for (final ASTNode copiedDefinition : originalDefiningNodesForCopiedDefiningNodes.keySet()) {
            final ASTNode originalDefinition = originalDefiningNodesForCopiedDefiningNodes.get(copiedDefinition);

            final Set<Node> originalUses = udModel.usesForDefinition(NodeStore.getInstance()
                    .getReference(originalDefinition));

            final Set<Node> copyUses = udModel.usesForDefinition(NodeStore.getInstance()
                    .getReference(copiedDefinition));

            for (final Node originalUseReference : originalUses) {
                final ASTNode originalUseNode = originalUseReference.getNode();

                final ASTNode copyUseNode = copiedUsingNodesForOriginalUsingNodes.get(originalUseNode);

                if (copyUseNode != null) { /* use is internal */
                    copyUses.add(NodeStore.getInstance().getReference(copyUseNode));
                } else { /* Use is external, so the original reference is fine */
                    copyUses.add(originalUseReference);
                }
            }
        }

        for (final ASTNode copiedUse : originalUsingNodesForCopiedUsingNodes.keySet()) {
            final ASTNode originalUse = originalUsingNodesForCopiedUsingNodes.get(copiedUse);

            final Set<Node> originalDefinitions = udModel.definitionsForUse(NodeStore
                    .getInstance().getReference(originalUse));

            final Set<Node> copyDefinitions = udModel.definitionsForUse(NodeStore
                    .getInstance().getReference(copiedUse));

            for (final Node originalDefinitionReference : originalDefinitions) {
                final ASTNode originalDefinitionNode = originalDefinitionReference.getNode();

                final ASTNode copyDefinitionNode = copiedDefiningNodesForCopiedOriginalDefiningNodes
                        .get(originalDefinitionNode);

                if (copyDefinitionNode != null) { /* def is internal */
                    copyDefinitions.add(NodeStore.getInstance().getReference(copyDefinitionNode));
                } else { /* Use is external, so the original reference is fine */
                    copyDefinitions.add(originalDefinitionReference);
                }
            }
        }

    }

    @Override
    public void performASTChange() {

        final List<ASTNode> copiedPasteboardRootNodes = new ArrayList<ASTNode>();

        for (final ASTNode original : this.selectedNodes) {
            final ASTNode copy = ASTNode.copySubtree(original.getAST(), original);

            copiedPasteboardRootNodes.add(copy);
        }

        copyNameModel(this.selectedNodes, copiedPasteboardRootNodes);

        copyUDModel(this.selectedNodes, copiedPasteboardRootNodes);

        final Pasteboard pasteboard = Pasteboard.getInstance();

        pasteboard.setPasteboardRoots(copiedPasteboardRootNodes);
    }

    @Override
    public void updateAfterReparse() {

    }

}
