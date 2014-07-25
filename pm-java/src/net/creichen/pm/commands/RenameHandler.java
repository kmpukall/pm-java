package net.creichen.pm.commands;

import net.creichen.pm.PMRenameProcessor;
import net.creichen.pm.RenameInputPage;
import net.creichen.pm.ui.Wizard;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.ltk.ui.refactoring.RefactoringWizardOpenOperation;

public class RenameHandler extends AbstractCommandHandler {

	@Override
	public final void handleEvent(final ExecutionEvent event) {
		final PMRenameProcessor processor = new PMRenameProcessor(
				getSelection(), getCompilationUnit());

		final RefactoringWizardOpenOperation operation = new RefactoringWizardOpenOperation(
				new Wizard(processor, new RenameInputPage(processor)));

		try {
			operation.run(getWindow().getShell(), "PM Rename Title");
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}

}
