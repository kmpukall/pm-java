/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm.analysis;

import static net.creichen.pm.utils.APIWrapperUtil.fragments;
import static net.creichen.pm.utils.APIWrapperUtil.statements;

import java.util.*;

import org.eclipse.jdt.core.dom.*;

public class PMRDefsAnalysis {
    protected static class VariableAssignment {

        private final PMDef _definition;

        private final IBinding _variableBinding;

        public VariableAssignment(final PMDef definition, final IBinding variableBinding) {
            this._definition = definition;
            this._variableBinding = variableBinding;
        }

        public PMDef getDefinition() {
            return this._definition;
        }

        public IBinding getVariableBinding() {
            return this._variableBinding;
        }
    }

    public static List<ASTNode> findDefiningNodesUnderNode(final ASTNode rootNode) {
        final List<ASTNode> result = new ArrayList<ASTNode>();

        final ASTVisitor visitor = new ASTVisitor() {

            boolean isAnalyzableLeftHandSide(final ASTNode lhs) {
                // for now we only support assignments to simple names

                return lhs instanceof SimpleName;
            }

            @Override
            public boolean visit(final Assignment assignment) {
                // plain-old x = y + 1

                if (isAnalyzableLeftHandSide(assignment.getLeftHandSide())) {
                    result.add(assignment);
                }

                return true;
            }

            @Override
            public boolean visit(final PostfixExpression postfixExpression) {
                // all postfix operators are definitions
                // x++
                if (isAnalyzableLeftHandSide(postfixExpression.getOperand())) {
                    result.add(postfixExpression);
                }
                return true;
            }

            @Override
            public boolean visit(final PrefixExpression prefixExpression) {
                // Can't have things like ! being definitions
                if (prefixExpression.getOperator() == PrefixExpression.Operator.INCREMENT
                        || prefixExpression.getOperator() == PrefixExpression.Operator.DECREMENT) {

                    // ++x

                    if (isAnalyzableLeftHandSide(prefixExpression.getOperand())) {
                        result.add(prefixExpression);
                    }
                }

                return true;
            }

            @Override
            public boolean visit(final SingleVariableDeclaration singleVariableDeclaration) {
                // Used in parameter lists and catch clauses
                // There is an implicit definition here

                result.add(singleVariableDeclaration);

                return true;
            }

            @Override
            public boolean visit(final VariableDeclarationFragment variableDeclarationFragment) {
                // int x, y, z = 7; //etc

                result.add(variableDeclarationFragment);

                return true;
            }

        };

        rootNode.accept(visitor);

        return result;
    }

    private final MethodDeclaration methodDeclaration;

    private ArrayList<PMDef> definitions;

    private Map<IBinding, Set<PMDef>> definitionsByBinding;

    private Map<ASTNode, PMDef> definitionsByDefiningNode;

    private Map<SimpleName, PMUse> usesByName;

    private ArrayList<PMBlock> allBlocks;

    private Map<ASTNode, PMBlock> blocksByNode;

    private Map<PMBlock, Set<VariableAssignment>> reachingDefsOnEntry;

    private Map<PMBlock, Set<VariableAssignment>> reachingDefsOnExit;

    private final HashMap<PMDef, HashMap<IBinding, VariableAssignment>> uniqueVariableAssigments = new HashMap<PMDef, HashMap<IBinding, VariableAssignment>>();

    public PMRDefsAnalysis(final MethodDeclaration methodDeclaration) {
        this.methodDeclaration = methodDeclaration;

        runAnalysis(); // may wish to do this lazily
    }

    private void addDefinitionForNode(final ASTNode node) {

        final PMDef definition = new PMDef(node);

        this.definitions.add(definition);
        this.definitionsByDefiningNode.put(node, definition);
    }

    protected void addSerialBlockToEndOfList(final PMBlock block, final ArrayList<PMBlock> blockList) {
        if (blockList.size() > 0) {
            final PMBlock lastBlock = blockList.get(blockList.size() - 1);

            lastBlock.addOutgoingBlock(block);
        }

        blockList.add(block);
    }

