/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm.analysis;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTNode;

public class PMBlock {
	protected ArrayList<ASTNode> _nodes;

	protected Set<PMBlock> _incomingBlocks;
	protected Set<PMBlock> _outgoingBlocks;

	public PMBlock() {
		_nodes = new ArrayList<ASTNode>();

		_incomingBlocks = new HashSet<PMBlock>();
		_outgoingBlocks = new HashSet<PMBlock>();
	}

	public ArrayList<ASTNode> getNodes() {
		return _nodes;
	}

	public void addNode(ASTNode node) {
		_nodes.add(node);
	}

	public Set<PMBlock> getIncomingBlocks() {
		return _incomingBlocks;
	}

	public void addIncomingBlock(PMBlock block) {

		if (_incomingBlocks.add(block)) {
			block.addOutgoingBlock(this);
		}
	}

	public Set<PMBlock> getOutgoingBlocks() {
		return _outgoingBlocks;
	}

	public void addOutgoingBlock(PMBlock block) {
		if (_outgoingBlocks.add(block)) {
			block.addIncomingBlock(this);
		}
	}

	public String toString() {
		String result = "";

		int i = 0;

		for (ASTNode node : getNodes()) {
			result = +i++ + ": " + node + "\n";
		}

		return result;
	}

}
