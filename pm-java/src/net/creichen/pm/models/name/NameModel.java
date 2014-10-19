/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm.models.name;

import static net.creichen.pm.utils.ASTQuery.getConstructors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.creichen.pm.api.ASTRootsProvider;
import net.creichen.pm.utils.visitors.IdentifierAssigner;
import net.creichen.pm.utils.visitors.collectors.SelectiveSimpleNameCollector;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

public class NameModel {
    private Map<Name, String> identifiers;
    private Multimap<String, Name> names = HashMultimap.create();

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

    public String getIdentifier(final Name name) {
        return this.identifiers.get(name);
    }

    public List<SimpleName> nameNodesRelatedToNameNode(final SimpleName name) {
        final Set<SimpleName> allRelatedNodes = new HashSet<SimpleName>();
        recursiveAddNameNodesRelatedToNameNode(name, allRelatedNodes);
        return new ArrayList<SimpleName>(allRelatedNodes);
    }

    public void removeIdentifier(final Name name) {
        this.identifiers.remove(name);
    }

    public void rename(final Name oldName, final Name newName) {
        if (this.identifiers.containsKey(oldName)) {
            String identifier = getIdentifier(oldName);
            setIdentifier(identifier, newName);
            removeIdentifier(oldName);
        }
    }

    public String setIdentifier(final String identifier, final Name name) {
        return this.identifiers.put(name, identifier);
    }

    private void recursiveAddNameNodesRelatedToNameNode(final SimpleName name, final Set<SimpleName> visitedNodes) {
        final String identifier = this.identifiers.get(name);
        final List<SimpleName> directlyRelatedNodes = findNodesWithIdentifier(identifier);
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

    private List<SimpleName> findNodesWithIdentifier(final String identifier) {
        SelectiveSimpleNameCollector visitor = new SelectiveSimpleNameCollector(identifier, this.identifiers);

        // We could keep reverse mappings instead of doing this?
        for (final ASTNode rootNode : this.rootsProvider.getASTRoots()) {
            rootNode.accept(visitor);
        }

        return visitor.getResults();
    }

    private Set<SimpleName> representativeNameNodesIndirectlyRelatedToNameNode(final SimpleName nameNode) {
        final ASTNode parent = nameNode.getParent();
        if (parent instanceof TypeDeclaration
                && nameNode.getLocationInParent() == ((TypeDeclaration) parent).getNameProperty()) {
            TypeDeclaration type = (TypeDeclaration) parent;
            return getRepresentativeNodes(type);
        } else if (parent instanceof MethodDeclaration) {
            MethodDeclaration method = (MethodDeclaration) parent;
            return getRepresentativeNodes(method);
        }

        return Collections.emptySet();
    }

    private Set<SimpleName> getRepresentativeNodes(final TypeDeclaration type) {
        final Set<SimpleName> result = new HashSet<SimpleName>();
        for (final MethodDeclaration constructor : getConstructors(type)) {
            result.add(constructor.getName());
        }
        return result;
    }

    private Set<SimpleName> getRepresentativeNodes(final MethodDeclaration method) {
        final Set<SimpleName> result = new HashSet<SimpleName>();
        if (method.isConstructor()) {
            // If the name is the name of a constructor, we have to add all
            // of the other constructors
            // (and the names related to them) and the
            // name of the class
            final TypeDeclaration type = (TypeDeclaration) method.getParent();
            result.add(type.getName());
            for (final MethodDeclaration constructor : getConstructors(type)) {
                result.add(constructor.getName());
            }
            // the method name itself should not be among the representative nodes
            result.remove(method.getName());
        }
        return result;
    }

}
