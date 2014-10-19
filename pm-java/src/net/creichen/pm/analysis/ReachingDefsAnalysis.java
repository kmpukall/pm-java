/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm.analysis;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.creichen.pm.core.PMException;
import net.creichen.pm.models.defuse.Def;
import net.creichen.pm.models.defuse.Use;
import net.creichen.pm.utils.ASTUtil;
import net.creichen.pm.utils.visitors.DefinitionCollector;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.VariableDeclaration;

public class ReachingDefsAnalysis {

    private final MethodDeclaration methodDeclaration;
    private List<Def> definitions;
    private Map<IBinding, Set<Def>> definitionsByBinding;
    private Map<ASTNode, Def> definitionsByDefiningNode;
    private Map<SimpleName, Use> usesByName;
    private List<PMBlock> blocks;
    private Map<ASTNode, PMBlock> blocksByNode;
    private final Map<Def, Map<IBinding, VariableAssignment>> uniqueVariableAssigments = new HashMap<Def, Map<IBinding, VariableAssignment>>();
    private Map<PMBlock, Set<VariableAssignment>> genSets;
    private Map<PMBlock, Set<VariableAssignment>> killSets;

    public ReachingDefsAnalysis(final MethodDeclaration methodDeclaration) {
        this.methodDeclaration = methodDeclaration;
        runAnalysis(); // may wish to do this lazily
    }

    public Def getDefinitionForDefiningNode(final ASTNode definingNode) {
        return this.definitionsByDefiningNode.get(definingNode);
    }

    public List<Def> getDefinitions() {
        return this.definitions;
    }

    // return Use object for a simple name, or null if the simpleName does not
    // represent a use
    public Use getUse(final SimpleName name) {
        return this.usesByName.get(name);
    }

    public Collection<Use> getUses() {
        return this.usesByName.values();
    }

    private void findDefinitions() {
        this.definitions = new ArrayList<Def>();
        this.definitionsByDefiningNode = new HashMap<ASTNode, Def>();

        final DefinitionCollector visitor = new DefinitionCollector();
        this.methodDeclaration.getBody().accept(visitor);
        final List<ASTNode> definingNodes = visitor.getResults();
        for (final ASTNode definingNode : definingNodes) {
            final Def definition = new Def(definingNode);
            this.definitions.add(definition);
            this.definitionsByDefiningNode.put(definingNode, definition);
        }

        this.definitionsByBinding = new HashMap<IBinding, Set<Def>>();
        for (final Def def : this.definitions) {
            final IBinding binding = ASTUtil.getBinding(def);
            Set<Def> definitionsForBinding = this.definitionsByBinding.get(binding);
            if (definitionsForBinding == null) {
                definitionsForBinding = new HashSet<Def>();
                this.definitionsByBinding.put(binding, definitionsForBinding);
            }
            definitionsForBinding.add(def);
        }
    }

    private Map<PMBlock, Set<VariableAssignment>> findGenSets() {
        final Map<PMBlock, Set<VariableAssignment>> result = new HashMap<PMBlock, Set<VariableAssignment>>();
        for (final Def definition : this.definitions) {
            // Create singleton set for gen set (could probably dispense w/
            // containing set)
            final Set<VariableAssignment> genSet = new HashSet<VariableAssignment>();
            final IBinding binding = ASTUtil.getBinding(definition);
            // The binding could be null if the declaration for the lhs no
            // longer exists
            if (binding != null) {
                genSet.add(uniqueVariableAssignment(definition, binding));
                final PMBlock block = getBlockForNode(definition.getDefiningNode());
                result.put(block, genSet);
            }
        }
        return result;
    }

    private Map<PMBlock, Set<VariableAssignment>> findKillSets() {

        final Map<PMBlock, Set<VariableAssignment>> result = new HashMap<PMBlock, Set<VariableAssignment>>();

        // Note: we populate the killsets by iterating through definitions
        // this means there will be no killset for a block with no definitions
        //
        for (final Def definition : this.definitions) {
            final IBinding binding = ASTUtil.getBinding(definition);

            // Binding may be null if the declaring node for our lhs no longer
            // exists
            if (binding != null) {
                final Set<VariableAssignment> killSet = new HashSet<VariableAssignment>();

                // killset for an assignment is the "undefined" assignment plus
                // all the other assignments than this one

                killSet.add(uniqueVariableAssignment(null, binding)); // "undefined"
                // assignment

                for (final Def otherDefinition : this.definitionsByBinding.get(binding)) {
                    if (otherDefinition != definition) {
                        killSet.add(uniqueVariableAssignment(otherDefinition, binding));
                    }
                }

                final PMBlock block = getBlockForNode(definition.getDefiningNode());

                result.put(block, killSet);
            }

        }

        return result;
    }

    private void findUses() {

        this.usesByName = new HashMap<SimpleName, Use>();

        final Block body = this.methodDeclaration.getBody();

        final VariableUsesCollector collector = new VariableUsesCollector();
        body.accept(collector);
        this.usesByName.putAll(collector.getUsesByName());
    }

    private PMBlock getBlockForNode(final ASTNode originalNode) {
        ASTNode node = originalNode;
        do {
            final PMBlock block = this.blocksByNode.get(node);
            if (block == null) {
                node = node.getParent();
            } else {
                return block;
            }
        } while (node != null);
        throw new PMException("Couldn't find block for definingnode  " + originalNode);

    }

