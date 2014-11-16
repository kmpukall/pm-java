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
import net.creichen.pm.utils.visitors.collectors.DefinitionCollector;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.VariableDeclaration;

public class ReachingDefsAnalysis {

    private final MethodDeclaration methodDeclaration;
    private List<Def> definitions;
    private Map<ASTNode, Def> definitionsByDefiningNode;
    private Map<SimpleName, Use> usesByName;
    private List<PMBlock> blocks;
    private final Map<Def, Map<IBinding, VariableAssignment>> uniqueVariableAssigments = new HashMap<Def, Map<IBinding, VariableAssignment>>();
    private BlockResolver blockResolver;

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

        final List<ASTNode> definingNodes = new DefinitionCollector().collectFrom(this.methodDeclaration.getBody());
        for (final ASTNode definingNode : definingNodes) {
            final Def definition = new Def(definingNode);
            this.definitions.add(definition);
            this.definitionsByDefiningNode.put(definingNode, definition);
        }
    }

    private void findGens() {
        for (final Def definition : this.definitions) {

            final IBinding binding = ASTUtil.getBinding(definition);
            // The binding could be null if the declaration for the lhs no
            // longer exists
            if (binding != null) {
                PMBlock block = this.blockResolver.getBlockForNode(definition.getDefiningNode());
                block.setGen(uniqueVariableAssignment(definition, binding));
            }
        }
    }

    private void findKillSets() {
        List<Def> definitions = this.definitions;

        Map<IBinding, Set<Def>> definitionsByBinding = new HashMap<IBinding, Set<Def>>();
        for (final Def def : definitions) {
            final IBinding binding = ASTUtil.getBinding(def);
            Set<Def> definitionsForBinding = definitionsByBinding.get(binding);
            if (definitionsForBinding == null) {
                definitionsForBinding = new HashSet<Def>();
                definitionsByBinding.put(binding, definitionsForBinding);
            }
            definitionsForBinding.add(def);
        }

        // Note: we populate the killsets by iterating through definitions
        // this means there will be no killset for a block with no definitions
        //
        for (final Def definition : definitions) {
            final IBinding binding = ASTUtil.getBinding(definition);

            // Binding may be null if the declaring node for our lhs no longer
            // exists
            if (binding != null) {
                final Set<VariableAssignment> killSet = new HashSet<VariableAssignment>();

                // killset for an assignment is the "undefined" assignment plus
                // all the other assignments than this one

                killSet.add(uniqueVariableAssignment(null, binding)); // "undefined"
                // assignment

                for (final Def otherDefinition : definitionsByBinding.get(binding)) {
                    if (otherDefinition != definition) {
                        killSet.add(uniqueVariableAssignment(otherDefinition, binding));
                    }
                }

                final PMBlock block = this.blockResolver.getBlockForNode(definition.getDefiningNode());
                block.setKillSet(killSet);
            }

        }
    }

    private void findUses() {
        final VariableUsesCollector collector = new VariableUsesCollector();
        this.methodDeclaration.getBody().accept(collector);
        this.usesByName = collector.getUsesByName();
    }

    private void runAnalysis() {
        findDefinitions();

        this.blockResolver = new BlockResolver();
        this.blockResolver.resolve(this.methodDeclaration);
        this.blocks = this.blockResolver.getBlocks();

        findGens();
        findKillSets();

        boolean hasChanged;
        do {
            hasChanged = updateBlocks();
            // ?? do we need to make a copy of the entry/exit info and use these
            // or can we update in place??
        } while (hasChanged);

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

        final Set<VariableAssignment> killSet = block.getKillSet();
        if (killSet != null) {
            newExitReachingDefs.removeAll(killSet);
        }
        final VariableAssignment gen = block.getGen();
        if (gen != null) {
            newExitReachingDefs.add(gen);
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
        for (final PMBlock incomingBlock : block.getPreviousBlocks()) {
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
            final PMBlock block = ReachingDefsAnalysis.this.blockResolver.getBlockForNode(name);
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
