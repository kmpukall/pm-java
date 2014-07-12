package net.creichen.pm.commands;

import net.creichen.pm.SplitProcessor;
import net.creichen.pm.Wizard;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor;
import org.eclipse.ltk.ui.refactoring.RefactoringWizardOpenOperation;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

public class SplitHandler extends AbstractActionWrapper {

    @Override
    public final Object execute(final ExecutionEvent event) throws ExecutionException {
        final ISelection selection = HandlerUtil.getCurrentSelection(event);
        if (selection instanceof ITextSelection) {

            final IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindow(event);
            final RefactoringProcessor processor = new SplitProcessor((ITextSelection) selection,
                    getCompilationUnit(window));

            final RefactoringWizardOpenOperation operation = new RefactoringWizardOpenOperation(
                    new Wizard(processor, null));

            try {
                operation.run(window.getShell(), "PM Rename Title");
            } catch (final Exception e) {
                e.printStackTrace();
            }
        } else {
            System.err.println("Action must be run on a text selection.");
        }
        return null;
    }

}
