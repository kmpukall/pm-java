/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm.actions;

import net.creichen.pm.Wizard;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor;
import org.eclipse.ltk.ui.refactoring.RefactoringWizardOpenOperation;
import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

public abstract class Action implements IWorkbenchWindowActionDelegate {
    private IWorkbenchWindow window;

    private ISelection selection;

    public Action() {

    }

    ICompilationUnit currentICompilationUnit() {
        final IWorkbenchPage activePage = this.window.getActivePage();

        if (activePage != null) {
            final IEditorPart editor = activePage.getActiveEditor();

            return (ICompilationUnit) org.eclipse.jdt.ui.JavaUI.getEditorInputJavaElement(editor
                    .getEditorInput());
        } else {
            return null;
        }

    }

    /**
     * We can use this method to dispose of any system resources we previously allocated.
     * 
     * @see IWorkbenchWindowActionDelegate#dispose
     */
    @Override
    public void dispose() {
    }

    public ISelection getSelection() {

        return this.window.getSelectionService().getSelection(); // Doesn't seem to
        // make a difference
        // in the selection
        // not updated
        // problem
        // return _selection;
    }

    /**
     * We will cache window object in order to be able to provide parent shell for the message
     * dialog.
     * 
     * @see IWorkbenchWindowActionDelegate#init
     */
    @Override
    public final void init(final IWorkbenchWindow window) {
        this.window = window;
    }

    public abstract RefactoringProcessor newProcessor();

    public abstract UserInputWizardPage newWizardInputPage(RefactoringProcessor processor);

    /**
     * The action has been activated. The argument of the method represents the 'real' action
     * sitting in the workbench UI.
     * 
     * @see IWorkbenchWindowActionDelegate#run
     */
    @Override
    public void run(final IAction action) {
        if (this.selection instanceof ITextSelection) {

            final RefactoringProcessor processor = newProcessor();

            final RefactoringWizardOpenOperation operation = new RefactoringWizardOpenOperation(
                    new Wizard(processor, newWizardInputPage(processor)));

            try {
                operation.run(this.window.getShell(), "PM Rename Title");
            } catch (final Exception e) {
                e.printStackTrace();
            }
        } else {
            System.err.println("Action must be run on a text selection.");
        }
    }

    /**
     * Selection in the workbench has been changed. We can change the state of the 'real' action
     * here if we want, but this can only happen after the delegate has been created.
     * 
     * @see IWorkbenchWindowActionDelegate#selectionChanged
     */
    @Override
    public final void selectionChanged(final IAction action, final ISelection selection) {

        // For performance sake, we should stash the selection
        // and only get selected ast node in run()

        // System.err.println("Selection changed");
        this.selection = selection;
    }

    void showErrorDialog(final String dialogTitle, final String errorExplanation) {
        MessageDialog.openError(this.window.getShell(), dialogTitle, errorExplanation);
    }
}
