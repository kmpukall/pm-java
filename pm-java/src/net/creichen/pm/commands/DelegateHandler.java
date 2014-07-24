package net.creichen.pm.commands;

import net.creichen.pm.DelegateInputPage;
import net.creichen.pm.DelegateProcessor;
import net.creichen.pm.Wizard;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ltk.ui.refactoring.RefactoringWizardOpenOperation;
import org.eclipse.ui.handlers.HandlerUtil;

public class DelegateHandler extends AbstractActionWrapper {

	@Override
	public final Object execute(final ExecutionEvent event)
			throws ExecutionException {
		setWindow(HandlerUtil.getActiveWorkbenchWindow(event));
		ISelection selection = HandlerUtil.getCurrentSelection(event);
		if (selection instanceof ITextSelection) {

			final DelegateProcessor processor = new DelegateProcessor(
					(ITextSelection) selection, getCompilationUnit());

			final RefactoringWizardOpenOperation operation = new RefactoringWizardOpenOperation(
					new Wizard(processor, new DelegateInputPage(processor)));

			try {
				operation.run(getWindow().getShell(), "PM Rename Title");
			} catch (final Exception e) {
				e.printStackTrace();
			}
		} else {
			System.err.println("Action must be run on a text selection.");
		}
		return null;
	}
}
