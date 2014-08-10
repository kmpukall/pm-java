package net.creichen.pm.consistency;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import net.creichen.pm.analysis.UseAnalysis;
import net.creichen.pm.api.NodeReference;
import net.creichen.pm.consistency.inconsistencies.Inconsistency;
import net.creichen.pm.consistency.inconsistencies.MissingDefinition;
import net.creichen.pm.consistency.inconsistencies.UnexpectedDefinition;
import net.creichen.pm.consistency.inconsistencies.UnknownUse;
import net.creichen.pm.core.Project;
import net.creichen.pm.data.NodeReferenceStore;
import net.creichen.pm.models.DefUseModel;
import net.creichen.pm.models.Use;
import net.creichen.pm.utils.Timer;

import org.eclipse.jdt.core.dom.ASTNode;

class DefUseModelConsistencyCheck {

    private Project project;

    public DefUseModelConsistencyCheck(final Project project) {
        this.project = project;
    }

    public Collection<Inconsistency> calculateInconsistencies(final DefUseModel model) {

        final Collection<Inconsistency> inconsistencies = new HashSet<Inconsistency>();

        final Collection<Use> uses = new UseAnalysis(this.project.getASTRoots()).getCurrentUses();

        Timer.sharedTimer().start("INCONSISTENCIES");

        for (final Use use : uses) {

            final ASTNode usingNode = use.getSimpleName();

            final Collection<ASTNode> currentDefiningNodes = model.definingNodesForUse(use);

            final NodeReference useNameIdentifier = NodeReferenceStore.getInstance().getReferenceForNode(usingNode);

            if (useNameIdentifier != null) {
                final Set<NodeReference> desiredDefinitionIdentifiers = model.getDefinitionByUse(useNameIdentifier);

                if (desiredDefinitionIdentifiers != null) {
                    // find definitions that should reach and missing
                    // definitions

                    for (final NodeReference desiredDefinitionIdentifier : desiredDefinitionIdentifiers) {
                        ASTNode desiredDefiningNode;

                        if (!model.isUninitialized(desiredDefinitionIdentifier)) {
                            desiredDefiningNode = desiredDefinitionIdentifier.getNode();

                            if (desiredDefiningNode == null) {
                                throw new RuntimeException("Couldn't find defining node for identifier:"
                                        + desiredDefinitionIdentifier + "for use " + usingNode);
                            }
                        } else {
                            desiredDefiningNode = null;
                        }

                        if (!currentDefiningNodes.contains(desiredDefiningNode)) {

                            inconsistencies.add(new MissingDefinition(this.project
                                    .findPMCompilationUnitForNode(usingNode), usingNode, desiredDefiningNode));

                        }
                    }

                } else {
                    inconsistencies.add(new UnknownUse(this.project.findPMCompilationUnitForNode(usingNode), use
                            .getSimpleName()));
                    continue;
                }

                // Now check to make sure there aren't any extra defs
                // i.e. is every current defining node in the list of desired
                // efining nodes

                for (final ASTNode currentDefiningNode : currentDefiningNodes) {
                    NodeReference currentDefiningIdentifier = null;

                    if (currentDefiningNode != null) {
                        currentDefiningIdentifier = NodeReferenceStore.getInstance().getReferenceForNode(
                                currentDefiningNode);

                        if (currentDefiningIdentifier == null) {
                            throw new RuntimeException("Couldn't find  identifier for current defining node "
                                    + currentDefiningNode);
                        }
                    } else {
                        currentDefiningIdentifier = model.getUninitialized();
                    }

                    if (!desiredDefinitionIdentifiers.contains(currentDefiningIdentifier)) {
                        inconsistencies.add(new UnexpectedDefinition(this.project
                                .findPMCompilationUnitForNode(usingNode), usingNode, currentDefiningNode));

                    }
                }

            } else {
                throw new RuntimeException("Couldn't find use identifier for use:" + use.getSimpleName());
            }
        }

        Timer.sharedTimer().stop("INCONSISTENCIES");

        return inconsistencies;
    }

}
