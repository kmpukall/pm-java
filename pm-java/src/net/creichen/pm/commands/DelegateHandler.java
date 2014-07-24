package net.creichen.pm.commands;

import net.creichen.pm.DelegateInputPage;
import net.creichen.pm.DelegateProcessor;
import net.creichen.pm.Wizard;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.ltk.ui.refactoring.RefactoringWizardOpenOperation;

public class DelegateHandler extends AbstractCommandHandler {

	@Override
	public final void handleEvent(final ExecutionEvent event) {
		final DelegateProcessor processor = new DelegateProcessor(
				getSelection(), getCompilationUnit());

		final RefactoringWizardOpenOperation operation = new RefactoringWizardOpenOperation(
				new Wizard(processor, new DelegateInputPage(processor)));

		try {
			operation.run(getWindow().getShell(), "PM Rename Title");
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}
}
