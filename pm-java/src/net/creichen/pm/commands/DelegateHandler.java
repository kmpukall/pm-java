package net.creichen.pm.commands;

import net.creichen.pm.actions.Action;
import net.creichen.pm.actions.DelegateAction;

public class DelegateHandler extends AbstractActionWrapper {

    private final Action action = new DelegateAction();

    @Override
    protected final Action getAction() {
        return this.action;
    }

}
