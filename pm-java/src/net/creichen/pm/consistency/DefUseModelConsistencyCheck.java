package net.creichen.pm.consistency;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import net.creichen.pm.analysis.DefUseAnalysis;
import net.creichen.pm.api.Node;
import net.creichen.pm.consistency.inconsistencies.Inconsistency;
import net.creichen.pm.consistency.inconsistencies.MissingDefinition;
import net.creichen.pm.consistency.inconsistencies.UnexpectedDefinition;
import net.creichen.pm.consistency.inconsistencies.UnknownUse;
import net.creichen.pm.core.PMException;
import net.creichen.pm.core.Project;
import net.creichen.pm.data.NodeStore;
import net.creichen.pm.models.defuse.DefUseModel;
import net.creichen.pm.models.defuse.Use;

import org.eclipse.jdt.core.dom.ASTNode;

class DefUseModelConsistencyCheck {

    private Project project;

    public DefUseModelConsistencyCheck(final Project project) {
        this.project = project;
    }

    public Collection<Inconsistency> calculateInconsistencies(final DefUseModel model) {
        final Collection<Inconsistency> inconsistencies = new HashSet<Inconsistency>();

        final Collection<Use> uses = new DefUseAnalysis(this.project.getASTRoots()).getUses();
        for (final Use use : uses) {
            final ASTNode usingNode = use.getSimpleName();
            final Collection<ASTNode> currentDefiningNodes = model.getDefiningNodesForUse(use);
            final Node useNameIdentifier = NodeStore.getInstance().getReference(usingNode);
            final Set<Node> desiredDefinitionIdentifiers = model.getDefinitionsForUse(useNameIdentifier);
            if (desiredDefinitionIdentifiers == null) {
                inconsistencies.add(new UnknownUse(use.getSimpleName()));
                continue;
            } else {
                // find definitions that should reach and missing
                // definitions
                for (final Node desiredDefinitionIdentifier : desiredDefinitionIdentifiers) {
                    ASTNode desiredDefiningNode;
                    if (!model.isUninitialized(desiredDefinitionIdentifier)) {
                        desiredDefiningNode = desiredDefinitionIdentifier.getNode();
                        if (desiredDefiningNode == null) {
                            throw new PMException("Couldn't find defining node for identifier:"
                                    + desiredDefinitionIdentifier + "for use " + usingNode);
                        }
                    } else {
                        desiredDefiningNode = null;
                    }
                    if (!currentDefiningNodes.contains(desiredDefiningNode)) {
                        inconsistencies.add(new MissingDefinition(usingNode, desiredDefiningNode));
                    }
                }

            }

            // Now check to make sure there aren't any extra defs
            // i.e. is every current defining node in the list of desired
            // efining nodes
            for (final ASTNode currentDefiningNode : currentDefiningNodes) {
                Node currentDefiningIdentifier = null;
                if (currentDefiningNode == null) {
                    currentDefiningIdentifier = model.getUninitialized();
                } else {
                    currentDefiningIdentifier = NodeStore.getInstance().getReference(currentDefiningNode);
                }
                if (!desiredDefinitionIdentifiers.contains(currentDefiningIdentifier)) {
                    inconsistencies.add(new UnexpectedDefinition(usingNode, currentDefiningNode));
                }
            }
        }

        return inconsistencies;
    }

}
