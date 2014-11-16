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
    private final Set<PMBlock> previousBlocks;
    private final Set<PMBlock> outgoingBlocks;
    private final Set<VariableAssignment> reachingDefsOnEntry;
    private Set<VariableAssignment> reachingDefsOnExit;
    private VariableAssignment gen;
    private Set<VariableAssignment> killSet;

    public PMBlock() {
        this.nodes = new ArrayList<ASTNode>();

        this.previousBlocks = new HashSet<PMBlock>();
        this.outgoingBlocks = new HashSet<PMBlock>();
        this.reachingDefsOnEntry = new HashSet<VariableAssignment>();
        this.reachingDefsOnExit = new HashSet<VariableAssignment>();
    }

    public void addNode(final ASTNode node) {
        this.nodes.add(node);
    }

    public Set<PMBlock> getPreviousBlocks() {
        return this.previousBlocks;
    }

    public List<ASTNode> getNodes() {
        return this.nodes;
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

    public void addPrevious(final PMBlock block) {
        this.previousBlocks.add(block);
    }

    protected final Set<VariableAssignment> getReachingDefsOnEntry() {
        return this.reachingDefsOnEntry;
    }

    protected final Set<VariableAssignment> getReachingDefsOnExit() {
        return this.reachingDefsOnExit;
    }

    public VariableAssignment getGen() {
        return this.gen;
    }

    public void setGen(VariableAssignment assignment) {
        this.gen = assignment;
    }

    public Set<VariableAssignment> getKillSet() {
        return this.killSet;
    }

    public void setKillSet(Set<VariableAssignment> killSet) {
        this.killSet = killSet;
    }
}
