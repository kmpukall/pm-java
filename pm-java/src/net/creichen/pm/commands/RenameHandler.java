package net.creichen.pm.commands;

import net.creichen.pm.actions.Action;
import net.creichen.pm.actions.RenameAction;

public class RenameHandler extends AbstractActionWrapper {

    private Action action = new RenameAction();

    protected Action getAction() {
        return action;
    }

}
