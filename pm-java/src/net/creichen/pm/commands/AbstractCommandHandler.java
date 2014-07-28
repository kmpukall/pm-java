package net.creichen.pm.commands;

import net.creichen.pm.Project;
import net.creichen.pm.Workspace;
import net.creichen.pm.checkers.ConsistencyValidator;

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
    private Project project;
    private ICompilationUnit compilationUnit;

    @Override
    public final Object execute(final ExecutionEvent event) {
        this.window = HandlerUtil.getActiveWorkbenchWindow(event);
        this.compilationUnit = createCompilationUnit();
        this.project = Workspace.sharedWorkspace().projectForIJavaProject(getCompilationUnit().getJavaProject());
        this.project.syncSources();
        ConsistencyValidator.getInstance().reset();

        ISelection currentSelection = HandlerUtil.getCurrentSelection(event);
        if (currentSelection instanceof ITextSelection) {
            this.selection = (ITextSelection) currentSelection;
            handleEvent(event);
        } else {
            showErrorDialog("Error", "This command must be run on a text selection.");
        }
        return null;
    }

    protected ICompilationUnit getCompilationUnit() {
        return this.compilationUnit;
    }

    private ICompilationUnit createCompilationUnit() {
        final IWorkbenchPage activePage = this.window.getActivePage();

        if (activePage != null) {
            final IEditorPart editor = activePage.getActiveEditor();
            return (ICompilationUnit) org.eclipse.jdt.ui.JavaUI.getEditorInputJavaElement(editor.getEditorInput());
        } else {
            return null;
        }
    }

    protected final ITextSelection getSelection() {
        return this.selection;
    }

    protected IWorkbenchWindow getWindow() {
        return this.window;
    }

    protected abstract void handleEvent(ExecutionEvent event);

    protected void showErrorDialog(final String dialogTitle, final String errorExplanation) {
        MessageDialog.openError(getWindow().getShell(), dialogTitle, errorExplanation);
    }

    public Project getProject() {
        return this.project;
    }
}
