package net.creichen.pm.commands;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

abstract class AbstractCommandHandler extends AbstractHandler {

	private IWorkbenchWindow window;
	private ITextSelection selection;

	@Override
	public final Object execute(final ExecutionEvent event) {
		this.window = HandlerUtil.getActiveWorkbenchWindow(event);
		ISelection currentSelection = HandlerUtil.getCurrentSelection(event);
		if (currentSelection instanceof ITextSelection) {
			selection = (ITextSelection) currentSelection;
			handleEvent(event);
		} else {
			showErrorDialog("Error",
					"This command must be run on a text selection.");
		}
		return null;
	}

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

	protected final ITextSelection getSelection() {
		return selection;
	}

	protected IWorkbenchWindow getWindow() {
		return window;
	}

	protected abstract void handleEvent(ExecutionEvent event);

	protected void showErrorDialog(final String dialogTitle,
			final String errorExplanation) {
		MessageDialog.openError(this.getWindow().getShell(), dialogTitle,
				errorExplanation);
	}

}
