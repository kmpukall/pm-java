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
import java.util.Set;
import java.util.UUID;

import net.creichen.pm.PMCompilationUnit;
import net.creichen.pm.PMProject;
import net.creichen.pm.inconsistencies.PMInconsistency;
import net.creichen.pm.inconsistencies.PMNameCapture;
import net.creichen.pm.inconsistencies.PMNameConflict;
import net.creichen.pm.inconsistencies.PMUnknownName;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;
import org.eclipse.jdt.core.dom.TypeDeclaration;

public class PMNameModel {

    HashMap<Name, String> _identifiersForNames;

    PMProject _project;

    public PMNameModel(PMProject project) {
        _project = project;

        _identifiersForNames = new HashMap<Name, String>();

        assignInitialIdentifiers();
    }

    public String generateNewIdentifierForName(Name name) {
        return UUID.randomUUID().toString();
    }

    private void assignInitialIdentifiers() {
        // use ast visitor to assign identifier to each SimpleName

        final HashMap<IBinding, String> identifiersForBindings = new HashMap<IBinding, String>();

        _identifiersForNames = new HashMap<Name, String>();

        for (ASTNode rootNode : _project.getASTRoots()) {
            rootNode.accept(new ASTVisitor() {

                // We should visit more than simle names here
                // We also care about field accesses, right?
                // method invocations, etc.??

                public boolean visit(SimpleName simpleName) {
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
                         * If we don't use the getVariableDeclaration, we get separate bindings for
                         * the first and second use of _ivar, which is clearly not what we want.
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

                    _identifiersForNames.put(simpleName, identifier);

                    return true;
                }
            });

        }
    }

    protected ArrayList<SimpleName> nameNodesRelatedToNameNodeWithIdentifier(String identifier) {
        final String identifierCopy = identifier;

        final ArrayList<SimpleName> result = new ArrayList<SimpleName>();

        // We could keep reverse mappings instead of doing this?

        for (ASTNode rootNode : _project.getASTRoots()) {
            rootNode.accept(new ASTVisitor() {
                public boolean visit(SimpleName visitedNode) {
                    String identifierForVisitedNode = _identifiersForNames.get(visitedNode);

                    if (identifierCopy.equals(identifierForVisitedNode))
                        result.add(visitedNode);

                    return true;
                }
            });

        }

        return result;
    }

    protected List<MethodDeclaration> constructorsForClass(TypeDeclaration classDeclaration) {
        List<MethodDeclaration> constructors = new ArrayList<MethodDeclaration>();

        for (MethodDeclaration method : classDeclaration.getMethods()) {
            if (method.isConstructor())
                constructors.add(method);
        }

        return constructors;
    }

    protected Set<SimpleName> representativeNameNodesIndirectlyRelatedToNameNode(SimpleName nameNode) {
        Set<SimpleName> result = new HashSet<SimpleName>();

        ASTNode parent = nameNode.getParent();
        StructuralPropertyDescriptor locationInParent = nameNode.getLocationInParent();

        if (parent instanceof TypeDeclaration
                && locationInParent == ((TypeDeclaration) parent).getNameProperty()) {
            List<MethodDeclaration> constructors = constructorsForClass((TypeDeclaration) nameNode
                    .getParent());

            for (MethodDeclaration constructor : constructors) {
                result.add(constructor.getName());
            }
        } else if (parent instanceof MethodDeclaration) {
            MethodDeclaration method = (MethodDeclaration) nameNode.getParent();

            if (method.isConstructor()) {
                // If the name is the name of a constructor, we have to add all
                // of the other constructors
                // (and the names related to them) and the
                // name of the class and all names related to that class

                TypeDeclaration containingClass = (TypeDeclaration) method.getParent();

                result.add(containingClass.getName());

                List<MethodDeclaration> constructors = constructorsForClass(containingClass);

                for (MethodDeclaration constructor : constructors) {
                    if (constructor != method) {
                        result.add(constructor.getName());
                    }
                }

            }
        }

        return result;
    }

    protected void recursiveAddNameNodesRelatedToNameNode(SimpleName name,
            Set<SimpleName> visitedNodes) {

        String identifier = _identifiersForNames.get(name);

        ArrayList<SimpleName> directlyRelatedNodes = nameNodesRelatedToNameNodeWithIdentifier(identifier);

        for (SimpleName directlyRelatedName : directlyRelatedNodes) {
            if (!visitedNodes.contains(directlyRelatedName)) {
                visitedNodes.add(directlyRelatedName);

                Set<SimpleName> indirectlyRelatedNames = representativeNameNodesIndirectlyRelatedToNameNode(directlyRelatedName);

                for (SimpleName indirectlyRelatedName : indirectlyRelatedNames) {
                    recursiveAddNameNodesRelatedToNameNode(indirectlyRelatedName, visitedNodes);
                }
            }
        }

    }

