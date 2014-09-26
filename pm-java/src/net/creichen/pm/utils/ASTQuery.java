/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm.utils;

import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Iterables.get;
import static net.creichen.pm.utils.APIWrapperUtil.fragments;
import static net.creichen.pm.utils.APIWrapperUtil.types;
import static net.creichen.pm.utils.factories.PredicateFactory.hasClassName;
import static net.creichen.pm.utils.factories.PredicateFactory.hasMethodName;
import static net.creichen.pm.utils.factories.PredicateFactory.hasVariableName;
import static net.creichen.pm.utils.factories.PredicateFactory.isNotInterface;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import net.creichen.pm.utils.visitors.VariableDeclarationCollector;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.TypeParameter;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;

public final class ASTQuery {

    public static VariableDeclaration localVariableDeclarationForSimpleName(final SimpleName name) {
        return (VariableDeclaration) ((CompilationUnit) name.getRoot()).findDeclaringNode(name.resolveBinding());
    }

    // Hmmm, this assumes there is only one simple name for a given declaring
    // node
    public static SimpleName getSimpleName(final ASTNode declaringNode) {
        if (declaringNode instanceof VariableDeclarationFragment) {
            return ((VariableDeclarationFragment) declaringNode).getName();
        } else if (declaringNode instanceof SingleVariableDeclaration) {
            return ((SingleVariableDeclaration) declaringNode).getName();
        } else if (declaringNode instanceof VariableDeclarationFragment) {
            return ((VariableDeclarationFragment) declaringNode).getName();
        } else if (declaringNode instanceof TypeDeclaration) {
            return ((TypeDeclaration) declaringNode).getName();
        } else if (declaringNode instanceof MethodDeclaration) {
            return ((MethodDeclaration) declaringNode).getName();
        } else if (declaringNode instanceof TypeParameter) {
            return ((TypeParameter) declaringNode).getName();
        } else {
            throw new IllegalArgumentException("Unable to find simple name for ASTNode " + declaringNode + " of class "
                    + declaringNode.getClass());
        }
    }

    public static List<MethodDeclaration> getConstructorsOfClass(final TypeDeclaration classDeclaration) {
        final List<MethodDeclaration> constructors = new ArrayList<MethodDeclaration>();

        for (final MethodDeclaration method : classDeclaration.getMethods()) {
            if (method.isConstructor()) {
                constructors.add(method);
            }
        }

        return constructors;
    }

    public static Assignment findAssignmentInMethod(final int assignmentOccurrence, final String methodName,
            final int methodNameOccurrence, final String className, final int classNameOccurrence,
            final CompilationUnit compilationUnit) {
        final MethodDeclaration methodDeclaration = findMethodByName(methodName, methodNameOccurrence, className,
                classNameOccurrence, compilationUnit);

        final Assignment[] result = new Assignment[1]; // use array to be able
        // to return result from
        // anonymous class
        // method

        methodDeclaration.getBody().accept(new ASTVisitor() {

            private int occurrence = assignmentOccurrence;

            @Override
            public boolean visit(final Assignment visitedAssignment) {

                if (result[0] == null) {
                    if (this.occurrence == 0) {
                        result[0] = visitedAssignment;
                    } else {
                        this.occurrence--;
                    }
                }

                return true;
            }
        });

        return result[0];
    }

    /**
     * We use classNameOccurrence to distinguish between classes when there are two classes with the same name This is
     * likely to be a common thing when testing rename
     *
     * indexing starts are 0 (i.e. to find the first occurence, pass 0)
     */
    public static TypeDeclaration findClassByName(final String className, final int classNameOccurrence,
            final CompilationUnit compilationUnit) {

        Iterable<TypeDeclaration> typeDeclarations = filter(types(compilationUnit), TypeDeclaration.class);
        Iterable<TypeDeclaration> classes = filter(typeDeclarations, isNotInterface());
        Iterable<TypeDeclaration> matchingClasses = filter(classes, hasClassName(className));

        return get(matchingClasses, classNameOccurrence, null);
    }

    public static TypeDeclaration findClassByName(final String className, final CompilationUnit compilationUnit) {

        Iterable<TypeDeclaration> typeDeclarations = filter(types(compilationUnit), TypeDeclaration.class);
        Iterable<TypeDeclaration> classes = filter(typeDeclarations, isNotInterface());
        Iterable<TypeDeclaration> matchingClasses = filter(classes, hasClassName(className));

        return get(matchingClasses, 0, null);
    }

    public static VariableDeclarationFragment findFieldByName(final String fieldName, final int fieldNameOccurrence,
            final String className, final int classNameOccurrence, final CompilationUnit compilationUnit) {
        final TypeDeclaration classDeclaration = findClassByName(className, classNameOccurrence, compilationUnit);
        Iterable<VariableDeclarationFragment> matchingFragments = getFieldsByName(classDeclaration, fieldName);
        return Iterables.get(matchingFragments, fieldNameOccurrence, null);
    }

