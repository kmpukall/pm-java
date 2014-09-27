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

import net.creichen.pm.utils.visitors.AssignmentCollector;
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

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;

public final class ASTQuery {

    private ASTQuery() {
        // private utility class constructor
    }

    /**
     * Collects all assignments from a given method body.
     *
     * @param method
     *            the method to analyze.
     * @return a {@link List} of {@link Assignment}. Will never be null.
     */
    public static List<Assignment> findAssignments(final MethodDeclaration method) {
        AssignmentCollector collector = new AssignmentCollector();
        method.getBody().accept(collector);
        return collector.getResults();
    }

    public static TypeDeclaration findClassByName(final String className, final CompilationUnit compilationUnit) {
        Iterable<TypeDeclaration> typeDeclarations = Iterables.filter(types(compilationUnit), TypeDeclaration.class);
        Iterable<TypeDeclaration> classes = filter(typeDeclarations, isNotInterface());
        Iterable<TypeDeclaration> matchingClasses = filter(classes, hasClassName(className));

        return get(matchingClasses, 0, null);
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

    /**
     * Return the first field with the given name. If an interface is passed, this will return the first matching
     * constant definition.
     *
     * @param name
     *            the name to find a field for
     * @param type
     *            the type to find the field in.
     * @return a {@link FieldDeclaration} containing the requested field, or null if no match could be found.
     */
    public static FieldDeclaration findFieldByName(final String name, final TypeDeclaration type) {
        List<VariableDeclarationFragment> fragments = findFieldsByName(name, type);
        if (fragments.isEmpty()) {
            return null;
        }

        return (FieldDeclaration) fragments.get(0).getParent();
    }

    /**
     * Returns the first local variable declaration with the given name.
     *
     * @param name
     *            the name to find a declaration for.
     * @param method
     *            the method to find the variable in.
     * @return a {@link VariableDeclaration} for the given name, or null if no match could be found.
     */
    public static VariableDeclaration findLocalByName(final String name, final MethodDeclaration method) {
        VariableDeclarationCollector visitor = new VariableDeclarationCollector(name);
        method.getBody().accept(visitor);
        List<VariableDeclaration> results = visitor.getResults();
        return get(results, 0, null);
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

    public static ASTNode findNodeForSelection(final int selectionOffset, final int selectionLength,
            final CompilationUnit compilationUnit) {

        // Should use PMSelection once it handles the generic case!!!
        return org.eclipse.jdt.core.dom.NodeFinder.perform(compilationUnit, selectionOffset, selectionLength);
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

    public static SimpleName findSimpleNameByIdentifier(final String simpleNameIdentifier,
            final int simpleNameOccurrence, final String methodName, final int methodNameOccurrence,
            final String className, final int classNameOccurrence, final CompilationUnit compilationUnit) {
        final MethodDeclaration methodDeclaration = findMethodByName(methodName, methodNameOccurrence, className,
                classNameOccurrence, compilationUnit);

        return findSimpleNameByIdentifier(simpleNameIdentifier, simpleNameOccurrence, methodDeclaration.getBody());
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

    public static List<MethodDeclaration> getConstructors(final TypeDeclaration classDeclaration) {
        final List<MethodDeclaration> constructors = new ArrayList<MethodDeclaration>();

        for (final MethodDeclaration method : classDeclaration.getMethods()) {
            if (method.isConstructor()) {
                constructors.add(method);
            }
        }

        return constructors;
    }

    public static Collection<MethodDeclaration> getMethodsByName(final TypeDeclaration classDeclaration,
            final String methodName) {
        return Collections2.filter(Arrays.asList(classDeclaration.getMethods()), hasMethodName(methodName));
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

    public static VariableDeclaration localVariableDeclarationForSimpleName(final SimpleName name) {
        return (VariableDeclaration) ((CompilationUnit) name.getRoot()).findDeclaringNode(name.resolveBinding());
    }

    private static List<VariableDeclarationFragment> findFieldsByName(final String fieldName,
            final TypeDeclaration classDeclaration) {
        Predicate<VariableDeclaration> matcher = hasVariableName(fieldName);

        List<VariableDeclarationFragment> result = new ArrayList<VariableDeclarationFragment>();
        for (final FieldDeclaration fieldDeclaration : classDeclaration.getFields()) {
            List<VariableDeclarationFragment> fragments = fragments(fieldDeclaration);
            for (VariableDeclarationFragment fragment : fragments) {
                if (matcher.apply(fragment)) {
                    result.add(fragment);
                }
            }
        }

        return result;
    }
}