    protected boolean astNodeContainsDefinition(final ASTNode node) {

        // could do this more efficiently

        final boolean[] containsDefinition = new boolean[1];
        containsDefinition[0] = false; // to get around final problem

        node.accept(new ASTVisitor() {
            @Override
            public boolean visit(final Assignment assignment) {
                containsDefinition[0] = true;

                return true;
            }

            @Override
            public boolean visit(final SingleVariableDeclaration singleVariableDeclaration) {
                containsDefinition[0] = true;

                return true;
            }

            @Override
            public boolean visit(final VariableDeclarationFragment variableDeclarationFragment) {
                containsDefinition[0] = true;

                return true;
            }

        });

        return containsDefinition[0];
    }

    void findAllBlocks() {

        this.allBlocks = new ArrayList<PMBlock>();

        this.allBlocks.add(new PMBlock()); // synthetic initial block;

        mergeBlockLists(this.allBlocks,
                generateBlocksForStatement(this.methodDeclaration.getBody()));

        // fill in _blocksByNode
        // Every node should have at least one ancestor that has a block
        // according to this hash

        this.blocksByNode = new HashMap<ASTNode, PMBlock>();

        for (final PMBlock block : this.allBlocks) {
            for (final ASTNode node : block.getNodes()) {
                this.blocksByNode.put(node, block);
            }

        }
    }

    void findDefinitions() {
        this.definitions = new ArrayList<PMDef>();
        this.definitionsByDefiningNode = new HashMap<ASTNode, PMDef>();

        final List<ASTNode> definingNodes = findDefiningNodesUnderNode(this.methodDeclaration
                .getBody());

        for (final ASTNode definingNode : definingNodes) {
            addDefinitionForNode(definingNode);
        }

        this.definitionsByBinding = new HashMap<IBinding, Set<PMDef>>();

        for (final PMDef def : this.definitions) {

            final IBinding binding = def.getBinding();

            Set<PMDef> definitionsForBinding = this.definitionsByBinding.get(binding);

            if (definitionsForBinding == null) {
                definitionsForBinding = new HashSet<PMDef>();
                this.definitionsByBinding.put(binding, definitionsForBinding);
            }

            definitionsForBinding.add(def);
        }
    }

