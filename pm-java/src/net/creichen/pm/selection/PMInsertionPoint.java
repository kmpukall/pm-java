/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

*******************************************************************************/

package net.creichen.pm.selection;

import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.ChildListPropertyDescriptor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.TypeDeclaration;


//This class is a mess

public class PMInsertionPoint {
	CompilationUnit _compilationUnit;
	int _offset;
	
	
	int _insertionIndex;
	ChildListPropertyDescriptor _insertionProperty;
	ASTNode _insertionParent;
	
	public PMInsertionPoint(CompilationUnit compilationUnit, int offset) {
		_insertionIndex = -1;
		
		_compilationUnit = compilationUnit;
		
		_offset = offset;
		
		if (!findInsertionPointInBlock(_offset)) {
			findInsertionPointInTypeDeclaration(_offset);
		}
	}
	
	public boolean isSaneInsertionPoint() {
		return _insertionIndex != -1;
	}
	
	public int insertionIndex() {
		return _insertionIndex;
	}
	
	public ChildListPropertyDescriptor insertionProperty() {
		return _insertionProperty;
	}
	
	public ASTNode insertionParent() {
		return _insertionParent;
	}
	
	private boolean findInsertionPointUnderNode(ASTNode parentNode, ChildListPropertyDescriptor property, int offset) {
		List<ASTNode> statements = (List<ASTNode>)parentNode.getStructuralProperty(property);
		
		int statementCount = statements.size();
		
		if (statementCount > 0) {
			if (offset <= statements.get(0).getStartPosition())
				_insertionIndex = 0;
			else {
				ASTNode lastStatement = statements.get(statementCount - 1);
				
				if (offset >= lastStatement.getStartPosition() + lastStatement.getLength()) {
					_insertionIndex = statementCount;
				} else {
					//offset is not before the first statement, or after the last statement, so see if it is between two statements
					
					for (int i = 1; i < statementCount; i++) {
						ASTNode statement1 = statements.get(i - 1);
						ASTNode statement2 = statements.get(i);
						
						
						if (offset >= statement1.getStartPosition() + statement1.getLength()
							&& offset <= statement2.getStartPosition()) {
							
							
							
							_insertionIndex = i;
							break;
						}
					}
				}
			}
		} else {
			_insertionIndex = 0;
		}
		
		if (_insertionIndex != -1) {
			_insertionParent = parentNode;
			_insertionProperty = property;
			return true;
		} else {
			_insertionParent = null;
			_insertionProperty = null;
			
			return false;
		}
		
	}
	

	
	private boolean findInsertionPointInBlock(int offset) {
				
		/* a point is an insertion point if:
		
		- it is in a block or a type declaration
		- it is NOT in a child of the above
		- OR it is the first/last character of such a child
		 */
		
		Block containingBlock = FindContainingBlockForSelection(_compilationUnit, offset, 0);
		
		if (containingBlock != null) {
			return findInsertionPointUnderNode(containingBlock, Block.STATEMENTS_PROPERTY, offset);
			
		} else {
			_insertionIndex = -1;
			
			return false;
		}
	}
	
	private boolean findInsertionPointInTypeDeclaration(int offset) {
		
		TypeDeclaration containingTypeDeclaration = FindContainingTypeDeclarationForSelection(_compilationUnit, offset, 0);
		
		if (containingTypeDeclaration != null) {
			return findInsertionPointUnderNode(containingTypeDeclaration, TypeDeclaration.BODY_DECLARATIONS_PROPERTY, offset);
			
		} else {
			System.err.println("Couldn't find containing type declaration");
			_insertionIndex = -1;
			
			return false;
		}
	}
	
	
	static protected class PMContainingNodeVisitor extends ASTVisitor {
		int _offset;
		int _length;
		
		
		ASTNode _containingNode = null;
		
		
		public PMContainingNodeVisitor(int offset, int length) {
			_offset = offset;
			_length = length;
		}
		
		
		public boolean visitContainingNode(ASTNode node) {
			if (node.getStartPosition() + 1 <= _offset && _offset+ _length <= node.getStartPosition() + node.getLength() - 1) {
				
								
				_containingNode = node;
				return true;
			} else
				return false;
		}
		
		public ASTNode getContainingNode() {
			return _containingNode;
		}
	}
	
	
	private static Block FindContainingBlockForSelection(ASTNode nodeToSearch, int offset, int length) {
		
		PMContainingBlockVisitor containingBlockVisitor = new PMContainingBlockVisitor(offset, length);
		
		nodeToSearch.accept(containingBlockVisitor);
		
		return (Block)containingBlockVisitor.getContainingNode();
	}
	
	static protected class PMContainingBlockVisitor extends PMContainingNodeVisitor {
		
		public PMContainingBlockVisitor(int offset, int length) {
			super(offset, length);
		}
		
		@Override
		public boolean visit(Block block) {
			return visitContainingNode(block);	
		}
	}
	
	
	private static TypeDeclaration FindContainingTypeDeclarationForSelection(ASTNode nodeToSearch, int offset, int length) {
		PMContainingTypeDeclarationVisitor containingTypeDeclarationVisitor = new PMContainingTypeDeclarationVisitor(offset, length);
		
		nodeToSearch.accept(containingTypeDeclarationVisitor);
		
		return (TypeDeclaration)containingTypeDeclarationVisitor.getContainingNode();
	}
	
	static protected class PMContainingTypeDeclarationVisitor extends PMContainingNodeVisitor {
		
		public PMContainingTypeDeclarationVisitor(int offset, int length) {
			super(offset, length);
		}
		
		@Override
		public boolean visit(TypeDeclaration typeDeclaration) {
			return visitContainingNode(typeDeclaration);	
		}
	}
}
