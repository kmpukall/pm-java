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
import org.eclipse.jdt.internal.corext.refactoring.*;
import org.eclipse.jdt.internal.corext.refactoring.structure.PullUpRefactoringProcessor;
import org.eclipse.jdt.internal.corext.refactoring.structure.PullUpRefactoring;
import org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor;
import org.eclipse.jdt.core.compiler.IProblem;

import pm_refactoring.*;
import pm_refactoring.steps.*;
import pm_refactoring.inconsistencies.*;

import autofrob.model.*;
import autofrob.analysis.*;

import java.util.*;
import java.io.File;

public abstract class HierarchyTransformation extends ProcessorBasedTransformation
{
	protected AType source_type;
	protected AType dest_type;
	protected List<AElement> elements;
	protected boolean include_dependencies;
	protected DependencyComputer dependency_computer;

	protected Set<AType> dest_types = null;

	public
	HierarchyTransformation(String prefix, TProject tp, ASuperSubtypePair typair, List<AElement> elements, boolean include_dependencies, DependencyComputer dependency_computer)
	{
		super(prefix + "(" + typair + ", [" + combineElementsNames(elements) + "], " + tp + ", " + include_dependencies + ")", tp);

		init(typair);
		this.elements = elements;
		this.include_dependencies = include_dependencies;
		this.dependency_computer = dependency_computer;
	}

	/* Overrides constructor arg */
	public void
	setDestTypes(Set<AType> dt)
	{
		this.dest_types = dt;
	}

	public void
	setDestTypesToSourceChildren()
	{
		setDestTypes(source_type.getDirectSubtypes());
	}

	public static String
	combineElementsNames(List<AElement> elements)
	{
		String retval = "";
		for (AElement e : elements) {
			if (!retval.equals(""))
				retval = retval = ", ";
			retval = retval + e.toString();
		}

		return retval;
	}

	public abstract void
	init(ASuperSubtypePair typair);

	private final void
	add_aconverted_members(Set<AElement> dest, Set<? extends IMember> src)
	{
		for (IMember m : src) {
			if (m instanceof IMethod)
				dest.add(new AMethod(this.tproject, (IMethod) m));
			else if (m instanceof IField)
				dest.add(new AField(this.tproject, (IField) m));
		}
	}

	public HashSet<AElement>
	getToDelete(HashSet<AElement> elts)
	{
		return null;
	}

	private void
	doPaste(PMProject pm_project, AType dest_type, AElement element)
	{
		final ASTNode dest_parent_node = dest_type.getASTNode();
		if (dest_parent_node == null)
			throw new RuntimeException("No destination node!");
		final PMStep step1 = new PMPasteStep(pm_project, dest_parent_node, TypeDeclaration.BODY_DECLARATIONS_PROPERTY, 0);
		try {
			step1.applyAllAtOnce();
			System.err.println(" -> moved " + element);
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("(Continuing)");
				}
		IProblem[] problems = this.tproject.compilationUnit(dest_type.getICompilationUnit()).getProblems();
		System.err.println("  Total # of post-transform problems: " + problems.length);
		for (IProblem p : problems) {
			System.err.println(" post-transform problem: " + p);
		}
	}

	public void
	doTransformPM(PMProject pm_project)
	{
		final HashSet<AElement> all_elements = new HashSet<AElement>();

		all_elements.addAll(this.elements);
		System.err.println("Initial set of elements to add:");
		for (AElement e : all_elements)
			System.err.println("    + " + e);

		if (this.include_dependencies) {
			final HashSet<IMember> imembers = new HashSet<IMember>();

			for (AElement e : all_elements)
				imembers.add(e.getIMember());

			final TransitiveClosureInAST closure = new TransitiveClosureInAST(this.dependency_computer,
											  imembers,
											  source_type.getICompilationUnit(),
											  source_type.getASTNode());
			add_aconverted_members(all_elements, closure.getMethods());
			add_aconverted_members(all_elements, closure.getFields());

			System.err.println("After expansion: elements to add:");
			for (AElement e : all_elements)
				System.err.println("    + " + e);
		}

		final HashSet<AElement> to_delete = getToDelete(all_elements);

		for (AElement element : all_elements) {
			final ASTNode source_node = element.getASTNode();
			if (source_node != null) {
				final PMStep step0 = new PMCutStep(pm_project, source_node);
				step0.applyAllAtOnce();

				if (dest_types == null)
					doPaste(pm_project, dest_type, element);
				else
					for (AType t : dest_types)
						doPaste(pm_project, t, element);

			}
		}

		if (to_delete != null) {
			all_elements.removeAll(to_delete);

			for (AElement element : to_delete)
				if (element.exists()) {
					final ASTNode source_node = element.getASTNode();

					if (source_node != null) {
						final String body = source_node.toString();

						try {
							final PMStep step0 = new PMCutStep(pm_project, source_node);
							step0.applyAllAtOnce();

							boolean done = false;
							do {
								final Set<PMInconsistency> inconsistencies = pm_project.allInconsistencies();
								done = true;
								for (PMInconsistency incons : inconsistencies) {
									if (incons instanceof PMNameCapture)
										if ((((PMNameCapture) incons).getActualDeclaration()).toString().equals(body)) {
											incons.acceptBehavioralChange();
											System.err.println(" + " + incons + "(Accepting behavioural change)");
											done = false;
											break;
										}
								}
							} while (!done);
						} catch (Exception e) {
							e.printStackTrace();
							System.err.println("(Continuing)");
						}
					}
				}
		}
	}
}