/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm;

import static net.creichen.pm.utils.APIWrapperUtil.getStructuralProperty;

import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.ChildListPropertyDescriptor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

public class PMASTNodeUtils {
    public static VariableDeclaration localVariableDeclarationForSimpleName(SimpleName name) {
        return (VariableDeclaration) ((CompilationUnit) name.getRoot()).findDeclaringNode(name
                .resolveBinding());
    }

    // We also consider parameters, for statement vars, and catch vars to be
    // local
    public static boolean variableDeclarationIsLocal(VariableDeclaration declaration) {

        ASTNode parent = declaration.getParent();

        // not sure this is actually the best way to do this

        if (parent instanceof CatchClause) {
            return true;
        }

        if (parent instanceof VariableDeclarationExpression) {
            return true;
        }

        if (parent instanceof VariableDeclarationStatement) {
            return true;
        }

        if (parent instanceof ForStatement) {
            return true;
        }

        return false;
    }

    public static void replaceNodeInParent(ASTNode oldNode, ASTNode replacement) {
        StructuralPropertyDescriptor location = oldNode.getLocationInParent();

        // replace the selected method invocation with the new invocation
        if (location.isChildProperty()) {
            oldNode.getParent().setStructuralProperty(location, replacement);
        } else {
            List<ASTNode> parentList = getStructuralProperty(
                    (ChildListPropertyDescriptor) location, oldNode.getParent());

            parentList.set(parentList.indexOf(oldNode), replacement);
        }
    }
}
