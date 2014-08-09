/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm.analysis;

import static com.google.common.collect.Iterables.filter;
import static net.creichen.pm.utils.APIWrapperUtil.fragments;
import static net.creichen.pm.utils.APIWrapperUtil.types;
import static net.creichen.pm.utils.factories.PredicateFactory.hasClassName;
import static net.creichen.pm.utils.factories.PredicateFactory.isNotInterface;

import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import com.google.common.collect.Lists;

public final class ASTQuery {

    private static class LocalFinderASTVisitor extends ASTVisitor {
        private final String localName;
        private int localNameOccurrenceCounter;

        private VariableDeclaration result;

        public LocalFinderASTVisitor(final String localName, final int localNameOccurrence) {
            this.localName = localName;

            this.localNameOccurrenceCounter = localNameOccurrence; // counts down to
            // zero

        }

        public VariableDeclaration result() {
            return this.result;
        }

        // visitor methods

        @Override
        public boolean visit(final AnonymousClassDeclaration anonymousClass) {

            // we ignore anonymous classes for now

            return false;
        }

        @Override
        public boolean visit(final SingleVariableDeclaration singleVariableDeclaration) {

            if (this.result == null && singleVariableDeclaration.getName().getIdentifier().equals(this.localName)) {
                if (this.localNameOccurrenceCounter == 0) {
                    this.result = singleVariableDeclaration;
                } else {
                    this.localNameOccurrenceCounter--;
                }
            }

            return true;
        }

        @Override
        public boolean visit(final VariableDeclarationFragment fragment) {

            if (this.result == null && fragment.getName().getIdentifier().equals(this.localName)) {

                if (this.localNameOccurrenceCounter == 0) {
                    this.result = fragment;
                } else {
                    this.localNameOccurrenceCounter--;
                }
            }

            return true;
        }
    }

    public static Assignment assignmentInMethodInClassInCompilationUnit(final int assignmentOccurrence,
            final String methodName, final int methodNameOccurrence, final String className,
            final int classNameOccurrence, final CompilationUnit compilationUnit) {
        final MethodDeclaration methodDeclaration = methodWithNameInClassInCompilationUnit(methodName,
                methodNameOccurrence, className, classNameOccurrence, compilationUnit);

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
    public static TypeDeclaration findClassWithName(final String className, final int classNameOccurrence,
            final CompilationUnit compilationUnit) {
        List<TypeDeclaration> matchingClasses = findClassesWithName(compilationUnit, className);

        if (matchingClasses.size() > classNameOccurrence) {
            return matchingClasses.get(classNameOccurrence);
        }

        return null;
    }

    private static List<TypeDeclaration> findClassesWithName(final CompilationUnit compilationUnit,
            final String className) {
        Iterable<TypeDeclaration> typeDeclarations = filter(types(compilationUnit), TypeDeclaration.class);
        Iterable<TypeDeclaration> classes = filter(typeDeclarations, isNotInterface());
        Iterable<TypeDeclaration> matchingClasses = filter(classes, hasClassName(className));
        return Lists.newArrayList(matchingClasses);
    }

    public static VariableDeclarationFragment fieldWithNameInClassInCompilationUnit(final String fieldName,
            final int fieldNameOccurrence, final String className, final int classNameOccurrence,
            final CompilationUnit compilationUnit) {
        final TypeDeclaration classDeclaration = findClassWithName(className, classNameOccurrence, compilationUnit);

        // This is basically copied and pasted from
        // methodWithNameInClassInCompilationUnit()

        int occurrence = fieldNameOccurrence;
        for (final FieldDeclaration fieldDeclaration : classDeclaration.getFields()) {
            for (final VariableDeclarationFragment fragment : fragments(fieldDeclaration)) {
                if (fragment.getName().getIdentifier().equals(fieldName)) {
                    if (occurrence == 0) {
                        return fragment;
                    } else {
                        occurrence--;
                    }
                }
            }
        }

        return null;
    }

    public static VariableDeclaration localWithNameInMethodInClassInCompilationUnit(final String localName,
            final int localNameOccurrence, final String methodName, final int methodNameOccurrence,
            final String className, final int classNameOccurrence, final CompilationUnit compilationUnit) {
        final MethodDeclaration methodDeclaration = methodWithNameInClassInCompilationUnit(methodName,
                methodNameOccurrence, className, classNameOccurrence, compilationUnit);

        // visit nodes in method body to find local vars

        final LocalFinderASTVisitor visitor = new LocalFinderASTVisitor(localName, localNameOccurrence);

        methodDeclaration.getBody().accept(visitor);

        return visitor.result();
    }

    public static MethodDeclaration methodWithNameInClassInCompilationUnit(final String methodName,
            final int methodNameOccurrence, final String className, final int classNameOccurrence,
            final CompilationUnit compilationUnit) {
        final TypeDeclaration classDeclaration = findClassWithName(className, classNameOccurrence, compilationUnit);

        int occurrence = methodNameOccurrence;
        for (final MethodDeclaration methodDeclaration : classDeclaration.getMethods()) {
            if (methodDeclaration.getName().getIdentifier().equals(methodName)) {
                if (occurrence == 0) {
                    return methodDeclaration;
                } else {
                    occurrence--;
                }
            }
        }

        return null;
    }

    public static ASTNode nodeForSelectionInCompilationUnit(final int selectionOffset, final int selectionLength,
            final CompilationUnit compilationUnit) {

        // Should use PMSelection once it handles the generic case!!!

        return org.eclipse.jdt.core.dom.NodeFinder.perform(compilationUnit, selectionOffset, selectionLength);
    }

    public static SimpleName simpleNameWithIdentifierInMethodInClassInCompilationUnit(
            final String simpleNameIdentifier, final int simpleNameOccurrence, final String methodName,
            final int methodNameOccurrence, final String className, final int classNameOccurrence,
            final CompilationUnit compilationUnit) {
        final MethodDeclaration methodDeclaration = methodWithNameInClassInCompilationUnit(methodName,
                methodNameOccurrence, className, classNameOccurrence, compilationUnit);

        return simpleNameWithIdentifierInNode(simpleNameIdentifier, simpleNameOccurrence, methodDeclaration.getBody());
    }

    public static SimpleName simpleNameWithIdentifierInNode(final String simpleNameIdentifier,
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
