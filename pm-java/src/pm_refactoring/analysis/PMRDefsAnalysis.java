/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

*******************************************************************************/

package pm_refactoring.analysis;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.IfStatement;

import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.WhileStatement;

import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;

import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

public class PMRDefsAnalysis {
	protected MethodDeclaration _methodDeclaration;
	
	protected ArrayList<PMDef> _definitions;
	protected Map<IBinding, Set<PMDef>> _definitionsByBinding;
	
	protected Map<ASTNode, PMDef> _definitionsByDefiningNode;
	
	protected Map<SimpleName, PMUse> _usesByName;
	
	protected ArrayList<PMBlock> _allBlocks;
	
	protected Map<ASTNode, PMBlock> _blocksByNode;
	
	protected ArrayList<VariableDeclaration> _declarations;
	
	

	
	protected Map<PMBlock, Set<VariableAssignment>> _reachingDefsOnEntry;
	protected Map<PMBlock, Set<VariableAssignment>> _reachingDefsOnExit;
	
	HashMap<PMDef, HashMap<IBinding, VariableAssignment>> _uniqueVariableAssigments = new HashMap<PMDef, HashMap<IBinding, VariableAssignment>>();

	
	public PMRDefsAnalysis(MethodDeclaration methodDeclaration) {
		_methodDeclaration = methodDeclaration;
		
		runAnalysis(); //may wish to do this lazily
	}
	
	public static List<ASTNode> findDefiningNodesUnderNode(ASTNode rootNode) {
		final List<ASTNode> result = new ArrayList<ASTNode>();
		
		
		
		ASTVisitor visitor = new ASTVisitor() {
			
			boolean isAnalyzableLeftHandSide(ASTNode lhs) {
				//for now we only support assignments to simple names
				
				return lhs instanceof SimpleName;
			}
			
			public boolean visit(SingleVariableDeclaration singleVariableDeclaration) {
				//Used in parameter lists and catch clauses
				//There is an implicit definition here
				
				result.add(singleVariableDeclaration);
				
				return true;
			}
			
			public boolean visit(VariableDeclarationFragment variableDeclarationFragment) {
				//int x, y, z = 7; //etc
				
				result.add(variableDeclarationFragment);
				
				
				return true;
			}
			
			public boolean visit(Assignment assignment) {
				//plain-old x = y + 1
				
				if (isAnalyzableLeftHandSide(assignment.getLeftHandSide()))
					result.add(assignment);
				
				return true;
			}
			
			public boolean visit(PrefixExpression prefixExpression) {
				//Can't have things like ! being definitions
				if (prefixExpression.getOperator() == PrefixExpression.Operator.INCREMENT
					|| prefixExpression.getOperator() == PrefixExpression.Operator.DECREMENT) {
					
					//++x
					
					if (isAnalyzableLeftHandSide(prefixExpression.getOperand()))
						result.add(prefixExpression);
				}
				
				
				return true;
			}
			
			public boolean visit(PostfixExpression postfixExpression) {
				//all postfix operators are definitions
				//x++
				if (isAnalyzableLeftHandSide(postfixExpression.getOperand()))
					result.add(postfixExpression);
				return true;
			}
			
		};
		
		rootNode.accept(visitor);
		
		return result;
	}
	
	
	private void addDefinitionForNode(ASTNode node) {
		
		PMDef definition = new PMDef(node);
		
		_definitions.add(definition);
		_definitionsByDefiningNode.put(node, definition);
	}
	
	boolean isAnalyzableLeftHandSide(ASTNode lhs) {
		//for now we only support assignments to simple names
		
		return lhs instanceof SimpleName;
	}
	
	void findDefinitions() {
		_definitions = new ArrayList<PMDef>();
		_definitionsByDefiningNode = new HashMap<ASTNode, PMDef>();
		
		
		List<ASTNode> definingNodes = findDefiningNodesUnderNode(_methodDeclaration.getBody());
		
		for (ASTNode definingNode : definingNodes) {
			addDefinitionForNode(definingNode);
		}
		
		_definitionsByBinding = new HashMap<IBinding, Set<PMDef>>();
		
		
		for (PMDef def: _definitions) {
			
			IBinding binding = def.getBinding();
			
			Set<PMDef> definitionsForBinding = _definitionsByBinding.get(binding);
			
			if (definitionsForBinding == null) {
				definitionsForBinding = new HashSet<PMDef>();
				_definitionsByBinding.put(binding, definitionsForBinding);
			}
			
			definitionsForBinding.add(def);
		}
	}
	
