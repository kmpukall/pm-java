package net.creichen.pm.commands;

import net.creichen.pm.SplitProcessor;
import net.creichen.pm.Wizard;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor;
import org.eclipse.ltk.ui.refactoring.RefactoringWizardOpenOperation;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

public class SplitHandler extends AbstractActionWrapper {

    private ICompilationUnit currentICompilationUnit(final IWorkbenchWindow window) {
        final IWorkbenchPage activePage = window.getActivePage();

        if (activePage != null) {
            final IEditorPart editor = activePage.getActiveEditor();

            return (ICompilationUnit) org.eclipse.jdt.ui.JavaUI.getEditorInputJavaElement(editor
                    .getEditorInput());
        } else {
            return null;
        }

    }

    @Override
    public final Object execute(final ExecutionEvent event) throws ExecutionException {
        final IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindow(event);
        final ISelection selection = HandlerUtil.getCurrentSelection(event);
        if (selection instanceof ITextSelection) {

            final RefactoringProcessor processor = newProcessor(window, (ITextSelection) selection);

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

    private RefactoringProcessor newProcessor(final IWorkbenchWindow window,
            final ITextSelection selection) {
        return new SplitProcessor(selection, currentICompilationUnit(window));
    }

}
