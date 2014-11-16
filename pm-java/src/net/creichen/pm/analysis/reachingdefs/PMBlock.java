/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm.analysis.reachingdefs;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTNode;

class PMBlock {
    private final List<ASTNode> nodes;
    private final Set<PMBlock> previousBlocks;
    private Set<ReachingDefinition> in;
    private Set<ReachingDefinition> out;
    private ReachingDefinition gen;
    private Set<ReachingDefinition> killSet;

    PMBlock() {
        this.nodes = new ArrayList<ASTNode>();
        this.previousBlocks = new HashSet<PMBlock>();
        this.in = new HashSet<ReachingDefinition>();
        this.out = new HashSet<ReachingDefinition>();
        this.killSet = new HashSet<ReachingDefinition>();
    }

    void addNode(final ASTNode node) {
        this.nodes.add(node);
    }

    List<ASTNode> getNodes() {
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

    void addPrevious(final PMBlock block) {
        this.previousBlocks.add(block);
    }

    final Set<ReachingDefinition> getIn() {
        return this.in;
    }

    void setGen(ReachingDefinition assignment) {
        this.gen = assignment;
    }

    void setKillSet(Set<ReachingDefinition> killSet) {
        this.killSet = killSet;
    }

    boolean updateOut() {
        final Set<ReachingDefinition> newOut = new HashSet<ReachingDefinition>();
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
        final Set<ReachingDefinition> newIn = new HashSet<ReachingDefinition>();
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
