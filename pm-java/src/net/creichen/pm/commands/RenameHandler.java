package net.creichen.pm.commands;

import net.creichen.pm.actions.Action;
import net.creichen.pm.actions.RenameAction;

public class RenameHandler extends AbstractActionWrapper {

    private final Action action = new RenameAction();

    @Override
    protected final Action getAction() {
        return this.action;
    }

}
