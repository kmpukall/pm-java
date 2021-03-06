package net.creichen.pm.commands;

import net.creichen.pm.refactorings.DelegateProcessor;
import net.creichen.pm.ui.Wizard;
import net.creichen.pm.ui.pages.DelegateInputPage;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.ltk.ui.refactoring.RefactoringWizardOpenOperation;

public class DelegateHandler extends AbstractCommandHandler {

    @Override
    public final void handleEvent(final ExecutionEvent event) {
        final DelegateProcessor processor = new DelegateProcessor(getSelection(), getCompilationUnit());

        final RefactoringWizardOpenOperation operation = new RefactoringWizardOpenOperation(new Wizard(processor,
                new DelegateInputPage(processor)));

        try {
            operation.run(getWindow().getShell(), "PM Rename Title");
        } catch (final InterruptedException e) {
            e.printStackTrace();
        }
    }
}
