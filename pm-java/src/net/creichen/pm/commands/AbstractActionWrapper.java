package net.creichen.pm.commands;

import net.creichen.pm.actions.Action;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.handlers.HandlerUtil;

public abstract class AbstractActionWrapper extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        getAction().init(HandlerUtil.getActiveWorkbenchWindow(event));
        getAction().selectionChanged(null, HandlerUtil.getCurrentSelection(event));
        getAction().run(null);
        return null;
    }

    protected abstract Action getAction();

}
