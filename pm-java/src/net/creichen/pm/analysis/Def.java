/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm.analysis;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.jdt.core.dom.*;

public class Def {

    private final ASTNode definingNode;

    private final Set<Use> uses;

    Def(final ASTNode definingNode) {
        this.definingNode = definingNode;

        this.uses = new HashSet<Use>();
    }

    void addUse(final Use use) {
        if (!this.uses.contains(use)) {
            this.uses.add(use);

            use.addReachingDefinition(this);
        }
    }

    private IBinding findBindingForLHS(final Expression lhs) {
        IBinding binding = null;

        if (lhs instanceof Name) {
            final Name assignmentName = (Name) lhs;
            binding = assignmentName.resolveBinding();
        } else if (lhs instanceof FieldAccess) {
            final FieldAccess fieldAccess = (FieldAccess) lhs;

            binding = fieldAccess.resolveFieldBinding();
        } else {
            throw new RuntimeException("Don't know how to find binding for "
                    + lhs.getClass().getCanonicalName() + " [" + lhs + "]");
        }

        return binding;
    }

    public IBinding getBinding() {
        IBinding result = null;

        if (this.definingNode instanceof Assignment) {
            final Assignment assignment = (Assignment) this.definingNode;

            result = findBindingForLHS(assignment.getLeftHandSide());
        } else if (this.definingNode instanceof SingleVariableDeclaration) {
            final SingleVariableDeclaration singleVariableDeclaration = (SingleVariableDeclaration) this.definingNode;

            result = findBindingForLHS(singleVariableDeclaration.getName());

        } else if (this.definingNode instanceof VariableDeclarationFragment) {
            final VariableDeclarationFragment variableDeclarationFragment = (VariableDeclarationFragment) this.definingNode;

            result = findBindingForLHS(variableDeclarationFragment.getName());
        } else if (this.definingNode instanceof PostfixExpression) {
            final PostfixExpression postfixExpression = (PostfixExpression) this.definingNode;

            result = findBindingForLHS(postfixExpression.getOperand());
        } else if (this.definingNode instanceof PrefixExpression) {
            final PrefixExpression prefixExpression = (PrefixExpression) this.definingNode;

            result = findBindingForLHS(prefixExpression.getOperand());
        } else {
            throw new RuntimeException("Un-handled _definingNode type "
                    + this.definingNode.getClass());
        }

        return result;
    }

    public ASTNode getDefiningNode() {
        return this.definingNode;
    }

    public Set<Use> getUses() {
        return this.uses;
    }

    // Not all declaring nodes are VariableDeclarations; they could be
    // FieldDeclarations
    // Should probably create a PMDeclaration type that combines these two.
    // For now, we just return VariableDeclaration, though

    /*
     * public VariableDeclaration getDeclaringNode() { VariableDeclaration result = null;
     * 
     * if (_definingNode instanceof Assignment) { Assignment assignment = (Assignment)_definingNode;
     * 
     * 
     * Expression lhs = assignment.getLeftHandSide();
     * 
     * if (lhs instanceof SimpleName) { SimpleName assignmentName = (SimpleName)lhs;
     * 
     * result = localDeclarationForSimpleName(assignmentName);
     * 
     * 
     * } } else if (_definingNode instanceof SingleVariableDeclaration) { SingleVariableDeclaration
     * singleVariableDeclaration = (SingleVariableDeclaration)_definingNode;
     * 
     * result = singleVariableDeclaration; //the declaring node for a SingleVariableDeclaration IS
     * that declaration } else if (_definingNode instanceof VariableDeclarationFragment) {
     * VariableDeclarationFragment variableDeclarationFragment =
     * (VariableDeclarationFragment)_definingNode;
     * 
     * result = variableDeclarationFragment; //the declaring node for a VariableDeclarationFragment
     * IS that declaration } else if (_definingNode instanceof PostfixExpression) {
     * PostfixExpression postfixExpression = (PostfixExpression)_definingNode;
     * 
     * Expression operand = postfixExpression.getOperand();
     * 
     * 
     * if (operand instanceof SimpleName) { result =
     * localDeclarationForSimpleName((SimpleName)operand);
     * 
     * 
     * } } else if (_definingNode instanceof PrefixExpression) { PrefixExpression prefixExpression =
     * (PrefixExpression)_definingNode;
     * 
     * Expression operand = prefixExpression.getOperand();
     * 
     * if (operand instanceof SimpleName) { result =
     * localDeclarationForSimpleName((SimpleName)operand);
     * 
     * } }else { throw new RuntimeException("Un-handled _definingNode type " +
     * _definingNode.getClass()); }
     * 
     * return result; }
     */

    @Override
    public String toString() {

        return "PMDef: " + this.definingNode + " [ " + this.uses.size() + " uses]";
    }
}
