/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm.analysis;

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
import java.util.List;

import net.creichen.pm.utils.visitors.VariableDeclarationFinder;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import com.google.common.collect.Iterables;

public final class ASTQuery {

    public static Assignment assignmentInMethodInClassInCompilationUnit(final int assignmentOccurrence,
            final String methodName, final int methodNameOccurrence, final String className,
            final int classNameOccurrence, final CompilationUnit compilationUnit) {
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

    /**
     *
     * @param fieldName
     * @param fieldNameOccurrence
     * @param className
     * @param classNameOccurrence
     * @param compilationUnit
     * @return
     */
    public static VariableDeclarationFragment findFieldByName(final String fieldName, final int fieldNameOccurrence,
            final String className, final int classNameOccurrence, final CompilationUnit compilationUnit) {
        final TypeDeclaration classDeclaration = findClassByName(className, classNameOccurrence, compilationUnit);

        List<VariableDeclarationFragment> fragments = new ArrayList<VariableDeclarationFragment>();
        for (final FieldDeclaration fieldDeclaration : classDeclaration.getFields()) {
            fragments.addAll(fragments(fieldDeclaration));
        }

        Iterable<VariableDeclarationFragment> matchingFragments = filter(fragments, hasVariableName(fieldName));
        return Iterables.get(matchingFragments, fieldNameOccurrence, /* or default to */null);
    }

    /**
     *
     * @param localName
     * @param localNameOccurrence
     * @param methodName
     * @param methodNameOccurrence
     * @param className
     * @param classNameOccurrence
     * @param compilationUnit
     * @return
     */
    public static VariableDeclaration findLocalByName(final String localName, final int localNameOccurrence,
            final String methodName, final int methodNameOccurrence, final String className,
            final int classNameOccurrence, final CompilationUnit compilationUnit) {

        final MethodDeclaration methodDeclaration = findMethodByName(methodName, methodNameOccurrence, className,
                classNameOccurrence, compilationUnit);
        // visit nodes in method body to find local vars
        VariableDeclarationFinder visitor = new VariableDeclarationFinder();
        methodDeclaration.getBody().accept(visitor);
        List<VariableDeclaration> results = visitor.result();
        Iterable<VariableDeclaration> matchingDeclarations = filter(results, hasVariableName(localName));

        return get(matchingDeclarations, localNameOccurrence, null);
    }

    /**
     *
     * @param methodName
     * @param methodNameOccurrence
     * @param className
     * @param classNameOccurrence
     * @param compilationUnit
     * @return
     */
    public static MethodDeclaration findMethodByName(final String methodName, final int methodNameOccurrence,
            final String className, final int classNameOccurrence, final CompilationUnit compilationUnit) {
        final TypeDeclaration classDeclaration = findClassByName(className, classNameOccurrence, compilationUnit);

        Iterable<MethodDeclaration> matchingMethods = filter(Arrays.asList(classDeclaration.getMethods()),
                hasMethodName(methodName));

        return get(matchingMethods, methodNameOccurrence, null);
    }

    /**
     *
     * @param selectionOffset
     * @param selectionLength
     * @param compilationUnit
     * @return
     */
    public static ASTNode findNodeForSelection(final int selectionOffset, final int selectionLength,
            final CompilationUnit compilationUnit) {

        // Should use PMSelection once it handles the generic case!!!
        return org.eclipse.jdt.core.dom.NodeFinder.perform(compilationUnit, selectionOffset, selectionLength);
    }

    /**
     *
     * @param simpleNameIdentifier
     * @param simpleNameOccurrence
     * @param methodName
     * @param methodNameOccurrence
     * @param className
     * @param classNameOccurrence
     * @param compilationUnit
     * @return
     */
    public static SimpleName findSimpleNameByIdentifier(
            final String simpleNameIdentifier, final int simpleNameOccurrence, final String methodName,
            final int methodNameOccurrence, final String className, final int classNameOccurrence,
            final CompilationUnit compilationUnit) {
        final MethodDeclaration methodDeclaration = findMethodByName(methodName, methodNameOccurrence, className,
                classNameOccurrence, compilationUnit);

        return findSimpleNameByIdentifier(simpleNameIdentifier, simpleNameOccurrence, methodDeclaration.getBody());
    }

    /**
     *
     * @param simpleNameIdentifier
     * @param simpleNameOccurrence
     * @param node
     * @return
     */
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

    private ASTQuery() {
        // private utility class constructor
    }
}