	public ArrayList<PMDef> getDefinitions() {
		return _definitions;
	}
	
	public PMDef getDefinitionForDefiningNode(ASTNode definingNode) {
		return _definitionsByDefiningNode.get(definingNode);
	}
	
	public Collection<PMUse> getUses() {
		return _usesByName.values();
	}
	
	//return PMUse object for a simple name, or null if the simpleName does not represent a use
	public PMUse useForSimpleName(SimpleName name) {
		return _usesByName.get(name);
	}
	
	
	
	protected boolean astNodeContainsDefinition(ASTNode node) {
		
		//could do this more efficiently
		
		
		final boolean[] containsDefinition = new boolean[1];
		containsDefinition[0] = false; //to get around final problem
		
		node.accept(new ASTVisitor() {
			public boolean visit(Assignment assignment) {
				containsDefinition[0] = true;
				
				return true;
			}
			
			public boolean visit(SingleVariableDeclaration singleVariableDeclaration) {
				containsDefinition[0] = true;
				
				return true;
			}
			
			public boolean visit(VariableDeclarationFragment variableDeclarationFragment) {
				containsDefinition[0] = true;
				
				return true;
			}
			
		});
		
		return containsDefinition[0];
	}
	
	
	protected void addSerialBlockToEndOfList(PMBlock block, ArrayList<PMBlock> blockList) {
		if (blockList.size() > 0) {
			PMBlock lastBlock = blockList.get(blockList.size() - 1);
			
			lastBlock.addOutgoingBlock(block);
		}
		
		blockList.add(block);
	}
	
	protected void mergeBlockLists(ArrayList<PMBlock> first, ArrayList<PMBlock> second) {
		//We assume the last block of the first list is followed sequentially by the first block of the second list
		
		if (first.size() > 0) {
			first.get(first.size() - 1).addOutgoingBlock(second.get(0));
		}
		
		
		first.addAll(second);
	}
	
	protected ArrayList<PMBlock> generateBlocksForExpression(Expression expression) {
		ArrayList<PMBlock> result = new ArrayList<PMBlock>();
		
		if (expression instanceof Assignment) {
			Assignment assignmentExpression = (Assignment)expression;
			
			PMBlock block = new PMBlock();
			
			mergeBlockLists(result, generateBlocksForExpression(assignmentExpression.getRightHandSide()));
			
			block.addNode(expression); 
			
			addSerialBlockToEndOfList(block, result);
		} else if (expression instanceof VariableDeclarationExpression) {
			//add a block for reach fragment
			// don't currently handle complex code in initializers
			
			VariableDeclarationExpression variableDeclarationExpression = (VariableDeclarationExpression)expression;
			
			for (VariableDeclarationFragment fragment: (List<VariableDeclarationFragment>)variableDeclarationExpression.fragments()) {
				//really should generate blocks for fragment initializer
				
				PMBlock block = new PMBlock();
				
				block.addNode(fragment);
				
				addSerialBlockToEndOfList(block, result);
			}
		} else {
			PMBlock block = new PMBlock();
			
			block.addNode(expression);
			
			addSerialBlockToEndOfList(block, result);
			
			
		}
		
		return result;
	}
	