    private void runAnalysis() {
        findDefinitions();

        BlockResolver resolver = new BlockResolver();
        resolver.resolveBlocks(this.methodDeclaration);
        this.blocks = resolver.getBlocks();
        this.blocksByNode = resolver.getBlocksByNode();

        this.genSets = findGenSets();
        this.killSets = findKillSets();

        do {
            // ?? do we need to make a copy of the entry/exit info and use these
            // or can we update in place??
        } while (updateBlocks());

        findUses();
    }

    private boolean updateBlocks() {
        boolean changed = false;
        for (final PMBlock block : this.blocks) {
            if (block.equals(this.blocks.get(0))) {
                updateInitialBlock(this.blocks.get(0));
            } else {
                changed |= updateReachingDefsOnEntry(block);
            }
            changed |= updateReachingDefsOnExit(block);
        }
        return changed;
    }

    private void updateInitialBlock(final PMBlock initialBlock) {
        // add "undefined" assignments for all free variables in method
        this.methodDeclaration.accept(new ASTVisitor() {
            @Override
            public boolean visit(final SimpleName name) {

                final IBinding binding = name.resolveBinding();
                // We only care about names if they are variables (i.e.
                // locals or fields)

                if (binding instanceof IVariableBinding) {
                    initialBlock.getReachingDefsOnEntry().add(uniqueVariableAssignment(null, binding));
                }
                return true;
            }
        });
    }

    private boolean updateReachingDefsOnExit(final PMBlock block) {
        final Set<VariableAssignment> newExitReachingDefs = new HashSet<VariableAssignment>();

        newExitReachingDefs.addAll(block.getReachingDefsOnEntry());

        final Set<VariableAssignment> killSet = this.killSets.get(block);
        if (killSet != null) {
            newExitReachingDefs.removeAll(killSet);
        }
        final Set<VariableAssignment> genSet = this.genSets.get(block);
        if (genSet != null) {
            newExitReachingDefs.addAll(genSet);
        }
        if (!newExitReachingDefs.equals(block.getReachingDefsOnExit())) {
            block.getReachingDefsOnExit().clear();
            block.getReachingDefsOnExit().addAll(newExitReachingDefs);
            return true;
        }
        return false;
    }

    private static boolean updateReachingDefsOnEntry(final PMBlock block) {
        final Set<VariableAssignment> newEntryReachingDefs = new HashSet<VariableAssignment>();
        for (final PMBlock incomingBlock : block.getIncomingBlocks()) {
            newEntryReachingDefs.addAll(incomingBlock.getReachingDefsOnExit());
        }
        if (!newEntryReachingDefs.equals(block.getReachingDefsOnEntry())) {
            block.getReachingDefsOnEntry().clear();
            block.getReachingDefsOnEntry().addAll(newEntryReachingDefs);
            return true;
        }
        return false;
    }

    private VariableAssignment uniqueVariableAssignment(final Def definition, final IBinding variableBinding) {
        if (variableBinding == null) {
            throw new PMException("variableBinding for " + definition + " is null!");
        }
        Map<IBinding, VariableAssignment> assignmentsForLocation = this.uniqueVariableAssigments.get(definition);
        if (assignmentsForLocation == null) {
            assignmentsForLocation = new HashMap<IBinding, VariableAssignment>();
            this.uniqueVariableAssigments.put(definition, assignmentsForLocation);
        }
        VariableAssignment variableAssignment = assignmentsForLocation.get(variableBinding);
        if (variableAssignment == null) {
            variableAssignment = new VariableAssignment(definition, variableBinding);
            assignmentsForLocation.put(variableBinding, variableAssignment);
        }
        return variableAssignment;
    }

    private class VariableUsesCollector extends ASTVisitor {

        private Map<SimpleName, Use> usesByName = new HashMap<SimpleName, Use>();

        public final Map<SimpleName, Use> getUsesByName() {
            return this.usesByName;
        }

        @Override
        public boolean visit(final SimpleName name) {
            final PMBlock block = getBlockForNode(name);
            final Set<VariableAssignment> reachingDefinitions = block.getReachingDefsOnEntry();

            if (isUse(name)) {
                final Use use = new Use(name);
                this.usesByName.put(name, use);
                final IBinding variableBinding = name.resolveBinding();

                for (final VariableAssignment reachingDefinition : reachingDefinitions) {
                    if (reachingDefinition.getVariableBinding() == variableBinding) {
                        Def def = reachingDefinition.getDefinition();
                        use.addReachingDefinition(def);
                        if (def != null) {
                            // not sure if we want reachingDef == null to mean unitialized or
                            // real reaching def object that is marked as unitialized
                            def.addUse(use);
                        }
                    }
                }
            }

            return true;
        }

        private boolean isUse(final SimpleName name) {
            /*
             * we assume all simple names are uses except:
             *
             * the lhs of Assignment expressions the name of a VariableDeclarationFragment the name of a
             * SingleVariableDeclaration
             *
             * There are probably more cases (i.e. method names in invocations?)
             */

            final ASTNode parent = name.getParent();

            if (parent instanceof Assignment && ((Assignment) parent).getLeftHandSide() == name) {
                return false;
            } else if (parent instanceof VariableDeclaration && ((VariableDeclaration) parent).getName() == name) {
                return false;
            }

            return true;
        }
    }

}
