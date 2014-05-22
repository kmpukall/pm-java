/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

*******************************************************************************/

package pm_refactoring.selection;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;

import org.eclipse.jdt.core.dom.ChildListPropertyDescriptor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;


public class PMSelection {

	CompilationUnit _compilationUnit;
	int _offset;
	int _length;
	
	
	ASTNode _singleSelectedNode; 
	
	ASTNode _propertyParentNode;
	StructuralPropertyDescriptor _selectedPropertyDescriptor;
	int _childListPropertyOffset;
	int _childListPropertyLength;
	
	
	public PMSelection(CompilationUnit compilationUnit, int offset, int length) {
		
		_compilationUnit = compilationUnit;
		
		_offset = offset;
		_length = length;
		
		_childListPropertyOffset = -1;
		_childListPropertyLength = -1;
		
		
		trimWhitespace();
		
		//ought to do this lazily
		findSelection(_offset, _length);
		
		
	}
	
	
	public ASTNode singleSelectedNode() {
		return _singleSelectedNode;
	}
	
	
	public ASTNode[] selectedNodes() {
		return new ASTNode[0];
	}
	
	public StructuralPropertyDescriptor selectedNodeParentProperty() {
		return _selectedPropertyDescriptor;
	}
	
	public ASTNode selectedNodeParent() {
		 if (_singleSelectedNode != null)
			 return _singleSelectedNode.getParent();
		 else return _propertyParentNode;
	}
	
	public int selectedNodeParentPropertyListOffset() {
		return _childListPropertyOffset;
	}
	
	public int selectedNodeParentPropertyListLength() {
		return _childListPropertyLength;
	}
	
	public boolean isListSelection() {
		return _selectedPropertyDescriptor instanceof ChildListPropertyDescriptor;
	}
	
	public boolean isMultipleSelection() {
		return _childListPropertyLength > 0;
	}
	
	public boolean isSaneSelection() {
		return _selectedPropertyDescriptor != null || _singleSelectedNode != null;
	}
	

	
	
	private void findSelection(int offset, int length) {
		PMExactSelectionVisitor selectionVisitor = new PMExactSelectionVisitor(offset, length);
		_compilationUnit.accept(selectionVisitor);
		
		ASTNode containingNode = selectionVisitor.getContainingNode();
		
		if (containingNode.getStartPosition() == offset && containingNode.getLength() == length)
			_singleSelectedNode = containingNode;
		else {
			_singleSelectedNode = null;
			
						
					
			// if there is no single selected node, find the insertion points corresponding to the start and
			// end of the selection and see if these insertion points contain a sequence of nodes
			
			PMInsertionPoint startInsertionPoint = new PMInsertionPoint(_compilationUnit, offset);
			
			PMInsertionPoint endInsertionPoint = new PMInsertionPoint(_compilationUnit, offset + length);
			
			if (startInsertionPoint.isSaneInsertionPoint() && endInsertionPoint.isSaneInsertionPoint()) {
								
				if (startInsertionPoint.insertionParent() == endInsertionPoint.insertionParent() &&
						startInsertionPoint.insertionProperty().equals(endInsertionPoint.insertionProperty())) {
										
					_propertyParentNode = startInsertionPoint.insertionParent();
					
					_selectedPropertyDescriptor = startInsertionPoint.insertionProperty();
					
					_childListPropertyOffset = startInsertionPoint.insertionIndex();
					
					_childListPropertyLength = endInsertionPoint.insertionIndex() - startInsertionPoint.insertionIndex();
				}
			}
		}
			
	}
	
	@SuppressWarnings ("restriction") protected static class PMExactSelectionVisitor extends org.eclipse.jdt.internal.corext.dom.GenericVisitor {
		
		int _offset;
		int _length;
		
		
		ASTNode _containingNode;
		
		public PMExactSelectionVisitor(int offset, int length) {
			_offset = offset;
			_length = length;
		}
		
		
		protected boolean nodeContainsSelection(ASTNode node, int offset, int length) {
			return offset >= node.getStartPosition() && offset + length <= node.getStartPosition() + node.getLength();
		}
		
		public boolean visitNode(ASTNode node) {
			//result from this method determines whether it will be called for nodes beneath it.
			
		
			if (nodeContainsSelection(node, _offset, _length)) {
				
				_containingNode = node;
				
				return true;
			} else {
				
				return false;
			}
				
			
					
		}
		
		public ASTNode getContainingNode() {
			return _containingNode;
		}
	}
	
	public String getSelectionAsString() {
		
		String result = null;
		
		String entireSource = null;
		
		try {
			ICompilationUnit iCompilationUnit = ((ICompilationUnit)_compilationUnit.getJavaElement());
			
			//Sometimes CompilationUnits don't have an associated ICompilationUnit (e.g if they were parsed from a string)
			//If not, just us the (for debugging purposes only) _compilation.toString()
			
			if (iCompilationUnit != null)
				entireSource = ((ICompilationUnit)_compilationUnit.getJavaElement()).getSource();
			else
				entireSource = _compilationUnit.toString();
			
		} catch (JavaModelException e) {
			System.err.println("Exception in PMSelection.getSelectionAsString(): " + e);
			
			throw new RuntimeException(e);
		}
		
		result = entireSource.substring(_offset, _offset + _length);
		
		return result;
	}
	
	private void trimWhitespace() {
		//move the selection so that it contains no leading or trailing whitespace
		
		String selection = getSelectionAsString();
		
		//trim whitespace at beginning
		for (int index = 0; index < selection.length(); index++) {
			if (Character.isWhitespace(selection.charAt(index))) {
				_offset++;
				_length--;
			} else
				break;
		}

		//trim whitespace at end
		for (int index = selection.length() - 1; index >= 0; index--) {
			if (Character.isWhitespace(selection.charAt(index))) {
				_length--;
			} else
				break;
		}
		
		
	}
	
	
	
	
	
}
