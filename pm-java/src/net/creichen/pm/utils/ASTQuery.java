/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm.utils;

import static com.google.common.collect.Iterables.get;
import static net.creichen.pm.utils.factories.PredicateFactory.hasMethodName;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import net.creichen.pm.api.PMCompilationUnit;
import net.creichen.pm.utils.visitors.collectors.AssignmentCollector;
import net.creichen.pm.utils.visitors.collectors.SelectiveSimpleNameCollector;
import net.creichen.pm.utils.visitors.collectors.SimpleNameCollector;
import net.creichen.pm.utils.visitors.finders.ClassFinder;
import net.creichen.pm.utils.visitors.finders.FieldFinder;
import net.creichen.pm.utils.visitors.finders.SimpleNameFinder;
import net.creichen.pm.utils.visitors.finders.VariableDeclarationFinder;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.NodeFinder;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.TypeParameter;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import com.google.common.collect.Collections2;

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
        return new AssignmentCollector().collectFrom(method.getBody());
    }

    /**
     * Returns the first class with the given name in the compilation unit.
     *
     * @param className
     * @param compilationUnit
     * @return the class, or null if no matching class could be found.
     * @deprecated use {@link #findClassByName(String, PMCompilationUnit)} instead
     */
    @Deprecated
    public static TypeDeclaration findClassByName(final String className, final CompilationUnit compilationUnit) {
        return new ClassFinder(className).findOn(compilationUnit);
    }

    /**
     * Returns the first class with the given name in the compilation unit.
     *
     * @param className
     * @param compilationUnit
     * @return the class, or null if no matching class could be found.
     */
    public static TypeDeclaration findClassByName(final String className, final PMCompilationUnit compilationUnit) {
        return new ClassFinder(className).findOn(compilationUnit);
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
        return new FieldFinder(name).findOn(type);
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
        return new VariableDeclarationFinder(name).findOn(method.getBody());
    }

    public static MethodDeclaration findMethodByName(final String methodName, final TypeDeclaration classDeclaration) {
        Collection<MethodDeclaration> matchingMethods = getMethodsByName(classDeclaration, methodName);

        return get(matchingMethods, 0, null);
    }

    public static ASTNode findNodeForSelection(final int selectionOffset, final int selectionLength,
            final CompilationUnit compilationUnit) {
        // Should use PMSelection once it handles the generic case!!!
        return NodeFinder.perform(compilationUnit, selectionOffset, selectionLength);
    }

    public static <T extends ASTNode> T findParent(final ASTNode node, final Class<T> type) {
        T result = null;
        ASTNode parent = node;
        while (parent != null) {
            if (type.isInstance(parent)) {
                result = type.cast(parent);
                break;
            }
            parent = parent.getParent();
        }
        return result;
    }

    public static SimpleName findSimpleName(final String identifier, final ASTNode node) {
        return new SimpleNameFinder(identifier).findOn(node);
    }

    public static SimpleName findSimpleName(final String identifier, final PMCompilationUnit compilationUnit) {
        return new SimpleNameFinder(identifier).findOn(compilationUnit);
    }

    public static SimpleName findSimpleName(final String identifier, final int index, final ASTNode node) {
        List<SimpleName> simpleNames = findSimpleNames(identifier, node);
        return get(simpleNames, index, null);
    }

    /**
     * Returns a list of all SimpleNames from the given node.
     *
     * @param node
     * @return
     */
    public static List<SimpleName> findSimpleNames(final ASTNode node) {
        return new SimpleNameCollector().collectFrom(node);
    }

    /**
     * Returns a list of all SimpleNames with the given identifier from a node.
     *
     * @param identifier
     * @param node
     * @return
     */
    public static List<SimpleName> findSimpleNames(final String identifier, final ASTNode node) {
        return new SelectiveSimpleNameCollector(identifier).collectFrom(node);
    }

    public static List<SimpleName> findSimpleNames(final String identifier, final PMCompilationUnit compilationUnit) {
        return new SelectiveSimpleNameCollector(identifier).collectFrom(compilationUnit);
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
    public static SimpleName resolveSimpleName(final ASTNode declaringNode) {
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

}
