/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm.utils;

import static net.creichen.pm.utils.APIWrapperUtil.getStructuralProperty;

import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.ChildListPropertyDescriptor;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

public final class ASTUtil {

    public static void replaceNodeInParent(final ASTNode oldNode, final ASTNode replacement) {
        final StructuralPropertyDescriptor location = oldNode.getLocationInParent();

        // replace the selected method invocation with the new invocation
        if (location.isChildProperty()) {
            oldNode.getParent().setStructuralProperty(location, replacement);
        } else {
            final List<ASTNode> parentList = getStructuralProperty((ChildListPropertyDescriptor) location,
                    oldNode.getParent());

            parentList.set(parentList.indexOf(oldNode), replacement);
        }
    }

    // We also consider parameters, for statement vars, and catch vars to be
    // local
    public static boolean isVariableDeclarationLocal(final VariableDeclaration declaration) {
        final ASTNode parent = declaration.getParent();

        // not sure this is actually the best way to do this
        return (parent instanceof CatchClause || parent instanceof VariableDeclarationExpression
                || parent instanceof VariableDeclarationStatement || parent instanceof ForStatement);

    }

    private ASTUtil() {
        // private utility class constructor
    }
}
