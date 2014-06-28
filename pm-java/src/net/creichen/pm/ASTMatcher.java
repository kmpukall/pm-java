/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.creichen.pm.utils.APIWrapperUtil;

import org.eclipse.jdt.core.dom.*;

public class ASTMatcher {

    private final ASTNode oldNode;

    private final ASTNode newNode;

    private final Map<ASTNode, ASTNode> isomorphicNodes;

    public ASTMatcher(final ASTNode oldNode, final ASTNode newNode) {
        this.isomorphicNodes = new HashMap<ASTNode, ASTNode>();

        this.newNode = newNode;
        this.oldNode = oldNode;
    }

    public Map<ASTNode, ASTNode> isomorphicNodes() {
        return this.isomorphicNodes;
    }

    public boolean match() {

        final boolean result = recursiveMatch(this.oldNode, this.newNode);

        if (!result) {
            this.isomorphicNodes.clear();
        }

        return result;
    }

    private boolean recursiveMatch(final ASTNode oldNode, final ASTNode newNode) {

        // eventually we'll get smarter here and detect new nodes, etc.
        final int oldType = oldNode.getNodeType();
        final int newType = newNode.getNodeType();

        if ((oldType == org.eclipse.jdt.core.dom.ASTNode.BLOCK_COMMENT
                || oldType == org.eclipse.jdt.core.dom.ASTNode.LINE_COMMENT || oldType == org.eclipse.jdt.core.dom.ASTNode.JAVADOC)
                && (newType == org.eclipse.jdt.core.dom.ASTNode.BLOCK_COMMENT
                        || newType == org.eclipse.jdt.core.dom.ASTNode.LINE_COMMENT || newType == org.eclipse.jdt.core.dom.ASTNode.JAVADOC)) {
            return true; // Nothing to do for comments, really
        }

        if (oldNode instanceof MethodDeclaration && newNode instanceof MethodDeclaration) {

            final MethodDeclaration oldMethodDeclaration = (MethodDeclaration) oldNode;
            final MethodDeclaration newMethodDeclaration = (MethodDeclaration) newNode;

            // fixup method declarations
            // if the old declaration is a constructor and the new one is a
            // method with the same name then
            // we set the new one to be a constructor too.
            // We do this because if we rename a class or constructor, the old
            // ast will think the constructor
            // is a constructor while the new one will think it is just a method
            // with a missing return type.

            if (oldMethodDeclaration.isConstructor()) {
                newMethodDeclaration.setConstructor(true);
            }
        }

        final List<StructuralPropertyDescriptor> oldStructuralProperties = APIWrapperUtil
                .structuralPropertiesForType(oldNode);

        if (oldStructuralProperties.size() == APIWrapperUtil.structuralPropertiesForType(newNode)
                .size()) {
            for (final StructuralPropertyDescriptor structuralPropertyDescriptor : oldStructuralProperties) {

                final Object oldPropertyValue = oldNode
                        .getStructuralProperty(structuralPropertyDescriptor);

                final Object newPropertyValue = newNode
                        .getStructuralProperty(structuralPropertyDescriptor);

                if ((oldPropertyValue == null && newPropertyValue != null)
                        || (newPropertyValue != null && newPropertyValue == null)) {
                    return false;
                }

                if (oldPropertyValue == null && newPropertyValue == null) {
                    continue;
                } else {
                    // property values are ptr different and not null

                    if (structuralPropertyDescriptor.isSimpleProperty()) {
                        if (oldPropertyValue.equals(newPropertyValue)) {
                            continue;
                        } else {
                            return false;
                        }
                    } else if (structuralPropertyDescriptor.isChildProperty()) {
                        final ASTNode oldChild = APIWrapperUtil.getStructuralProperty(
                                (ChildPropertyDescriptor) structuralPropertyDescriptor, oldNode);
                        final ASTNode newChild = APIWrapperUtil.getStructuralProperty(
                                (ChildPropertyDescriptor) structuralPropertyDescriptor, newNode);

                        if (!recursiveMatch(oldChild, newChild)) {
                            return false;
                        } else {
                            continue;
                        }

                    } else if (structuralPropertyDescriptor.isChildListProperty()) {

                        final List<ASTNode> oldList = APIWrapperUtil
                                .getStructuralProperty(
                                        (ChildListPropertyDescriptor) structuralPropertyDescriptor,
                                        oldNode);
                        final List<ASTNode> newList = APIWrapperUtil
                                .getStructuralProperty(
                                        (ChildListPropertyDescriptor) structuralPropertyDescriptor,
                                        newNode);

                        if (oldList.size() == newList.size()) {

                            for (int i = 0; i < oldList.size(); i++) {
                                final ASTNode oldChildNode = oldList.get(i);

                                final ASTNode newChildNode = newList.get(i);

                                if (recursiveMatch(oldChildNode, newChildNode)) {
                                    continue;
                                } else {
                                    return false;
                                }
                            }

                        } else {
                            return false;
                        }

                    } else {

                        throw new RuntimeException("Unknown kind of structuralPropertyDescriptor");
                    }
                }

            }
        } else {
            return false;
        }

        // if we've gotten this far then all the structural properties match, so
        // we
        // add to the isomorphism map

        this.isomorphicNodes.put(oldNode, newNode);

        return true;
    }

}
