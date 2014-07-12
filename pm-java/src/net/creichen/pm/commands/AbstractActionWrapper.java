package net.creichen.pm.commands;

import net.creichen.pm.actions.Action;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;

abstract class AbstractActionWrapper extends AbstractHandler {

    private Action action;

    protected final Action getAction() {
        return this.action;
    }

    protected ICompilationUnit getCompilationUnit(final IWorkbenchWindow window) {
        final IWorkbenchPage activePage = window.getActivePage();

        if (activePage != null) {
            final IEditorPart editor = activePage.getActiveEditor();
            return (ICompilationUnit) org.eclipse.jdt.ui.JavaUI.getEditorInputJavaElement(editor
                    .getEditorInput());
        } else {
            return null;
        }

    }

    protected final void setAction(final Action action) {
        this.action = action;
    }

}