	protected ArrayList<PMBlock> generateBlocksForStatement(Statement statement) {
		ArrayList<PMBlock> result = new ArrayList<PMBlock>();
		
		if (statement instanceof ExpressionStatement) {
			ExpressionStatement expressionStatement = (ExpressionStatement)statement;
			
			result.addAll(generateBlocksForExpression(expressionStatement.getExpression()));
		} else if (statement instanceof Block) {
			Block blockStatement = (Block)statement;
			
			for (Statement subStatement: (List<Statement>)blockStatement.statements()) {
				ArrayList<PMBlock> blocksForSubStatement = generateBlocksForStatement(subStatement);
				
				mergeBlockLists(result, blocksForSubStatement);
			}
		} else if (statement instanceof IfStatement) {
			IfStatement ifStatement = (IfStatement)statement;
			/* three components:
				- guard block
				- then block
				- else block
				
				- exit block to join then and else
			*/
			
			ArrayList<PMBlock> blocksForGuard = generateBlocksForExpression(ifStatement.getExpression());
			PMBlock endingGuardBlock = blocksForGuard.get(blocksForGuard.size() - 1);
			
			PMBlock exitBlock = new PMBlock();
			
			mergeBlockLists(result, blocksForGuard);
			
			
			ArrayList<PMBlock> blocksForThen = generateBlocksForStatement(ifStatement.getThenStatement());
			
			//this will make a connection from the ending guard block to the first then block
			mergeBlockLists(result, blocksForThen); 
			
			
			PMBlock endingThenBlock = (PMBlock)blocksForThen.get(blocksForThen.size() - 1);
			endingThenBlock.addOutgoingBlock(exitBlock);
			
			if (ifStatement.getElseStatement() != null) {
				ArrayList<PMBlock> blocksForElse = generateBlocksForStatement(ifStatement.getElseStatement());
				
				//make connection from the ending guard block to the starting else block
				//and from the ending else block to the exitBlock 
				
				PMBlock startingElseBlock = (PMBlock)blocksForElse.get(0);
				
				endingGuardBlock.addOutgoingBlock(startingElseBlock);
				
				
				PMBlock endingElseBlock = (PMBlock)blocksForElse.get(blocksForElse.size() - 1);
				
				endingElseBlock.addOutgoingBlock(exitBlock);
			
				result.addAll(blocksForElse);
			} else {
				//No else block, so guard block may flow directly to exit block
				
				endingGuardBlock.addOutgoingBlock(exitBlock);
			}
			
			
			result.add(exitBlock);
			
			
		} else if (statement instanceof WhileStatement) {
			WhileStatement whileStatement = (WhileStatement)statement;
			
			/* while statements consist of:
			 * 	- guard condition
			 *  - body
			 *  
			 *  - synthetic exit block
			 */
			
			
			ArrayList<PMBlock> blocksForGuard = generateBlocksForExpression(whileStatement.getExpression());
			PMBlock startingGuardBlock = blocksForGuard.get(0);
			PMBlock lastGuardBlock = blocksForGuard.get(blocksForGuard.size() - 1);
			
			PMBlock exitBlock = new PMBlock();
			
			mergeBlockLists(result, blocksForGuard);
			
			
			ArrayList<PMBlock> blocksForBody = generateBlocksForStatement(whileStatement.getBody());
			
			mergeBlockLists(result, blocksForBody);
			
			//last block of body always flows to guard
			
			PMBlock lastBodyBlock = blocksForBody.get(blocksForBody.size() - 1);
			lastBodyBlock.addOutgoingBlock(startingGuardBlock);
			
			//guard may fail and flow to exit
			lastGuardBlock.addOutgoingBlock(exitBlock);
			
			result.add(exitBlock);
			
			
		}
		
		
		//we need to add the statement itself to a block to maintain the invariant that 
		//every node has some ancestor that is in a block
		
		PMBlock statementBlock = new PMBlock();
		
		statementBlock.addNode(statement);
		
		addSerialBlockToEndOfList(statementBlock, result);
		
		return result;
	}
	
	void findAllBlocks() {
		
		_allBlocks = new ArrayList<PMBlock>();
		
		
		_allBlocks.add(new PMBlock()); //synthetic initial block;
		
		mergeBlockLists(_allBlocks,generateBlocksForStatement(_methodDeclaration.getBody()));
		
		
		//fill in _blocksByNode
		//Every node should have at least one ancestor that has a block according to this hash
		
		_blocksByNode = new HashMap<ASTNode, PMBlock>();
		
		for (PMBlock block : _allBlocks) {
			for (ASTNode node : block.getNodes()) {
				_blocksByNode.put(node, block);
			}
			
		}
	}
	
