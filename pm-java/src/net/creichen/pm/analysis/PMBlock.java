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
    private Set<VariableAssignment> in;
    private Set<VariableAssignment> out;
    private VariableAssignment gen;
    private Set<VariableAssignment> killSet;

    public PMBlock() {
        this.nodes = new ArrayList<ASTNode>();
        this.previousBlocks = new HashSet<PMBlock>();
        this.in = new HashSet<VariableAssignment>();
        this.out = new HashSet<VariableAssignment>();
        this.killSet = new HashSet<VariableAssignment>();
    }

    public void addNode(final ASTNode node) {
        this.nodes.add(node);
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

    protected final Set<VariableAssignment> getIn() {
        return this.in;
    }

    public void setGen(VariableAssignment assignment) {
        this.gen = assignment;
    }

    public void setKillSet(Set<VariableAssignment> killSet) {
        this.killSet = killSet;
    }

    boolean updateOut() {
        final Set<VariableAssignment> newOut = new HashSet<VariableAssignment>();
        newOut.addAll(this.in);
        newOut.removeAll(this.killSet);
        if (this.gen != null) {
            newOut.add(this.gen);
        }
        if (!newOut.equals(this.out)) {
            this.out = newOut;
            return true;
        }
        return false;
    }

    boolean updateIn() {
        final Set<VariableAssignment> newIn = new HashSet<VariableAssignment>();
        for (final PMBlock incomingBlock : this.previousBlocks) {
            newIn.addAll(incomingBlock.out);
        }
        if (!newIn.equals(this.in)) {
            this.in = newIn;
            return true;
        }
        return false;
    }
}
