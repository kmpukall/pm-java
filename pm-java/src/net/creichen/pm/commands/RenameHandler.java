package net.creichen.pm.commands;

import net.creichen.pm.actions.PMAction;
import net.creichen.pm.actions.PMRenameAction;

public class RenameHandler extends AbstractActionWrapper {

    private PMAction action = new PMRenameAction();

    protected PMAction getAction() {
        return action;
    }

}
