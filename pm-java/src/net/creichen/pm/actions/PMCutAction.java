/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm.actions;

import java.util.ArrayList;
import java.util.List;

import net.creichen.pm.PMProject;
import net.creichen.pm.PMWorkspace;
import net.creichen.pm.selection.PMSelection;
import net.creichen.pm.steps.PMCutStep;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor;
import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;

public class PMCutAction extends PMAction {

	@Override
	public RefactoringProcessor newProcessor() {

		return null;
	}

	@Override
	public UserInputWizardPage newWizardInputPage(RefactoringProcessor processor) {
		return null;
	}

	public void run(IAction action) {
		System.err.println("In PMCutAction run()");

		if (getSelection() instanceof ITextSelection) {

			ICompilationUnit iCompilationUnit = currentICompilationUnit();

			PMProject project = PMWorkspace.sharedWorkspace()
					.projectForIJavaProject(iCompilationUnit.getJavaProject());

			ITextSelection textSelection = (ITextSelection) getSelection();

			project.syncSources(); // in case they have changed behid our backs

			PMSelection selectionDescriptor = new PMSelection(
					(CompilationUnit) project
							.findASTRootForICompilationUnit(iCompilationUnit),
					textSelection.getOffset(), textSelection.getLength());

			if (selectionDescriptor.isSaneSelection()) {
				List<ASTNode> nodesToCut = new ArrayList<ASTNode>();

				if (!selectionDescriptor.isListSelection()) {
					ASTNode selectedNode = selectionDescriptor
							.singleSelectedNode(); // project.nodeForSelection((ITextSelection)getSelection(),
													// iCompilationUnit);

					System.err.println("selected node is " + selectedNode + "["
							+ selectedNode.getClass() + "]");

					if (selectedNode instanceof Statement
							|| selectedNode instanceof FieldDeclaration
							|| selectedNode instanceof MethodDeclaration) {
						nodesToCut.add(selectedNode);
					} else {
						System.err
								.println("PMCutAction must be run on a selected statement or method or field");

						showErrorDialog(
								"PM Cut Error",
								"We currently only support PM Cut on statements, methods, and fields -- you've selected a "
										+ selectedNode.getClass());
					}
				} else {

					List<ASTNode> propertyList = (List<ASTNode>) selectionDescriptor
							.selectedNodeParent().getStructuralProperty(
									selectionDescriptor
											.selectedNodeParentProperty());

					for (int i = selectionDescriptor
							.selectedNodeParentPropertyListOffset(); i < selectionDescriptor
							.selectedNodeParentPropertyListOffset()
							+ selectionDescriptor
									.selectedNodeParentPropertyListLength(); i++) {
						nodesToCut.add(propertyList.get(i));
					}

				}

				if (nodesToCut.size() > 0) {
					PMCutStep cutStep = new PMCutStep(project, nodesToCut);

					cutStep.applyAllAtOnce();
				}

			} else {
				System.err
						.println("PMRCutAction: this selection is not PM-Cuttable");

				showErrorDialog("PM Cut Error",
						"PMCut cannot be applied to this selection");
			}

		} else {
			System.err.println("PMRCutAction must be run on a text selection.");

			showErrorDialog("PM Cut Error",
					"PM Cut must be run on a text selection.");
		}
	}

}