    public ArrayList<SimpleName> nameNodesRelatedToNameNode(SimpleName name) {
        Set<SimpleName> allRelatedNodes = new HashSet<SimpleName>();

        recursiveAddNameNodesRelatedToNameNode(name, allRelatedNodes);

        return new ArrayList<SimpleName>(allRelatedNodes);
    }

    public void replaceNameWithName(Name oldName, Name newName) {
        String identifier = identifierForName(oldName);

        if (identifier != null) {
            removeIdentifierForName(oldName);
            setIdentifierForName(identifier, newName);
        }

    }

    public String identifierForName(Name name) {
        return _identifiersForNames.get(name);
    }

    public String setIdentifierForName(String identifier, Name name) {
        return _identifiersForNames.put(name, identifier);
    }

    public void removeIdentifierForName(Name name) {
        _identifiersForNames.remove(name);
    }

    public void removeIdentifiersForTreeStartingAtNode(ASTNode rootNode) {
        rootNode.accept(new ASTVisitor() {
            public boolean visit(SimpleName simpleName) {
                removeIdentifierForName(simpleName);

                return true;
            }
        });

    }

    public Set<PMInconsistency> calculateInconsistencies() {
        Set<PMInconsistency> inconsistencies = new HashSet<PMInconsistency>();

        for (PMCompilationUnit pmCompilationUnit : _project.getPMCompilationUnits()) {

            inconsistencies.addAll(nameInconsistenciesForICompilationUnit(pmCompilationUnit));
        }

        return inconsistencies;
    }

    private Set<PMInconsistency> nameInconsistenciesForICompilationUnit(
            PMCompilationUnit pmCompilationUnit) {

        Set<PMInconsistency> inconsistencies = new HashSet<PMInconsistency>();

        CompilationUnit compilationUnit = pmCompilationUnit.getASTNode();

        Set<SimpleName> simpleNamesInCompilationUnit = simpleNamesInCompilationUnit(compilationUnit);

        for (SimpleName simpleName : simpleNamesInCompilationUnit) {

            ASTNode declaringNode = _project.findDeclaringNodeForName(simpleName);// declaringModel.getCompilationUnit().findDeclaringNode(simpleName.resolveBinding());

            if (declaringNode != null) {
                SimpleName declaringSimpleName = _project.simpleNameForDeclaringNode(declaringNode);

                String declaringIdentifier = identifierForName(declaringSimpleName);

                String usingIdentifier = identifierForName(simpleName);

                if (usingIdentifier == null) {
                    inconsistencies.add(new PMUnknownName(_project, pmCompilationUnit, simpleName));
                } else {
                    if (declaringIdentifier != usingIdentifier
                            || !declaringIdentifier.equals(usingIdentifier)) {

                        // System.err.println("Capture of " + simpleName +
                        // " by " +
                        // ((CompilationUnit)declaringSimpleName.getRoot()).getJavaElement());

                        // System.err.println("Declaring identifier is " +
                        // declaringIdentifier + " usingIdentifier is " +
                        // usingIdentifier);

                        // System.err.println("Declaring simpleName is " +
                        // declaringSimpleName);
                        // System.err.println("Using binding is " +
                        // simpleName.resolveBinding().hashCode() + " of class "
                        // + simpleName.resolveBinding().getClass());
                        // System.err.println("Declaring binding is " +
                        // declaringSimpleName.resolveBinding().hashCode());

                        // System.err.println("Using binding.getVariableDeclaration is "
                        // +
                        // ((IVariableBinding)simpleName.resolveBinding()).getVariableDeclaration().hashCode()
                        // + " of class " +
                        // simpleName.resolveBinding().getClass());
                        // System.err.println("Declaring binding.getVariableDeclaration is "
                        // +
                        // ((IVariableBinding)declaringSimpleName.resolveBinding()).getVariableDeclaration().hashCode());

                        // don't have quick way to figure out what the declaring
                        // node should have been yet

                        inconsistencies.add(new PMNameCapture(_project, pmCompilationUnit,
                                simpleName, null, declaringNode));
                    }
                }

                if (!declaringSimpleName.getIdentifier().equals(simpleName.getIdentifier())) {
                    inconsistencies.add(new PMNameConflict(_project, pmCompilationUnit, simpleName,
                            declaringSimpleName.getIdentifier()));
                }

            } else {
                // FIXME(dcc)
                // System.err.println("!!! ignoring inconsistencies for " +
                // simpleName + "  in " + iCompilationUnit.getHandleIdentifier()
                // + " because can't find declaring node");

            }

        }

        return inconsistencies;
    }

    private Set<SimpleName> simpleNamesInCompilationUnit(CompilationUnit compilationUnit) {
        final Set<SimpleName> result = new HashSet<SimpleName>();

        compilationUnit.accept(new ASTVisitor() {
            public boolean visit(SimpleName node) {
                result.add(node);

                return true;
            }
        });

        return result;
    }

}
