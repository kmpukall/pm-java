package net.creichen.pm.commands;

import net.creichen.pm.actions.Action;
import net.creichen.pm.actions.DelegateAction;

public class DelegateHandler extends AbstractActionWrapper {

    private Action action = new DelegateAction();

    protected Action getAction() {
        return action;
    }

}
