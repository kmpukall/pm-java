package net.creichen.pm.analysis;

import static net.creichen.pm.utils.APIWrapperUtil.fragments;
import static net.creichen.pm.utils.APIWrapperUtil.statements;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.creichen.pm.core.PMException;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.WhileStatement;

class BlockResolver {

    private List<PMBlock> blocks;
    private Map<ASTNode, PMBlock> blocksByNode;

    public final List<PMBlock> getBlocks() {
        return this.blocks;
    }

    private void createMapping() {
        this.blocksByNode = new HashMap<ASTNode, PMBlock>();
        for (final PMBlock block : this.blocks) {
            for (final ASTNode node : block.getNodes()) {
                this.blocksByNode.put(node, block);
            }
        }
    }

    public void resolve(MethodDeclaration methodDeclaration) {
        this.blocks = new ArrayList<PMBlock>();
        this.blocks.add(new PMBlock()); // synthetic initial block;
        List<PMBlock> blocksFromMethod = generateBlocks(methodDeclaration.getBody());
        appendBlocks(blocksFromMethod);
        createMapping();
    }

    private void appendBlock(final List<PMBlock> target, final PMBlock block) {
        appendBlockList(target, Arrays.asList(block));
    }

    private void appendBlockList(final List<PMBlock> target, final List<PMBlock> list) {
        // We assume the last block of the first list is followed sequentially
        // by the first block of the second list

        if (target.size() > 0) {
            target.get(target.size() - 1).addOutgoingBlock(list.get(0));
        }

        target.addAll(list);
    }

    private void appendBlocks(List<PMBlock> blocksFromMethod) {
        appendBlockList(this.blocks, blocksFromMethod);
    }

    private List<PMBlock> generateBlocks(final Expression expression) {
        final List<PMBlock> result = new ArrayList<PMBlock>();

        if (expression instanceof Assignment) {
            final Assignment assignmentExpression = (Assignment) expression;
            appendBlockList(result, generateBlocks(assignmentExpression.getRightHandSide()));

            final PMBlock block = new PMBlock();
            block.addNode(expression);
            appendBlock(result, block);
        } else if (expression instanceof VariableDeclarationExpression) {
            // add a block for each fragment
            // don't currently handle complex code in initializers
            List<PMBlock> blocksFromVariableDeclaration = generateBlocks((VariableDeclarationExpression) expression);
            appendBlockList(result, blocksFromVariableDeclaration);
        } else {
            final PMBlock block = new PMBlock();
            block.addNode(expression);
            appendBlock(result, block);
        }

        return result;
    }

    private List<PMBlock> generateBlocks(final IfStatement ifStatement) {
        final List<PMBlock> result = new ArrayList<PMBlock>();
        /*
         * three components: - guard block - then block - else block
         *
         * - exit block to join then and else
         */

        final List<PMBlock> blocksForGuard = generateBlocks(ifStatement.getExpression());
        final PMBlock endingGuardBlock = blocksForGuard.get(blocksForGuard.size() - 1);

        final PMBlock exitBlock = new PMBlock();

        appendBlockList(result, blocksForGuard);

        final List<PMBlock> blocksForThen = generateBlocks(ifStatement.getThenStatement());

        // this will make a connection from the ending guard block to the
        // first then block
        appendBlockList(result, blocksForThen);

        final PMBlock endingThenBlock = blocksForThen.get(blocksForThen.size() - 1);
        endingThenBlock.addOutgoingBlock(exitBlock);

        if (ifStatement.getElseStatement() != null) {
            final List<PMBlock> blocksForElse = generateBlocks(ifStatement.getElseStatement());

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
            // this assumes the condition is not tautological

            endingGuardBlock.addOutgoingBlock(exitBlock);
        }

        result.add(exitBlock);

        return result;
    }

    private List<PMBlock> generateBlocks(final Statement statement) {
        final List<PMBlock> result = new ArrayList<PMBlock>();

        if (statement instanceof ExpressionStatement) {
            final ExpressionStatement expressionStatement = (ExpressionStatement) statement;
            List<PMBlock> blocksFromExpression = generateBlocks(expressionStatement.getExpression());
            result.addAll(blocksFromExpression);
        } else if (statement instanceof Block) {
            final Block blockStatement = (Block) statement;
            for (final Statement subStatement : statements(blockStatement)) {
                final List<PMBlock> blocksForSubStatement = generateBlocks(subStatement);
                appendBlockList(result, blocksForSubStatement);
            }
        } else if (statement instanceof IfStatement) {
            List<PMBlock> blocksForIfStatement = generateBlocks((IfStatement) statement);
            appendBlockList(result, blocksForIfStatement);
        } else if (statement instanceof WhileStatement) {
            List<PMBlock> blocksForWhileStatement = generateBlocks((WhileStatement) statement);
            appendBlockList(result, blocksForWhileStatement);
        }

        // we need to add the statement itself to a block to maintain the
        // invariant that
        // every node has some ancestor that is in a block
        final PMBlock statementBlock = new PMBlock();
        statementBlock.addNode(statement);
        appendBlock(result, statementBlock);

        return result;
    }

    private List<PMBlock> generateBlocks(final VariableDeclarationExpression variableDeclarationExpression) {
        final List<PMBlock> result = new ArrayList<PMBlock>();
        for (final VariableDeclarationFragment fragment : fragments(variableDeclarationExpression)) {
            // really should generate blocks for fragment initializer
            final PMBlock block = new PMBlock();
            block.addNode(fragment);
            appendBlock(result, block);
        }
        return result;
    }

    private List<PMBlock> generateBlocks(final WhileStatement whileStatement) {
        final List<PMBlock> result = new ArrayList<PMBlock>();
        /*
         * while statements consist of: - guard condition - body
         *
         * - synthetic exit block
         */

        final List<PMBlock> blocksForGuard = generateBlocks(whileStatement.getExpression());
        final PMBlock startingGuardBlock = blocksForGuard.get(0);
        final PMBlock lastGuardBlock = blocksForGuard.get(blocksForGuard.size() - 1);

        final PMBlock exitBlock = new PMBlock();

        appendBlockList(result, blocksForGuard);

        final List<PMBlock> blocksForBody = generateBlocks(whileStatement.getBody());

        appendBlockList(result, blocksForBody);

        // last block of body always flows to guard

        final PMBlock lastBodyBlock = blocksForBody.get(blocksForBody.size() - 1);
        lastBodyBlock.addOutgoingBlock(startingGuardBlock);

        // guard may fail and flow to exit
        lastGuardBlock.addOutgoingBlock(exitBlock);

        result.add(exitBlock);

        return result;
    }

    public PMBlock getBlockForNode(final ASTNode originalNode) {
        ASTNode node = originalNode;
        do {
            final PMBlock block = this.blocksByNode.get(node);
            if (block != null) {
                return block;
            }
            node = node.getParent();
        } while (node != null);
        throw new PMException("Couldn't find block for definingnode  " + originalNode);
    }

}
