package net.creichen.pm.commands;

import net.creichen.pm.actions.Action;
import net.creichen.pm.actions.PMSplitAction;

public class SplitHandler extends AbstractActionWrapper {

    private final PMSplitAction action = new PMSplitAction();

    @Override
    protected Action getAction() {
        return this.action;
    }

}