    public static VariableDeclarationFragment findFieldByName(final TypeDeclaration classDeclaration,
            final String fieldName) {
        Iterable<VariableDeclarationFragment> matchingFragments = getFieldsByName(classDeclaration, fieldName);
        return Iterables.get(matchingFragments, 0, null);
    }

    public static Iterable<VariableDeclarationFragment> getFieldsByName(final TypeDeclaration classDeclaration,
            final String fieldName) {
        List<VariableDeclarationFragment> fragments = new ArrayList<VariableDeclarationFragment>();
        for (final FieldDeclaration fieldDeclaration : classDeclaration.getFields()) {
            fragments.addAll(fragments(fieldDeclaration));
        }

        return filter(fragments, hasVariableName(fieldName));
    }

    public static VariableDeclaration findLocalByName(final String localName, final int localNameOccurrence,
            final String methodName, final int methodNameOccurrence, final String className,
            final int classNameOccurrence, final CompilationUnit compilationUnit) {

        final MethodDeclaration methodDeclaration = findMethodByName(methodName, methodNameOccurrence, className,
                classNameOccurrence, compilationUnit);
        // visit nodes in method body to find local vars
        VariableDeclarationCollector visitor = new VariableDeclarationCollector();
        methodDeclaration.getBody().accept(visitor);
        List<VariableDeclaration> results = visitor.getResults();
        Iterable<VariableDeclaration> matchingDeclarations = filter(results, hasVariableName(localName));

        return get(matchingDeclarations, localNameOccurrence, null);
    }

    public static MethodDeclaration findMethodByName(final String methodName, final int methodNameOccurrence,
            final String className, final int classNameOccurrence, final CompilationUnit compilationUnit) {
        final TypeDeclaration classDeclaration = findClassByName(className, classNameOccurrence, compilationUnit);
        Collection<MethodDeclaration> matchingMethods = getMethodsByName(classDeclaration, methodName);

        return get(matchingMethods, methodNameOccurrence, null);
    }

    public static MethodDeclaration findMethodByName(final String methodName, final TypeDeclaration classDeclaration) {
        Collection<MethodDeclaration> matchingMethods = getMethodsByName(classDeclaration, methodName);

        return get(matchingMethods, 0, null);
    }

    public static Collection<MethodDeclaration> getMethodsByName(final TypeDeclaration classDeclaration,
            final String methodName) {
        return Collections2.filter(Arrays.asList(classDeclaration.getMethods()), hasMethodName(methodName));
    }

    public static ASTNode findNodeForSelection(final int selectionOffset, final int selectionLength,
            final CompilationUnit compilationUnit) {

        // Should use PMSelection once it handles the generic case!!!
        return org.eclipse.jdt.core.dom.NodeFinder.perform(compilationUnit, selectionOffset, selectionLength);
    }

    public static SimpleName findSimpleNameByIdentifier(final String simpleNameIdentifier,
            final int simpleNameOccurrence, final String methodName, final int methodNameOccurrence,
            final String className, final int classNameOccurrence, final CompilationUnit compilationUnit) {
        final MethodDeclaration methodDeclaration = findMethodByName(methodName, methodNameOccurrence, className,
                classNameOccurrence, compilationUnit);

        return findSimpleNameByIdentifier(simpleNameIdentifier, simpleNameOccurrence, methodDeclaration.getBody());
    }

    public static SimpleName findSimpleNameByIdentifier(final String simpleNameIdentifier,
            final int simpleNameOccurrence, final ASTNode node) {
        final SimpleName[] result = new SimpleName[1]; // use aray to be able to
        // return result from
        // anonymous class
        // method

        node.accept(new ASTVisitor() {

            private int simpleNameOccurrenceCount = simpleNameOccurrence;

            @Override
            public boolean visit(final SimpleName visitedSimpleName) {

                if (result[0] == null && visitedSimpleName.getIdentifier().equals(simpleNameIdentifier)) {
                    if (this.simpleNameOccurrenceCount == 0) {
                        result[0] = visitedSimpleName;
                    } else {
                        this.simpleNameOccurrenceCount--;
                    }
                }

                return true;
            }
        });

        return result[0];
    }

    public static List<SimpleName> findSimpleNames(final String identifier, final ASTNode node) {
        final List<SimpleName> result = new ArrayList<SimpleName>();

        node.accept(new ASTVisitor() {

            @Override
            public boolean visit(final SimpleName visitedSimpleName) {

                if (visitedSimpleName.getIdentifier().equals(identifier)) {
                    result.add(visitedSimpleName);
                }
                return true;
            }
        });

        return result;
    }

    private ASTQuery() {
        // private utility class constructor
    }
}
