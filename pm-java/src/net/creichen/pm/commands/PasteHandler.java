package net.creichen.pm.commands;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.handlers.HandlerUtil;

import net.creichen.pm.actions.PasteAction;

public class PasteHandler extends AbstractActionWrapper {

    public PasteHandler() {
        setAction(new PasteAction());
    }

    @Override
    public final Object execute(final ExecutionEvent event) throws ExecutionException {
        this.getAction().init(HandlerUtil.getActiveWorkbenchWindow(event));
        this.getAction().selectionChanged(null, HandlerUtil.getCurrentSelection(event));
        this.getAction().run(null);
        return null;
    }

}
