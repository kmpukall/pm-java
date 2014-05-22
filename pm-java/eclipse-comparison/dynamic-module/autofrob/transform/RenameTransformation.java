/*******************************************************************************
  Copyright (C) 2008 Christoph Reichenbach

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

*******************************************************************************/

package autofrob.transform;

import org.eclipse.jdt.apt.core.build.JdtApt;
import org.eclipse.core.resources.*;
import org.eclipse.debug.core.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.apt.core.internal.AptPlugin;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.internal.junit.launcher.JUnitLaunchConfigurationConstants;
import org.eclipse.jdt.junit.launcher.JUnitLaunchConfigurationDelegate;
import org.eclipse.jdt.junit.model.*;
import org.eclipse.jdt.junit.TestRunListener;
import org.eclipse.jdt.junit.JUnitCore;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.debug.core.*;
import org.eclipse.debug.internal.core.*;
import org.eclipse.core.resources.IFile;
import org.eclipse.debug.internal.core.LaunchConfiguration;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.core.refactoring.*;
import org.eclipse.jdt.core.refactoring.descriptors.*;
import org.eclipse.ltk.core.refactoring.*;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.internal.corext.dom.NodeFinder;

import pm_refactoring.*;
import pm_refactoring.steps.*;
import pm_refactoring.inconsistencies.*;

import autofrob.model.*;

import java.util.*;
import java.io.File;

public class RenameTransformation extends DescriptorBasedTransformation
{
	private Location location;
	private String new_name;

	public
	RenameTransformation(TProject tp, Location location, String new_name)
	{
		super("rename(" + location + ", \"" + new_name.toString() + "\")", tp);
		this.new_name = new_name;
		this.location = location;
	}

	public void
	doTransformPM(final PMProject pm_project)
	{
		final SimpleName element = (SimpleName) location.getASTNode();
		final IBinding binding = element.resolveBinding();

// 		if (binding != null) {
// 			final int kind = binding.getKind();

// 			if (kind == IBinding.TYPE
// 			    || (kind == IBinding.METHOD
// 				&& ((IMethodBinding) binding).isConstructor())) {
// 				// rename the associated type and all constructors
// 				final ITypeBinding declaring_class = (kind == IBinding.TYPE)? (ITypeBinding) binding : ((IMethodBinding) binding).getDeclaringClass();
// 				final IMethodBinding[] methods = declaring_class.getDeclaredMethods();
// 				final AType type = new AType(tproject, (IType) declaring_class.getJavaElement());
// 				final LinkedList<AMethod> constructors = new LinkedList<AMethod>();

// 				for (IMethodBinding b : methods)
// 					if (b.isConstructor())
// 						constructors.add(new AMethod(tproject, (IMethod) b.getJavaElement()));

// 				// We found all that needs renaming.
// 				System.err.println("--< start >--------------------------------------");
// 				System.err.println("=| " + type + "\n" + type.getASTNode());
// 				System.err.println("-- Complex rename : " + constructors.size() + " constructors.");
// 				for (AMethod constructor : constructors) {
// 					System.err.println("-- rename constructor " + constructor);
// 					doTransformPMOne(pm_project, ((MethodDeclaration) constructor.getASTNode()).getName());
// 				}
// 				System.err.println("-- rename class " + type);

// 				final ASTNode n = type.getASTNode();
// 				SimpleName name = null;
// 				if (n instanceof SimpleName)
// 					name = (SimpleName) n;
// 				else if (n instanceof CompilationUnit) {
// 					final List<?> list = ((CompilationUnit)n).types();
// 					for (Object o : list) {
// 						if (o instanceof TypeDeclaration) {
// 							TypeDeclaration tdecl = (TypeDeclaration) o;
// 							if (name == null)
// 								name = tdecl.getName();
// 							else
// 								throw new RuntimeException("Multiple classes to rename: at least " + name + " and " + tdecl
// 											   + " (in " + type + ")!");
// 						}
// 					}
// 				} else
// 					throw new RuntimeException("Don't know how to get SimpleName from " + n.getClass());

// 				doTransformPMOne(pm_project, name);
// 				System.err.println("--< end >--------------------------------------");

// 				return;
// 			}
// 		}

		System.err.println("-- Doing simple renaming on " + element);
		doTransformPMOne(pm_project, element);
	}

	protected void
	doTransformPMOne(final PMProject pm_project, final SimpleName element)
	{
		PMRenameStep transform = new PMRenameStep(pm_project, element);
		transform.setNewName(new_name);
		transform.applyAllAtOnce();
		System.err.println("-- Rename completed.");
	}


