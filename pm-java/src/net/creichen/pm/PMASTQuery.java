/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm;

import java.util.List;

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

	@SuppressWarnings("restriction")
	static public ASTNode nodeForSelectionInCompilationUnit(
			int selectionOffset, int selectionLength,
			CompilationUnit compilationUnit) {

		// Should use PMSelection once it handles the generic case!!!

		return org.eclipse.jdt.internal.corext.dom.NodeFinder.perform(
				compilationUnit, selectionOffset, selectionLength);
	}

	/*
	 * We use classNameOccurrence to distinguish between classes when there are
	 * two classes with the same name This is likely to be a common thing when
	 * testing rename
	 * 
	 * indexing starts are 0 (i.e. to find the first occurence, pass 0)
	 */

	static public TypeDeclaration classWithNameInCompilationUnit(
			String className, int classNameOccurrence,
			CompilationUnit compilationUnit) {

		for (AbstractTypeDeclaration abstractType : (List<AbstractTypeDeclaration>) compilationUnit
				.types()) {
			if (abstractType instanceof TypeDeclaration) {
				TypeDeclaration typeDeclaration = (TypeDeclaration) abstractType;

				if (!typeDeclaration.isInterface()) {
					if (typeDeclaration.getName().getIdentifier()
							.equals(className)) {
						if (classNameOccurrence == 0)
							return typeDeclaration;
						else
							classNameOccurrence--;

					}
				}
			}
		}

		return null;
	}

	// For now, we don't deal with the case where there are two classes with the
	// same name and we need to
	// provide an index to disambiguate
	//
	// This will come up a lot though, especially with testing rename

	static public MethodDeclaration methodWithNameInClassInCompilationUnit(
			String methodName, int methodNameOccurrence, String className,
			int classNameOccurrence, CompilationUnit compilationUnit) {
		TypeDeclaration classDeclaration = classWithNameInCompilationUnit(
				className, classNameOccurrence, compilationUnit);

		for (MethodDeclaration methodDeclaration : classDeclaration
				.getMethods()) {
			if (methodDeclaration.getName().getIdentifier().equals(methodName)) {
				if (methodNameOccurrence == 0)
					return methodDeclaration;
				else
					methodNameOccurrence--;
			}
		}

		return null;
	}

	static public VariableDeclarationFragment fieldWithNameInClassInCompilationUnit(
			String fieldName, int fieldNameOccurrence, String className,
			int classNameOccurrence, CompilationUnit compilationUnit) {
		TypeDeclaration classDeclaration = classWithNameInCompilationUnit(
				className, classNameOccurrence, compilationUnit);

		// This is basically copied and pasted from
		// methodWithNameInClassInCompilationUnit()

		for (FieldDeclaration fieldDeclaration : classDeclaration.getFields()) {
			for (VariableDeclarationFragment fragment : (List<VariableDeclarationFragment>) fieldDeclaration
					.fragments()) {
				if (fragment.getName().getIdentifier().equals(fieldName)) {
					if (fieldNameOccurrence == 0)
						return fragment;
					else
						fieldNameOccurrence--;
				}
			}
		}

		return null;
	}

	static public VariableDeclaration localWithNameInMethodInClassInCompilationUnit(
			String localName, int localNameOccurrence, String methodName,
			int methodNameOccurrence, String className,
			int classNameOccurrence, CompilationUnit compilationUnit) {
		MethodDeclaration methodDeclaration = methodWithNameInClassInCompilationUnit(
				methodName, methodNameOccurrence, className,
				classNameOccurrence, compilationUnit);

		// visit nodes in method body to find local vars

		LocalFinderASTVisitor visitor = new LocalFinderASTVisitor(localName,
				localNameOccurrence);

		methodDeclaration.getBody().accept(visitor);

		return visitor.result();
	}

	static public SimpleName simpleNameWithIdentifierInMethodInClassInCompilationUnit(
			String simpleNameIdentifier, int simpleNameOccurrence,
			String methodName, int methodNameOccurrence, String className,
			int classNameOccurrence, CompilationUnit compilationUnit) {
		MethodDeclaration methodDeclaration = methodWithNameInClassInCompilationUnit(
				methodName, methodNameOccurrence, className,
				classNameOccurrence, compilationUnit);

		return simpleNameWithIdentifierInNode(simpleNameIdentifier,
				simpleNameOccurrence, methodDeclaration.getBody());
	}

	static public SimpleName simpleNameWithIdentifierInNode(
			final String simpleNameIdentifier, final int simpleNameOccurrence,
			ASTNode node) {
		final SimpleName[] result = new SimpleName[1]; // use aray to be able to
														// return result from
														// anonymous class
														// method

		node.accept(new ASTVisitor() {

			int _simpleNameOccurrenceCount = simpleNameOccurrence;

			public boolean visit(SimpleName visitedSimpleName) {

				if (result[0] == null
						&& visitedSimpleName.getIdentifier().equals(
								simpleNameIdentifier)) {
					if (_simpleNameOccurrenceCount == 0)
						result[0] = visitedSimpleName;
					else
						_simpleNameOccurrenceCount--;
				}

				return true;
			}
		});

		return result[0];
	}

	static public Assignment assignmentInMethodInClassInCompilationUnit(
			final int assignmentOccurrence, String methodName,
			int methodNameOccurrence, String className,
			int classNameOccurrence, CompilationUnit compilationUnit) {
		MethodDeclaration methodDeclaration = methodWithNameInClassInCompilationUnit(
				methodName, methodNameOccurrence, className,
				classNameOccurrence, compilationUnit);

		final Assignment[] result = new Assignment[1]; // use array to be able
														// to return result from
														// anonymous class
														// method

		methodDeclaration.getBody().accept(new ASTVisitor() {

			int _assignmentOccurrence = assignmentOccurrence;

			public boolean visit(Assignment visitedAssignment) {

				if (result[0] == null) {
					if (_assignmentOccurrence == 0)
						result[0] = visitedAssignment;
					else
						_assignmentOccurrence--;
				}

				return true;
			}
		});

		return result[0];
	}

	static private class LocalFinderASTVisitor extends ASTVisitor {
		String _localName;
		int _localNameOccurrence;
		int _localNameOccurrenceCounter;

		VariableDeclaration _result;

		public LocalFinderASTVisitor(String localName, int localNameOccurrence) {
			_localName = localName;
			_localNameOccurrence = localNameOccurrence;

			_localNameOccurrenceCounter = localNameOccurrence; // counts down to
																// zero

		}

		public VariableDeclaration result() {
			return _result;
		}

		// visitor methods

		public boolean visit(AnonymousClassDeclaration anonymousClass) {

			// we ignore anonymous classes for now

			return false;
		}

		public boolean visit(VariableDeclarationFragment fragment) {

			if (_result == null
					&& fragment.getName().getIdentifier().equals(_localName)) {

				if (_localNameOccurrenceCounter == 0) {
					_result = fragment;
				} else
					_localNameOccurrenceCounter--;
			}

			return true;
		}

		public boolean visit(SingleVariableDeclaration singleVariableDeclaration) {

			if (_result == null
					&& singleVariableDeclaration.getName().getIdentifier()
							.equals(_localName)) {
				if (_localNameOccurrenceCounter == 0) {
					_result = singleVariableDeclaration;
				} else
					_localNameOccurrenceCounter--;
			}

			return true;
		}
	}
}
