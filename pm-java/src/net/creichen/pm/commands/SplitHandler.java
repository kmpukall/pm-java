package net.creichen.pm.commands;

import net.creichen.pm.SplitProcessor;
import net.creichen.pm.Wizard;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor;
import org.eclipse.ltk.ui.refactoring.RefactoringWizardOpenOperation;

public class SplitHandler extends AbstractCommandHandler {

	@Override
	public final void handleEvent(final ExecutionEvent event) {
		final RefactoringProcessor processor = new SplitProcessor(
				getSelection(), getCompilationUnit());

		final RefactoringWizardOpenOperation operation = new RefactoringWizardOpenOperation(
				new Wizard(processor, null));

		try {
			operation.run(getWindow().getShell(), "PM Rename Title");
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}

}
