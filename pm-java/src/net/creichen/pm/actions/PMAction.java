/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

*******************************************************************************/

package net.creichen.pm.actions;

import net.creichen.pm.PMWizard;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor;
import org.eclipse.ltk.ui.refactoring.RefactoringWizardOpenOperation;
import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.texteditor.ITextEditor;

abstract public class PMAction implements IWorkbenchWindowActionDelegate{
	protected IWorkbenchWindow window;
	
	private ISelection _selection;
	
	

	public PMAction() {
		
	}
	
	public ICompilationUnit currentICompilationUnit() {
		IWorkbenchPage activePage = window.getActivePage();

		if (activePage != null) {
			IEditorPart editor = activePage.getActiveEditor();
			
			return (ICompilationUnit)org.eclipse.jdt.ui.JavaUI.getEditorInputJavaElement(editor.getEditorInput());
		} else
			return null;
	
	}
	
	public IDocument currentIDocument() {
		IWorkbenchPage activePage = window.getActivePage();

		if (activePage != null) {
			IEditorPart editor = activePage.getActiveEditor();
			
			IDocument document=(((ITextEditor)editor).getDocumentProvider()).getDocument(editor.getEditorInput());
			
			return document;
			
		} else
			return null;
	
	}
	
	
	
	public ISelection getSelection() {
		
		return window.getSelectionService().getSelection(); //Doesn't seem to make a difference in the selection not updated problem
		//return _selection;
	}
	
	abstract public RefactoringProcessor newProcessor();
	

	abstract public UserInputWizardPage newWizardInputPage(RefactoringProcessor processor);
	
	/**
	 * The action has been activated. The argument of the
	 * method represents the 'real' action sitting
	 * in the workbench UI.
	 * @see IWorkbenchWindowActionDelegate#run
	 */
	public void run(IAction action) {
		if (_selection instanceof ITextSelection) {
					
			RefactoringProcessor processor = newProcessor();
			
			RefactoringWizardOpenOperation operation= new RefactoringWizardOpenOperation(new PMWizard(processor, newWizardInputPage(processor)));

			try {
				operation.run(window.getShell(), "PM Rename Title");
			} catch (Exception e) {
				e.printStackTrace();
			}	
		} else {		
			System.err.println("Action must be run on a text selection.");
		}
	}
	/**
	 * Selection in the workbench has been changed. We 
	 * can change the state of the 'real' action here
	 * if we want, but this can only happen after 
	 * the delegate has been created.
	 * @see IWorkbenchWindowActionDelegate#selectionChanged
	 */
	public void selectionChanged(IAction action, ISelection selection) {
		
		//For performance sake, we should  stash the selection
		//and only get selected ast node in run()
		
		//System.err.println("Selection changed");
		_selection = selection;	
	}
	


	/**
	 * We can use this method to dispose of any system
	 * resources we previously allocated.
	 * @see IWorkbenchWindowActionDelegate#dispose
	 */
	public void dispose() {
	}

	/**
	 * We will cache window object in order to
	 * be able to provide parent shell for the message dialog.
	 * @see IWorkbenchWindowActionDelegate#init
	 */
	public void init(IWorkbenchWindow window) {
		this.window = window;
	}
	
	
	public void showErrorDialog(String dialogTitle, String errorExplanation) {		
		MessageDialog.openError(window.getShell(), dialogTitle, errorExplanation);
	}
}
