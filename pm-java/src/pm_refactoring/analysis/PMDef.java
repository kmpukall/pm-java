/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

*******************************************************************************/

package pm_refactoring.analysis;



import java.util.HashSet;

import java.util.Set;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;



public class PMDef {

		protected ASTNode _definingNode;
		
		protected Set<PMUse> _uses;
		
		public PMDef(ASTNode definingNode) {
			_definingNode = definingNode;
			
			_uses = new HashSet<PMUse>();
		}
		
		
		public String toString() {
			
			return "PMDef: " + _definingNode + " [ " + _uses.size() + " uses]";
		}
		
		public void addUse(PMUse use) {
			if (!_uses.contains(use)) {
				_uses.add(use);
				
				use.addReachingDefinition(this);
			}
		}
		public Set<PMUse> getUses() {
			return _uses;
		}
		
		
		public ASTNode getDefiningNode() {
			return _definingNode;
		}
		
		
		public IBinding findBindingForLHS(Expression lhs) {
			IBinding binding = null;
			
			if (lhs instanceof Name) {
				Name assignmentName = (Name)lhs;	
				binding = assignmentName.resolveBinding();
			} else if (lhs instanceof FieldAccess) {
				FieldAccess fieldAccess = (FieldAccess)lhs;
				
				binding = fieldAccess.resolveFieldBinding();
			} else {
				throw new RuntimeException("Don't know how to find binding for " + lhs.getClass().getCanonicalName() + " [" + lhs + "]");
			}
		
			return binding;
		}
		
		public IBinding getBinding() {
			IBinding result = null;
			
			if (_definingNode instanceof Assignment) {
				Assignment assignment = (Assignment)_definingNode;
				
				
				result = findBindingForLHS(assignment.getLeftHandSide());
			} else if (_definingNode instanceof SingleVariableDeclaration) {
				SingleVariableDeclaration singleVariableDeclaration = (SingleVariableDeclaration)_definingNode;
				
				result = findBindingForLHS(singleVariableDeclaration.getName());
				
			} else if (_definingNode instanceof VariableDeclarationFragment) {
				VariableDeclarationFragment variableDeclarationFragment = (VariableDeclarationFragment)_definingNode;
				
				result = findBindingForLHS(variableDeclarationFragment.getName());
			} else if (_definingNode instanceof PostfixExpression) {
				PostfixExpression postfixExpression = (PostfixExpression)_definingNode;
				
				result = findBindingForLHS(postfixExpression.getOperand());
			} else if (_definingNode instanceof PrefixExpression) {
				PrefixExpression prefixExpression = (PrefixExpression)_definingNode;
				
				result = findBindingForLHS(prefixExpression.getOperand());
			} else {
				throw new RuntimeException("Un-handled _definingNode type " + _definingNode.getClass());				
			}
			
			return result;
		}
		
		//Not all declaring nodes are VariableDeclarations; they could be FieldDeclarations
		//Should probably create a PMDeclaration type that combines these two.
		//For now, we just return VariableDeclaration, though
		
		/*public VariableDeclaration getDeclaringNode() {
			VariableDeclaration result = null;
			
			if (_definingNode instanceof Assignment) {
				Assignment assignment = (Assignment)_definingNode;
				
				
				Expression lhs = assignment.getLeftHandSide();
				
				if (lhs instanceof SimpleName) {
					SimpleName assignmentName = (SimpleName)lhs;
					
					result = localDeclarationForSimpleName(assignmentName);
					
					
					}
			} else if (_definingNode instanceof SingleVariableDeclaration) {
				SingleVariableDeclaration singleVariableDeclaration = (SingleVariableDeclaration)_definingNode;
				
				result = singleVariableDeclaration; //the declaring node for a SingleVariableDeclaration IS that declaration	
			} else if (_definingNode instanceof VariableDeclarationFragment) {
				VariableDeclarationFragment variableDeclarationFragment = (VariableDeclarationFragment)_definingNode;
				
				result = variableDeclarationFragment; //the declaring node for a VariableDeclarationFragment IS that declaration	
			} else if (_definingNode instanceof PostfixExpression) {
				PostfixExpression postfixExpression = (PostfixExpression)_definingNode;
				
				Expression operand = postfixExpression.getOperand();
				
				
				if (operand instanceof SimpleName) {
					result = localDeclarationForSimpleName((SimpleName)operand);
					
					
				}
			} else if (_definingNode instanceof PrefixExpression) {
				PrefixExpression prefixExpression = (PrefixExpression)_definingNode;
				
				Expression operand = prefixExpression.getOperand();
				
				if (operand instanceof SimpleName) {
					result = localDeclarationForSimpleName((SimpleName)operand);
					
				}
			}else {
				throw new RuntimeException("Un-handled _definingNode type " + _definingNode.getClass());				
			}
			
			return result;
		}
		*/
		
		
		public static VariableDeclaration localDeclarationForSimpleName(SimpleName simpleName) {	
			return (VariableDeclaration)((CompilationUnit)simpleName.getRoot()).findDeclaringNode(simpleName.resolveBinding());
		}
}
