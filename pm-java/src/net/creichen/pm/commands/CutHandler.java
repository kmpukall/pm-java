package net.creichen.pm.commands;

import net.creichen.pm.actions.Action;
import net.creichen.pm.actions.CutAction;

public class CutHandler extends AbstractActionWrapper {

    private final Action action = new CutAction();

    @Override
    protected Action getAction() {
        return this.action;
    }

}