	public ArrayList<PMBlock> getAllBlocks() {
		return _allBlocks;
	}
	
	
	
protected PMBlock getBlockForNode(ASTNode node) {
	ASTNode originalNode = node;
	
	do {
		PMBlock block = _blocksByNode.get(node);
		
		if (block == null) 
			node = node.getParent();
		else 
			return block;
		
	} while (node != null);
	
	throw new RuntimeException("Couldn't find block for definingnode  " + originalNode);
	
}


HashMap<PMBlock, HashSet<VariableAssignment>> findGenSets() {
		
		HashMap<PMBlock, HashSet<VariableAssignment>> result = new HashMap<PMBlock, HashSet<VariableAssignment>>();
		
		for (PMDef definition : _definitions) {
			
			//Create singleton set for gen set (could probably dispense w/ containing set)
			HashSet<VariableAssignment> genSet = new HashSet<VariableAssignment>();
			
			IBinding binding = definition.getBinding();
			
			//The binding could be null if the declaration for the lhs no longer exists
			
			if (binding != null) {
				genSet.add(uniqueVariableAssignment(definition, binding));
				
				
				
				PMBlock block = getBlockForNode(definition.getDefiningNode());
				
				
				result.put(block, genSet);
			}
		}
		
		return result;
	}


	
HashMap<PMBlock, HashSet<VariableAssignment>> findKillSets() {
	
	HashMap<PMBlock, HashSet<VariableAssignment>> result = new HashMap<PMBlock, HashSet<VariableAssignment>>();
	
	
	//Note: we populate the killsets by iterating through definitions
	//this means there will be no killset for a block with no definitions
	//
	for (PMDef definition : _definitions) {
		IBinding binding = definition.getBinding();
		
		
		//Binding may be null if the declaring node for our lhs no longer exists
		if (binding != null) {
			HashSet<VariableAssignment> killSet = new HashSet<VariableAssignment>();
			
			//killset for an assignment is the "undefined" assignment plus all the other assignments than this one
			
			killSet.add(uniqueVariableAssignment(null, binding)); //"undefined" assignment
			
			for (PMDef otherDefinition: _definitionsByBinding.get(binding)) {
				if (otherDefinition != definition) {
					killSet.add(uniqueVariableAssignment(otherDefinition, binding));
				}
			}
			
			PMBlock block = getBlockForNode(definition.getDefiningNode());
			
			result.put(block, killSet);
		}
		
	}
	
	return result;
}

