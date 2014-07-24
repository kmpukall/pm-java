package net.creichen.pm.commands;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;

abstract class AbstractActionWrapper extends AbstractHandler {

	private IWorkbenchWindow window;

	protected ICompilationUnit getCompilationUnit() {
		final IWorkbenchPage activePage = window.getActivePage();

		if (activePage != null) {
			final IEditorPart editor = activePage.getActiveEditor();
			return (ICompilationUnit) org.eclipse.jdt.ui.JavaUI
					.getEditorInputJavaElement(editor.getEditorInput());
		} else {
			return null;
		}

	}

	void showErrorDialog(final String dialogTitle, final String errorExplanation) {
		MessageDialog.openError(this.getWindow().getShell(), dialogTitle,
				errorExplanation);
	}

	protected IWorkbenchWindow getWindow() {
		return window;
	}

	protected void setWindow(final IWorkbenchWindow window) {
		this.window = window;
	}

}