	private static String
	getRenameKind(IJavaElement e)
	{
		switch (e.getElementType()) {

		case IJavaElement.COMPILATION_UNIT:
			return IJavaRefactorings.RENAME_COMPILATION_UNIT;

		case IJavaElement.FIELD:
			return IJavaRefactorings.RENAME_FIELD;

		case IJavaElement.LOCAL_VARIABLE:
			return IJavaRefactorings.RENAME_LOCAL_VARIABLE;

		case IJavaElement.METHOD:
			return IJavaRefactorings.RENAME_METHOD;

		case IJavaElement.TYPE:
			return IJavaRefactorings.RENAME_TYPE;

		case IJavaElement.TYPE_PARAMETER:
			return IJavaRefactorings.RENAME_TYPE_PARAMETER;

		case IJavaElement.PACKAGE_FRAGMENT:
			return IJavaRefactorings.RENAME_PACKAGE;

		case IJavaElement.PACKAGE_FRAGMENT_ROOT:
			return IJavaRefactorings.RENAME_PACKAGE;

		default: throw new RuntimeException("Unexpected object `" + e.getClass() + "' to rename");
		}
	}

	public JavaRefactoringDescriptor
	buildEclipseDescriptor() throws Exception
	{
		System.err.println("[eclipse-rename] Building descriptor...");
		IJavaElement element = location.getIJavaElement();
		String kind = getRenameKind(element);
		// [1] BUGFIX was needed:  The scripting interface didn't allow VariableDeclarationFragments as IJavaElements
		// and thus had to be extended

		// [2] WORKAROUND for scripting interface bug (see below)
		if (kind.equals(IJavaRefactorings.RENAME_METHOD)) {
			IMethod m = (IMethod) element;
			if (m.isConstructor()) {
				// Rename the type instead (as the UI would do-- the scripting interface will only rename the constructor, which is broken)
				kind = IJavaRefactorings.RENAME_TYPE;
				element = m.getDeclaringType();
			}
		}

		System.err.println("[eclipse-rename] Kind = " + kind + ",  element = " + element);

		// [3] Don't test for package fragments now
		if (kind.equals(IJavaRefactorings.RENAME_PACKAGE))
			return null; // don't bother with this now

		if (element == null) {
			System.err.println("!!! ABORT: No IJavaElement to represent location");
			throw new RuntimeException("!!! ABORT: No IJavaElement for location");
		}

		if (element instanceof ILocalVariable) {
			System.err.println("element is of type " + element.getClass());
			final ILocalVariable fLocalVariable = (ILocalVariable) element;
			final ISourceRange sourceRange= fLocalVariable.getNameRange();
			final CompilationUnit fCompilationUnitNode = location.getCompilationUnit();
			ASTNode name= NodeFinder.perform(fCompilationUnitNode, sourceRange);
			System.err.println("node is of type " + name.getClass());
			if (name == null)
				System.err.println("!!! ILV doesn't have associated name!");
			if (name.getParent() instanceof VariableDeclaration)
				System.err.println("ILV has parent : " + (VariableDeclaration) name.getParent());
			else
				System.err.println("!!! ILV doesn't have var declaration parent, instead " + name.getParent().getClass());
		}

		System.err.println("Trying to rename a " + kind + ": " + element);
		if (element instanceof SimpleName)
			System.err.println("  Name = '" + ((SimpleName) element).getIdentifier() + "'");

		if (kind.equals(IJavaRefactorings.RENAME_TYPE)) {
			System.err.println("(Possibly need a new launch configuration)");
			tproject.renameClass((IType) element, new_name);
		}


		final RenameJavaElementDescriptor descriptor =
			(RenameJavaElementDescriptor) getDescriptor(kind);
		descriptor.setJavaElement(element);
		descriptor.setNewName(this.new_name);

		if (element.getElementType() == IJavaElement.TYPE || element.getElementType() == IJavaElement.PACKAGE_FRAGMENT)
			descriptor.setUpdateQualifiedNames(true);
		else
			descriptor.setUpdateQualifiedNames(false);

		descriptor.setUpdateReferences(true);
		descriptor.setDeprecateDelegate(false);
		descriptor.setRenameGetters(false);
		descriptor.setRenameSetters(false);
		descriptor.setKeepOriginal(false);
		descriptor.setUpdateHierarchy(false);
		descriptor.setUpdateSimilarDeclarations(false);

		// [3] Fix:  Eclipse will complain if the transformation is a no-op, but we don't want that:
		if (element.getElementName().equals(this.new_name))
			throw new NOPException();

		System.err.println("[eclipse-rename] Computed descriptor.");
		return descriptor;
	}


}