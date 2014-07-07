package net.creichen.pm.commands;

import net.creichen.pm.actions.Action;

import org.eclipse.core.commands.AbstractHandler;

public abstract class AbstractActionWrapper extends AbstractHandler {

    private Action action;

    protected final Action getAction() {
        return this.action;
    }

    protected final void setAction(final Action action) {
        this.action = action;
    }

}
