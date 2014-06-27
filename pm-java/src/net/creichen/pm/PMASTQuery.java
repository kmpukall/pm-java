/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm;

import static net.creichen.pm.utils.APIWrapperUtil.fragments;
import static net.creichen.pm.utils.APIWrapperUtil.types;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
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

public class PMASTQuery {

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

            if (this.result == null
                    && singleVariableDeclaration.getName().getIdentifier().equals(this.localName)) {
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

    /*
     * We use classNameOccurrence to distinguish between classes when there are two classes with the
     * same name This is likely to be a common thing when testing rename
     * 
     * indexing starts are 0 (i.e. to find the first occurence, pass 0)
     */

    public static Assignment assignmentInMethodInClassInCompilationUnit(
            final int assignmentOccurrence, final String methodName,
            final int methodNameOccurrence, final String className, final int classNameOccurrence,
            final CompilationUnit compilationUnit) {
        final MethodDeclaration methodDeclaration = methodWithNameInClassInCompilationUnit(
                methodName, methodNameOccurrence, className, classNameOccurrence, compilationUnit);

        final Assignment[] result = new Assignment[1]; // use array to be able
                                                       // to return result from
                                                       // anonymous class
                                                       // method

        methodDeclaration.getBody().accept(new ASTVisitor() {

            int _assignmentOccurrence = assignmentOccurrence;

            @Override
            public boolean visit(final Assignment visitedAssignment) {

                if (result[0] == null) {
                    if (this._assignmentOccurrence == 0) {
                        result[0] = visitedAssignment;
                    } else {
                        this._assignmentOccurrence--;
                    }
                }

                return true;
            }
        });

        return result[0];
    }

    // For now, we don't deal with the case where there are two classes with the
    // same name and we need to
    // provide an index to disambiguate
    //
    // This will come up a lot though, especially with testing rename

    public static TypeDeclaration classWithNameInCompilationUnit(final String className,
            int classNameOccurrence, final CompilationUnit compilationUnit) {

        for (final AbstractTypeDeclaration abstractType : types(compilationUnit)) {
            if (abstractType instanceof TypeDeclaration) {
                final TypeDeclaration typeDeclaration = (TypeDeclaration) abstractType;

                if (!typeDeclaration.isInterface()) {
                    if (typeDeclaration.getName().getIdentifier().equals(className)) {
                        if (classNameOccurrence == 0) {
                            return typeDeclaration;
                        } else {
                            classNameOccurrence--;
                        }

                    }
                }
            }
        }

        return null;
    }

    public static VariableDeclarationFragment fieldWithNameInClassInCompilationUnit(
            final String fieldName, int fieldNameOccurrence, final String className,
            final int classNameOccurrence, final CompilationUnit compilationUnit) {
        final TypeDeclaration classDeclaration = classWithNameInCompilationUnit(className,
                classNameOccurrence, compilationUnit);

        // This is basically copied and pasted from
        // methodWithNameInClassInCompilationUnit()

        for (final FieldDeclaration fieldDeclaration : classDeclaration.getFields()) {
            for (final VariableDeclarationFragment fragment : fragments(fieldDeclaration)) {
                if (fragment.getName().getIdentifier().equals(fieldName)) {
                    if (fieldNameOccurrence == 0) {
                        return fragment;
                    } else {
                        fieldNameOccurrence--;
                    }
                }
            }
        }

        return null;
    }

    public static VariableDeclaration localWithNameInMethodInClassInCompilationUnit(
            final String localName, final int localNameOccurrence, final String methodName,
            final int methodNameOccurrence, final String className, final int classNameOccurrence,
            final CompilationUnit compilationUnit) {
        final MethodDeclaration methodDeclaration = methodWithNameInClassInCompilationUnit(
                methodName, methodNameOccurrence, className, classNameOccurrence, compilationUnit);

        // visit nodes in method body to find local vars

        final LocalFinderASTVisitor visitor = new LocalFinderASTVisitor(localName,
                localNameOccurrence);

        methodDeclaration.getBody().accept(visitor);

        return visitor.result();
    }

    public static MethodDeclaration methodWithNameInClassInCompilationUnit(final String methodName,
            int methodNameOccurrence, final String className, final int classNameOccurrence,
            final CompilationUnit compilationUnit) {
        final TypeDeclaration classDeclaration = classWithNameInCompilationUnit(className,
                classNameOccurrence, compilationUnit);

        for (final MethodDeclaration methodDeclaration : classDeclaration.getMethods()) {
            if (methodDeclaration.getName().getIdentifier().equals(methodName)) {
                if (methodNameOccurrence == 0) {
                    return methodDeclaration;
                } else {
                    methodNameOccurrence--;
                }
            }
        }

        return null;
    }

    public static ASTNode nodeForSelectionInCompilationUnit(final int selectionOffset,
            final int selectionLength, final CompilationUnit compilationUnit) {

        // Should use PMSelection once it handles the generic case!!!

        return org.eclipse.jdt.core.dom.NodeFinder.perform(compilationUnit, selectionOffset,
                selectionLength);
    }

    public static SimpleName simpleNameWithIdentifierInMethodInClassInCompilationUnit(
            final String simpleNameIdentifier, final int simpleNameOccurrence,
            final String methodName, final int methodNameOccurrence, final String className,
            final int classNameOccurrence, final CompilationUnit compilationUnit) {
        final MethodDeclaration methodDeclaration = methodWithNameInClassInCompilationUnit(
                methodName, methodNameOccurrence, className, classNameOccurrence, compilationUnit);

        return simpleNameWithIdentifierInNode(simpleNameIdentifier, simpleNameOccurrence,
                methodDeclaration.getBody());
    }

    public static SimpleName simpleNameWithIdentifierInNode(final String simpleNameIdentifier,
            final int simpleNameOccurrence, final ASTNode node) {
        final SimpleName[] result = new SimpleName[1]; // use aray to be able to
                                                       // return result from
                                                       // anonymous class
                                                       // method

        node.accept(new ASTVisitor() {

            private int _simpleNameOccurrenceCount = simpleNameOccurrence;

            @Override
            public boolean visit(final SimpleName visitedSimpleName) {

                if (result[0] == null
                        && visitedSimpleName.getIdentifier().equals(simpleNameIdentifier)) {
                    if (this._simpleNameOccurrenceCount == 0) {
                        result[0] = visitedSimpleName;
                    } else {
                        this._simpleNameOccurrenceCount--;
                    }
                }

                return true;
            }
        });

        return result[0];
    }
}