    HashMap<PMBlock, HashSet<VariableAssignment>> findGenSets() {

        final HashMap<PMBlock, HashSet<VariableAssignment>> result = new HashMap<PMBlock, HashSet<VariableAssignment>>();

        for (final PMDef definition : this.definitions) {

            // Create singleton set for gen set (could probably dispense w/
            // containing set)
            final HashSet<VariableAssignment> genSet = new HashSet<VariableAssignment>();

            final IBinding binding = definition.getBinding();

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

    HashMap<PMBlock, HashSet<VariableAssignment>> findKillSets() {

        final HashMap<PMBlock, HashSet<VariableAssignment>> result = new HashMap<PMBlock, HashSet<VariableAssignment>>();

        // Note: we populate the killsets by iterating through definitions
        // this means there will be no killset for a block with no definitions
        //
        for (final PMDef definition : this.definitions) {
            final IBinding binding = definition.getBinding();

            // Binding may be null if the declaring node for our lhs no longer
            // exists
            if (binding != null) {
                final HashSet<VariableAssignment> killSet = new HashSet<VariableAssignment>();

                // killset for an assignment is the "undefined" assignment plus
                // all the other assignments than this one

                killSet.add(uniqueVariableAssignment(null, binding)); // "undefined"
                                                                      // assignment

                for (final PMDef otherDefinition : this.definitionsByBinding.get(binding)) {
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

    protected void findUses() {

        this.usesByName = new HashMap<SimpleName, PMUse>();

        final Block body = this.methodDeclaration.getBody();

        final ASTVisitor visitor = new ASTVisitor() {

            @Override
            public boolean visit(final SimpleName name) {

                final PMBlock block = getBlockForNode(name);

                final Set<VariableAssignment> reachingDefinitions = PMRDefsAnalysis.this.reachingDefsOnEntry
                        .get(block);

                if (simpleNameIsUse(name)) {
                    final PMUse use = new PMUse(name);

                    PMRDefsAnalysis.this.usesByName.put(name, use);

                    final IBinding variableBinding = name.resolveBinding();

                    for (final VariableAssignment reachingDefinition : reachingDefinitions) {
                        if (reachingDefinition.getVariableBinding() == variableBinding) {
                            use.addReachingDefinition(reachingDefinition.getDefinition());
                        }
                    }
                }

                return true;
            }
        };

        body.accept(visitor);

    }

    protected ArrayList<PMBlock> generateBlocksForExpression(final Expression expression) {
        final ArrayList<PMBlock> result = new ArrayList<PMBlock>();

        if (expression instanceof Assignment) {
            final Assignment assignmentExpression = (Assignment) expression;

            final PMBlock block = new PMBlock();

            mergeBlockLists(result,
                    generateBlocksForExpression(assignmentExpression.getRightHandSide()));

            block.addNode(expression);

            addSerialBlockToEndOfList(block, result);
        } else if (expression instanceof VariableDeclarationExpression) {
            // add a block for reach fragment
            // don't currently handle complex code in initializers

            final VariableDeclarationExpression variableDeclarationExpression = (VariableDeclarationExpression) expression;

            for (final VariableDeclarationFragment fragment : fragments(variableDeclarationExpression)) {
                // really should generate blocks for fragment initializer

                final PMBlock block = new PMBlock();

                block.addNode(fragment);

                addSerialBlockToEndOfList(block, result);
            }
        } else {
            final PMBlock block = new PMBlock();

            block.addNode(expression);

            addSerialBlockToEndOfList(block, result);

        }

        return result;
    }

    protected ArrayList<PMBlock> generateBlocksForStatement(final Statement statement) {
        final ArrayList<PMBlock> result = new ArrayList<PMBlock>();

        if (statement instanceof ExpressionStatement) {
            final ExpressionStatement expressionStatement = (ExpressionStatement) statement;

            result.addAll(generateBlocksForExpression(expressionStatement.getExpression()));
        } else if (statement instanceof Block) {
            final Block blockStatement = (Block) statement;

            for (final Statement subStatement : statements(blockStatement)) {
                final ArrayList<PMBlock> blocksForSubStatement = generateBlocksForStatement(subStatement);

                mergeBlockLists(result, blocksForSubStatement);
            }
        } else if (statement instanceof IfStatement) {
            final IfStatement ifStatement = (IfStatement) statement;
            /*
             * three components: - guard block - then block - else block
             * 
             * - exit block to join then and else
             */

            final ArrayList<PMBlock> blocksForGuard = generateBlocksForExpression(ifStatement
                    .getExpression());
            final PMBlock endingGuardBlock = blocksForGuard.get(blocksForGuard.size() - 1);

            final PMBlock exitBlock = new PMBlock();

            mergeBlockLists(result, blocksForGuard);

            final ArrayList<PMBlock> blocksForThen = generateBlocksForStatement(ifStatement
                    .getThenStatement());

            // this will make a connection from the ending guard block to the
            // first then block
            mergeBlockLists(result, blocksForThen);

            final PMBlock endingThenBlock = blocksForThen.get(blocksForThen.size() - 1);
            endingThenBlock.addOutgoingBlock(exitBlock);

            if (ifStatement.getElseStatement() != null) {
                final ArrayList<PMBlock> blocksForElse = generateBlocksForStatement(ifStatement
                        .getElseStatement());

                // make connection from the ending guard block to the starting
                // else block
                // and from the ending else block to the exitBlock

                final PMBlock startingElseBlock = blocksForElse.get(0);

                endingGuardBlock.addOutgoingBlock(startingElseBlock);

                final PMBlock endingElseBlock = blocksForElse.get(blocksForElse.size() - 1);

                endingElseBlock.addOutgoingBlock(exitBlock);

                result.addAll(blocksForElse);
            } else {
                // No else block, so guard block may flow directly to exit block

                endingGuardBlock.addOutgoingBlock(exitBlock);
            }

            result.add(exitBlock);

        } else if (statement instanceof WhileStatement) {
            final WhileStatement whileStatement = (WhileStatement) statement;

            /*
             * while statements consist of: - guard condition - body
             * 
             * - synthetic exit block
             */

            final ArrayList<PMBlock> blocksForGuard = generateBlocksForExpression(whileStatement
                    .getExpression());
            final PMBlock startingGuardBlock = blocksForGuard.get(0);
            final PMBlock lastGuardBlock = blocksForGuard.get(blocksForGuard.size() - 1);

            final PMBlock exitBlock = new PMBlock();

            mergeBlockLists(result, blocksForGuard);

            final ArrayList<PMBlock> blocksForBody = generateBlocksForStatement(whileStatement
                    .getBody());

            mergeBlockLists(result, blocksForBody);

            // last block of body always flows to guard

            final PMBlock lastBodyBlock = blocksForBody.get(blocksForBody.size() - 1);
            lastBodyBlock.addOutgoingBlock(startingGuardBlock);

            // guard may fail and flow to exit
            lastGuardBlock.addOutgoingBlock(exitBlock);

            result.add(exitBlock);

        }

        // we need to add the statement itself to a block to maintain the
        // invariant that
        // every node has some ancestor that is in a block

        final PMBlock statementBlock = new PMBlock();

        statementBlock.addNode(statement);

        addSerialBlockToEndOfList(statementBlock, result);

        return result;
    }

    public ArrayList<PMBlock> getAllBlocks() {
        return this.allBlocks;
    }

    protected PMBlock getBlockForNode(ASTNode node) {
        final ASTNode originalNode = node;

        do {
            final PMBlock block = this.blocksByNode.get(node);

            if (block == null) {
                node = node.getParent();
            } else {
                return block;
            }

        } while (node != null);

        throw new RuntimeException("Couldn't find block for definingnode  " + originalNode);

    }

    public PMDef getDefinitionForDefiningNode(final ASTNode definingNode) {
        return this.definitionsByDefiningNode.get(definingNode);
    }

    public ArrayList<PMDef> getDefinitions() {
        return this.definitions;
    }

    public Collection<PMUse> getUses() {
        return this.usesByName.values();
    }

    boolean isAnalyzableLeftHandSide(final ASTNode lhs) {
        // for now we only support assignments to simple names

        return lhs instanceof SimpleName;
    }

    protected void mergeBlockLists(final ArrayList<PMBlock> first, final ArrayList<PMBlock> second) {
        // We assume the last block of the first list is followed sequentially
        // by the first block of the second list

        if (first.size() > 0) {
            first.get(first.size() - 1).addOutgoingBlock(second.get(0));
        }

        first.addAll(second);
    }

    void runAnalysis() {
        findDefinitions();
        findAllBlocks();

        final HashMap<PMBlock, HashSet<VariableAssignment>> genSets = findGenSets();
        final HashMap<PMBlock, HashSet<VariableAssignment>> killSets = findKillSets();

        // Forward analysis

        this.reachingDefsOnEntry = new HashMap<PMBlock, Set<VariableAssignment>>();
        this.reachingDefsOnExit = new HashMap<PMBlock, Set<VariableAssignment>>();

        final PMBlock initialBlock = this.allBlocks.get(0);

        for (final PMBlock block : this.allBlocks) {
            this.reachingDefsOnEntry.put(block, new HashSet<VariableAssignment>());

            if (block == initialBlock) {
                // add "undefined" assignments for all free variables in method

                this.methodDeclaration.accept(new ASTVisitor() {
                    @Override
                    public boolean visit(final SimpleName name) {

                        final IBinding binding = name.resolveBinding();
                        // We only care about names if they are variables (i.e.
                        // locals or fields)

                        if (binding instanceof IVariableBinding) {
                            PMRDefsAnalysis.this.reachingDefsOnEntry.get(block).add(
                                    uniqueVariableAssignment(null, binding));
                        }

                        return true;
                    }

                });

            }

            this.reachingDefsOnExit.put(block, new HashSet<VariableAssignment>());
        }
        boolean changed = false;
        do {
            // ?? do we need to make a copy of the entry/exit info and use these
            // or can we update in place??
            changed = false;

            for (final PMBlock block : this.allBlocks) {

                // entry prop
                if (block != initialBlock) {
                    final Set<VariableAssignment> newEntryReachingDefs = new HashSet<VariableAssignment>();

                    for (final PMBlock incomingBlock : block.getIncomingBlocks()) {
                        if (this.reachingDefsOnExit.get(incomingBlock) == null) {
                            System.out.println("Coulding find reaching defs for block "
                                    + incomingBlock);
                        }
                        newEntryReachingDefs.addAll(this.reachingDefsOnExit.get(incomingBlock));
                    }

                    if (!newEntryReachingDefs.equals(this.reachingDefsOnEntry.get(block))) {
                        changed = true;
                        this.reachingDefsOnEntry.put(block, newEntryReachingDefs);
                    }

                }

                // exit prop

                final Set<VariableAssignment> newExitReachingDefs = new HashSet<VariableAssignment>();

                newExitReachingDefs.addAll(this.reachingDefsOnEntry.get(block));

                final HashSet<VariableAssignment> killSet = killSets.get(block);

                if (killSet != null) {
                    newExitReachingDefs.removeAll(killSet);
                }

                final HashSet<VariableAssignment> genSet = genSets.get(block);

                if (genSet != null) {
                    newExitReachingDefs.addAll(genSet);
                }

                if (!newExitReachingDefs.equals(this.reachingDefsOnExit.get(block))) {
                    changed = true;
                    this.reachingDefsOnExit.put(block, newExitReachingDefs);
                }

            }

        } while (changed);

        /*
         * for (PMBlock block: _allBlocks) {
         * 
         * String output = "For [" + block + "]:\n";
         * 
         * output += "\tGen Set is:\n";
         * 
         * if (genSets.get(block) != null) { for (VariableAssignment
         * variableAssignment:genSets.get(block)) { output += "\t\t" +
         * variableAssignment.getDefinition().getDefiningNode() + " for [" +
         * variableAssignment.getVariableBinding() + "]\n"; } }
         * 
         * 
         * output += "\tKill Set is:\n";
         * 
         * if (killSets.get(block) != null) { for (VariableAssignment
         * variableAssignment:killSets.get(block)) { ASTNode definingNode = null;
         * 
         * if (variableAssignment.getDefinition() != null) definingNode =
         * variableAssignment.getDefinition().getDefiningNode();
         * 
         * output += "\t\t" + definingNode + " for [" + variableAssignment.getVariableBinding() +
         * "]\n"; } }
         * 
         * output += "\tReaching defs are:\n";
         * 
         * for (VariableAssignment variableAssignment: _reachingDefsOnEntry.get(block)) { ASTNode
         * definingNode = null;
         * 
         * if (variableAssignment.getDefinition() != null) definingNode =
         * variableAssignment.getDefinition().getDefiningNode();
         * 
         * output += "\t\t" + definingNode + " for [" + variableAssignment.getVariableBinding() +
         * "]\n"; }
         * 
         * 
         * 
         * System.out.println(output);
         * 
         * 
         * }
         */

        findUses();

        /*
         * System.out.println("Uses:");
         * 
         * 
         * for (PMUse use: _usesByName.values()) { String output = "" + use.getSimpleName() + "\n";
         * 
         * for (PMDef reachingDefinition: use.getReachingDefinitions()) {
         * 
         * ASTNode definingNode = null;
         * 
         * if (reachingDefinition != null) definingNode = reachingDefinition.getDefiningNode();
         * 
         * output += "\t" + definingNode; }
         * 
         * System.out.println(output); }
         */
    }

    protected boolean simpleNameIsUse(final SimpleName name) {
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
        } else if (parent instanceof VariableDeclaration
                && ((VariableDeclaration) parent).getName() == name) {
            return false;
        }

        return true;
    }

    VariableAssignment uniqueVariableAssignment(final PMDef definition,
            final IBinding variableBinding) {

        if (variableBinding == null) {
            throw new RuntimeException("variableBinding for " + definition + " is null!");
        }

        HashMap<IBinding, VariableAssignment> assignmentsForLocation = this.uniqueVariableAssigments
                .get(definition);

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

    // return PMUse object for a simple name, or null if the simpleName does not
    // represent a use
    public PMUse useForSimpleName(final SimpleName name) {
        return this.usesByName.get(name);
    }
}