	protected boolean simpleNameIsUse(SimpleName name) {
		/* we assume all simple names are uses except:
		 * 
		 *  the lhs of Assignment expressions
		 *  the name of a VariableDeclarationFragment
		 *  the name of a SingleVariableDeclaration
		 *  
		 *  There are probably more cases (i.e. method names in invocations?)
		 */
		
		ASTNode parent = name.getParent();
		
		if (parent instanceof Assignment && ((Assignment)parent).getLeftHandSide() == name)
			return false;
		else if (parent instanceof VariableDeclaration && ((VariableDeclaration)parent).getName() == name) 
			return false;
		
		
		return true;
	}
	protected void findUses() {
		
		_usesByName = new HashMap<SimpleName, PMUse>();
		
		Block body = _methodDeclaration.getBody();
		
		
		
			
		
		
	
		ASTVisitor visitor = new ASTVisitor() {
			
			public boolean visit(SimpleName name) {
				
				PMBlock block = getBlockForNode(name);
				
				Set<VariableAssignment>  reachingDefinitions = _reachingDefsOnEntry.get(block);
				
				if (simpleNameIsUse(name)) {
					PMUse use = new PMUse(name);
					
					_usesByName.put(name, use);
					
					IBinding variableBinding = name.resolveBinding();
					

					for (VariableAssignment reachingDefinition: reachingDefinitions) {
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

	void runAnalysis() {
		findDefinitions();
		findAllBlocks();
		
		HashMap<PMBlock, HashSet<VariableAssignment>> genSets = findGenSets();
		HashMap<PMBlock, HashSet<VariableAssignment>> killSets = findKillSets();
		
		
		//Forward analysis
		
		_reachingDefsOnEntry = new HashMap<PMBlock, Set<VariableAssignment>>();
		_reachingDefsOnExit = new HashMap<PMBlock, Set<VariableAssignment>>();
		
		
		
		
		
		
		PMBlock initialBlock = _allBlocks.get(0);
		
		for (final PMBlock block : _allBlocks) {
			_reachingDefsOnEntry.put(block, new HashSet<VariableAssignment>());
			
			if (block == initialBlock) {
				//add "undefined" assignments for all free variables in method
				
				
				_methodDeclaration.accept(new ASTVisitor() {
					public boolean visit(SimpleName name) {
						
						IBinding binding = name.resolveBinding();
						//We only care about names if they are variables (i.e. locals or fields)
						
						if (binding instanceof IVariableBinding)
							_reachingDefsOnEntry.get(block).add(uniqueVariableAssignment(null, binding));
						
						return true;
					}
					
				});
				
				
			}
			
			
			_reachingDefsOnExit.put(block, new HashSet<VariableAssignment>());
		}
		boolean changed = false;
		do {
			//?? do we need to make a copy of the entry/exit info and use these
			// or can we update in place??
			changed = false;
			
			for (PMBlock block : _allBlocks) {
				
				//entry prop
				if (block != initialBlock) {
					Set<VariableAssignment> newEntryReachingDefs = new HashSet<VariableAssignment>();
					
					for (PMBlock incomingBlock: block.getIncomingBlocks()) {
						if (_reachingDefsOnExit.get(incomingBlock) == null) {
							System.out.println("Coulding find reaching defs for block " + incomingBlock);
						}
						newEntryReachingDefs.addAll(_reachingDefsOnExit.get(incomingBlock));
					}
					
					
					if (!newEntryReachingDefs.equals(_reachingDefsOnEntry.get(block))) {
						changed = true;
						_reachingDefsOnEntry.put(block, newEntryReachingDefs);
					}
						
				}
				
				//exit prop
				
				Set<VariableAssignment> newExitReachingDefs = new HashSet<VariableAssignment>();
				
				newExitReachingDefs.addAll(_reachingDefsOnEntry.get(block));
				
				HashSet<VariableAssignment> killSet = killSets.get(block);
				
				if (killSet != null)
					newExitReachingDefs.removeAll(killSet);
				
				HashSet<VariableAssignment> genSet = genSets.get(block);
				
				if (genSet != null)
					newExitReachingDefs.addAll(genSet);
				
				if (!newExitReachingDefs.equals(_reachingDefsOnExit.get(block))) {
					changed = true;
					_reachingDefsOnExit.put(block, newExitReachingDefs);
				}
				
			}
			
			
		} while (changed == true);
		
		/*
		for (PMBlock block: _allBlocks) {
			
			String output = "For [" + block + "]:\n";
			
			output += "\tGen Set is:\n";
			
			if (genSets.get(block) != null) {
				for (VariableAssignment variableAssignment:genSets.get(block)) {
					output += "\t\t" + variableAssignment.getDefinition().getDefiningNode() + " for [" + variableAssignment.getVariableBinding() + "]\n";
				}
			}
			

			output += "\tKill Set is:\n";
			
			if (killSets.get(block) != null) {
				for (VariableAssignment variableAssignment:killSets.get(block)) {
					ASTNode definingNode = null;
					
					if (variableAssignment.getDefinition() != null)
						definingNode = variableAssignment.getDefinition().getDefiningNode();
					
					output += "\t\t" +  definingNode + " for [" + variableAssignment.getVariableBinding() + "]\n";
				}
			}
			
			output += "\tReaching defs are:\n";
			
			for (VariableAssignment variableAssignment: _reachingDefsOnEntry.get(block)) {
				ASTNode definingNode = null;
				
				if (variableAssignment.getDefinition() != null)
					definingNode = variableAssignment.getDefinition().getDefiningNode();
				
				output += "\t\t" + definingNode + " for [" + variableAssignment.getVariableBinding() + "]\n";
			}
			
			
			
			System.out.println(output);
			
			
		}
		*/
		
		
		findUses();
		
		/*
		System.out.println("Uses:");
		
		
		for (PMUse use: _usesByName.values()) {
			String output = "" + use.getSimpleName() + "\n";
			
			for (PMDef reachingDefinition: use.getReachingDefinitions()) {
				
				ASTNode definingNode = null;
				
				if (reachingDefinition != null)
					definingNode = reachingDefinition.getDefiningNode();
					
				output += "\t" + definingNode;
			}
			
			System.out.println(output);
		}
		
		*/
	}
	
		
	VariableAssignment uniqueVariableAssignment(PMDef definition, IBinding variableBinding) {
		
		
		if (variableBinding == null)
			throw new RuntimeException("variableBinding for " + definition + " is null!");
		
		HashMap<IBinding, VariableAssignment> assignmentsForLocation = _uniqueVariableAssigments.get(definition);
		
		if (assignmentsForLocation == null) {
			assignmentsForLocation = new HashMap<IBinding, VariableAssignment>();
			_uniqueVariableAssigments.put(definition, assignmentsForLocation);
		}
		
		VariableAssignment variableAssignment = assignmentsForLocation.get(variableBinding);
		
		if (variableAssignment == null) {
			variableAssignment = new VariableAssignment(definition, variableBinding);
			assignmentsForLocation.put(variableBinding, variableAssignment);
		}
		
		return variableAssignment;
	}
	
	protected static class VariableAssignment {
		
		protected PMDef _definition;
		
		protected IBinding _variableBinding;
		
		public VariableAssignment(PMDef definition, IBinding variableBinding) {
			_definition = definition;
			_variableBinding = variableBinding;
		}
		
		public PMDef getDefinition() {
			return _definition;
		}
	
		public IBinding getVariableBinding() {
			return _variableBinding;
		}
	}
}
