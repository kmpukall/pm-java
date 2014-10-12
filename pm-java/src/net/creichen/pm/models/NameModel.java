/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm.models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.creichen.pm.api.ASTRootsProvider;
import net.creichen.pm.utils.ASTQuery;
import net.creichen.pm.utils.visitors.IdentifierAssigner;
import net.creichen.pm.utils.visitors.SelectiveSimpleNameCollector;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.TypeDeclaration;

public class NameModel {
    private Map<Name, String> identifiers;

    private final ASTRootsProvider rootsProvider;

    public NameModel(final ASTRootsProvider rootsProvider) {
        this.rootsProvider = rootsProvider;

        // use ast visitor to assign identifier to each SimpleName
        IdentifierAssigner visitor = new IdentifierAssigner();
        for (final ASTNode rootNode : this.rootsProvider.getASTRoots()) {
            rootNode.accept(visitor);
        }
        this.identifiers = visitor.getIdentifiers();
    }

    /**
     *
     * @return
     */
    public static String generateNewIdentifier() {
        return UUID.randomUUID().toString();
    }

    public String getIdentifier(final Name name) {
        return this.identifiers.get(name);
    }

    /**
     *
     * @param name
     * @return
     */
    public List<SimpleName> nameNodesRelatedToNameNode(final SimpleName name) {
        final Set<SimpleName> allRelatedNodes = new HashSet<SimpleName>();
        recursiveAddNameNodesRelatedToNameNode(name, allRelatedNodes);
        return new ArrayList<SimpleName>(allRelatedNodes);
    }

    /**
     *
     * @param name
     */
    public void removeIdentifierForName(final Name name) {
        this.identifiers.remove(name);
    }

    /**
     *
     * @param oldName
     * @param newName
     */
    public void replaceName(final Name oldName, final Name newName) {
        if (this.identifiers.containsKey(oldName)) {
            setIdentifierForName(getIdentifier(oldName), newName);
            removeIdentifierForName(oldName);
        }

    }

    /**
     *
     * @param identifier
     * @param name
     * @return
     */
    public String setIdentifierForName(final String identifier, final Name name) {
        return this.identifiers.put(name, identifier);
    }

    private List<SimpleName> nameNodesRelatedToNameNodeWithIdentifier(final String identifier) {
        SelectiveSimpleNameCollector visitor = new SelectiveSimpleNameCollector(identifier, this.identifiers);

        // We could keep reverse mappings instead of doing this?
        for (final ASTNode rootNode : this.rootsProvider.getASTRoots()) {
            rootNode.accept(visitor);
        }

        return visitor.getResults();
    }

    private void recursiveAddNameNodesRelatedToNameNode(final SimpleName name, final Set<SimpleName> visitedNodes) {
        final String identifier = this.identifiers.get(name);

        final List<SimpleName> directlyRelatedNodes = nameNodesRelatedToNameNodeWithIdentifier(identifier);
        for (final SimpleName directlyRelatedName : directlyRelatedNodes) {
            if (!visitedNodes.contains(directlyRelatedName)) {
                visitedNodes.add(directlyRelatedName);
                final Set<SimpleName> indirectlyRelatedNames = representativeNameNodesIndirectlyRelatedToNameNode(directlyRelatedName);
                for (final SimpleName indirectlyRelatedName : indirectlyRelatedNames) {
                    recursiveAddNameNodesRelatedToNameNode(indirectlyRelatedName, visitedNodes);
                }
            }
        }

    }

    private Set<SimpleName> representativeNameNodesIndirectlyRelatedToNameNode(final SimpleName nameNode) {
        final ASTNode parent = nameNode.getParent();
        if (parent instanceof TypeDeclaration
                && nameNode.getLocationInParent() == ((TypeDeclaration) parent).getNameProperty()) {
            return getRepresentativeNodesForType((TypeDeclaration) parent);
        } else if (parent instanceof MethodDeclaration) {
            return getRepresentativeNodesForMethod((MethodDeclaration) parent);
        }

        return Collections.emptySet();
    }

    private Set<SimpleName> getRepresentativeNodesForMethod(final MethodDeclaration method) {
        final Set<SimpleName> result = new HashSet<SimpleName>();
        if (method.isConstructor()) {
            // If the name is the name of a constructor, we have to add all
            // of the other constructors
            // (and the names related to them) and the
            // name of the class and all names related to that class
            final TypeDeclaration containingClass = (TypeDeclaration) method.getParent();
            result.add(containingClass.getName());

            for (final MethodDeclaration constructor : ASTQuery.getConstructors(containingClass)) {
                if (constructor != method) {
                    result.add(constructor.getName());
                }
            }
        }
        return result;
    }

    private Set<SimpleName> getRepresentativeNodesForType(final TypeDeclaration parent) {
        final Set<SimpleName> result = new HashSet<SimpleName>();
        final List<MethodDeclaration> constructors = ASTQuery.getConstructors(parent);

        for (final MethodDeclaration constructor : constructors) {
            result.add(constructor.getName());
        }
        return result;
    }

}
