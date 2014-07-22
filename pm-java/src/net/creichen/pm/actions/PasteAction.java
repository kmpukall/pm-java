/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm.actions;

import net.creichen.pm.PMProject;
import net.creichen.pm.PMWorkspace;
import net.creichen.pm.Pasteboard;
import net.creichen.pm.selection.InsertionPoint;
import net.creichen.pm.selection.InsertionPointFactory;
import net.creichen.pm.steps.PasteStep;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor;
import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;

public class PasteAction extends Action {

	@Override
	public RefactoringProcessor newProcessor() {

		return null;
	}

	@Override
	public UserInputWizardPage newWizardInputPage(final RefactoringProcessor processor) {
		return null;
	}

	@Override
	public void run(final IAction action) {
		System.err.println("In PMPasteAction run()");

		if (getSelection() instanceof ITextSelection) {

			final ITextSelection textSelection = (ITextSelection) getSelection();

			final ICompilationUnit iCompilationUnit = currentICompilationUnit();

			final PMProject project = PMWorkspace.sharedWorkspace().projectForIJavaProject(
					iCompilationUnit.getJavaProject());

			final InsertionPoint insertionPoint = InsertionPointFactory.createInsertionPoint(
					(CompilationUnit) project.findASTRootForICompilationUnit(iCompilationUnit),
					textSelection.getOffset());

			final ASTNode selectedNode = insertionPoint.getParent(); // project.nodeForSelection((ITextSelection)getSelection(),
			// iCompilationUnit);

			final Pasteboard pasteboard = project.getPasteboard();

			if (insertionPoint.isValid()
					&& (selectedNode instanceof Block
							&& pasteboard.containsOnlyNodesOfClass(Statement.class) || selectedNode instanceof TypeDeclaration
							&& pasteboard.containsOnlyNodesOfClass(BodyDeclaration.class))) {

				final ChildListPropertyDescriptor childProperty = insertionPoint.getProperty();

				final int insertIndex = insertionPoint.getIndex();

				final PasteStep pasteStep = new PasteStep(project, selectedNode, childProperty,
						insertIndex);

				pasteStep.applyAllAtOnce();

			} else {
				System.err.println("PMPasteAction must be run a block or a class definition");

				showErrorDialog("PM Paste Error", "Paste must be run a block or a class definition");
			}

		} else {
			System.err.println("PMPasteAction must be run on a text selection.");

			showErrorDialog("PM Paste Error", "PMPasteAction must be run on a text selection.");
		}
	}

}
