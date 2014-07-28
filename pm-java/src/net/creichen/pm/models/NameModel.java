/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm.models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;
import org.eclipse.jdt.core.dom.TypeDeclaration;

public class NameModel {
    private Map<Name, String> identifiersForNames;

    private final Project project;

    public NameModel(final Project project) {
        this.project = project;

        this.identifiersForNames = new HashMap<Name, String>();

        assignInitialIdentifiers();
    }

    private void assignInitialIdentifiers() {
        // use ast visitor to assign identifier to each SimpleName

        final Map<IBinding, String> identifiersForBindings = new HashMap<IBinding, String>();

        this.identifiersForNames = new HashMap<Name, String>();

        for (final ASTNode rootNode : this.project.getASTRoots()) {
            rootNode.accept(new ASTVisitor() {

                // We should visit more than simle names here
                // We also care about field accesses, right?
                // method invocations, etc.??

                @Override
                public boolean visit(final SimpleName simpleName) {
                    IBinding binding = simpleName.resolveBinding();

                    if (binding instanceof IVariableBinding) {

                        binding = ((IVariableBinding) binding).getVariableDeclaration();

                        // to deal with generics; different instances of
                        // generics have different bindings
                        // we can cal getVariableDeclaration() to get a common
                        // binding representing the non-instantiated binding
                        //
                        // This may not be the right thing to do, but seems to
                        // be necessary for inside the definitions of generic
                        // classes;
                        // i.e.
                        /*
                         * class Foo<T> { T _ivar;
                         * 
                         * T getIvar() { return _ivar; } }
                         * 
                         * If we don't use the getVariableDeclaration, we get separate bindings for the first and second
                         * use of _ivar, which is clearly not what we want.
                         * 
                         * But, this may lose information that we want in other cases
                         */

                    }

                    if (binding instanceof ITypeBinding) {
                        binding = ((ITypeBinding) binding).getTypeDeclaration();
                    }

                    if (binding instanceof IMethodBinding) {
                        binding = ((IMethodBinding) binding).getMethodDeclaration();
                    }

                    // System.err.println("Got binding " + binding.hashCode() +
                    // " for " + simpleName);
                    String identifier = identifiersForBindings.get(binding);

                    if (identifier == null) {
                        identifier = generateNewIdentifierForName(simpleName);
                        identifiersForBindings.put(binding, identifier);
                    }

                    NameModel.this.identifiersForNames.put(simpleName, identifier);

                    return true;
                }
            });

        }
    }

    private List<MethodDeclaration> constructorsForClass(final TypeDeclaration classDeclaration) {
        final List<MethodDeclaration> constructors = new ArrayList<MethodDeclaration>();

        for (final MethodDeclaration method : classDeclaration.getMethods()) {
            if (method.isConstructor()) {
                constructors.add(method);
            }
        }

        return constructors;
    }

    public String generateNewIdentifierForName(final Name name) {
        return UUID.randomUUID().toString();
    }

    public String identifierForName(final Name name) {
        return this.identifiersForNames.get(name);
    }

    public List<SimpleName> nameNodesRelatedToNameNode(final SimpleName name) {
        final Set<SimpleName> allRelatedNodes = new HashSet<SimpleName>();

        recursiveAddNameNodesRelatedToNameNode(name, allRelatedNodes);

        return new ArrayList<SimpleName>(allRelatedNodes);
    }

    private List<SimpleName> nameNodesRelatedToNameNodeWithIdentifier(final String identifier) {
        final String identifierCopy = identifier;

        final List<SimpleName> result = new ArrayList<SimpleName>();

        // We could keep reverse mappings instead of doing this?

        for (final ASTNode rootNode : this.project.getASTRoots()) {
            rootNode.accept(new ASTVisitor() {
                @Override
                public boolean visit(final SimpleName visitedNode) {
                    final String identifierForVisitedNode = NameModel.this.identifiersForNames.get(visitedNode);

                    if (identifierCopy.equals(identifierForVisitedNode)) {
                        result.add(visitedNode);
                    }

                    return true;
                }
            });

        }

        return result;
    }

    private void recursiveAddNameNodesRelatedToNameNode(final SimpleName name, final Set<SimpleName> visitedNodes) {

        final String identifier = this.identifiersForNames.get(name);

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

    public void removeIdentifierForName(final Name name) {
        this.identifiersForNames.remove(name);
    }

    public void replaceNameWithName(final Name oldName, final Name newName) {
        final String identifier = identifierForName(oldName);

        if (identifier != null) {
            removeIdentifierForName(oldName);
            setIdentifierForName(identifier, newName);
        }

    }

    private Set<SimpleName> representativeNameNodesIndirectlyRelatedToNameNode(final SimpleName nameNode) {
        final Set<SimpleName> result = new HashSet<SimpleName>();

        final ASTNode parent = nameNode.getParent();
        final StructuralPropertyDescriptor locationInParent = nameNode.getLocationInParent();

        if (parent instanceof TypeDeclaration && locationInParent == ((TypeDeclaration) parent).getNameProperty()) {
            final List<MethodDeclaration> constructors = constructorsForClass((TypeDeclaration) nameNode.getParent());

            for (final MethodDeclaration constructor : constructors) {
                result.add(constructor.getName());
            }
        } else if (parent instanceof MethodDeclaration) {
            final MethodDeclaration method = (MethodDeclaration) nameNode.getParent();

            if (method.isConstructor()) {
                // If the name is the name of a constructor, we have to add all
                // of the other constructors
                // (and the names related to them) and the
                // name of the class and all names related to that class

                final TypeDeclaration containingClass = (TypeDeclaration) method.getParent();

                result.add(containingClass.getName());

                final List<MethodDeclaration> constructors = constructorsForClass(containingClass);

                for (final MethodDeclaration constructor : constructors) {
                    if (constructor != method) {
                        result.add(constructor.getName());
                    }
                }

            }
        }

        return result;
    }

    public String setIdentifierForName(final String identifier, final Name name) {
        return this.identifiersForNames.put(name, identifier);
    }

}
