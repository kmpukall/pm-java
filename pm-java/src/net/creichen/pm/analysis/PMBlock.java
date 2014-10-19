/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm.analysis;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTNode;

class PMBlock {
    private final List<ASTNode> nodes;
    private final Set<PMBlock> incomingBlocks;
    private final Set<PMBlock> outgoingBlocks;
    private final Set<VariableAssignment> reachingDefsOnEntry;
    private Set<VariableAssignment> reachingDefsOnExit;

    public PMBlock() {
        this.nodes = new ArrayList<ASTNode>();

        this.incomingBlocks = new HashSet<PMBlock>();
        this.outgoingBlocks = new HashSet<PMBlock>();
        this.reachingDefsOnEntry = new HashSet<VariableAssignment>();
        this.reachingDefsOnExit = new HashSet<VariableAssignment>();
    }

    public void addNode(final ASTNode node) {
        this.nodes.add(node);
    }

    public void addOutgoingBlock(final PMBlock block) {
        if (this.outgoingBlocks.add(block)) {
            block.addIncomingBlock(this);
        }
    }

    public Set<PMBlock> getIncomingBlocks() {
        return this.incomingBlocks;
    }

    public List<ASTNode> getNodes() {
        return this.nodes;
    }

    public Set<PMBlock> getOutgoingBlocks() {
        return this.outgoingBlocks;
    }

    @Override
    public String toString() {
        String result = "";
        int i = 0;
        for (final ASTNode node : getNodes()) {
            result = +i++ + ": " + node + "\n";
        }
        return result;
    }

    private void addIncomingBlock(final PMBlock block) {
        if (this.incomingBlocks.add(block)) {
            block.addOutgoingBlock(this);
        }
    }

    protected final Set<VariableAssignment> getReachingDefsOnEntry() {
        return this.reachingDefsOnEntry;
    }

    protected final Set<VariableAssignment> getReachingDefsOnExit() {
        return this.reachingDefsOnExit;
    }
}
