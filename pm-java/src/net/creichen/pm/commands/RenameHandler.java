package net.creichen.pm.commands;

import net.creichen.pm.refactorings.PMRenameProcessor;
import net.creichen.pm.ui.Wizard;
import net.creichen.pm.ui.pages.RenameInputPage;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.ltk.ui.refactoring.RefactoringWizardOpenOperation;

public class RenameHandler extends AbstractCommandHandler {

    @Override
    public final void handleEvent(final ExecutionEvent event) {
        final PMRenameProcessor processor = new PMRenameProcessor(getSelection(), getCompilationUnit());

        final RefactoringWizardOpenOperation operation = new RefactoringWizardOpenOperation(new Wizard(processor,
                new RenameInputPage(processor)));

        try {
            operation.run(getWindow().getShell(), "PM Rename Title");
        } catch (final InterruptedException e) {
            e.printStackTrace();
        }
    }

}
