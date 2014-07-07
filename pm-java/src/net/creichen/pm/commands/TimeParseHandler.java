package net.creichen.pm.commands;

import net.creichen.pm.actions.Action;
import net.creichen.pm.actions.PMTimeParseAction;

public class TimeParseHandler extends AbstractActionWrapper {

    private final Action action = new PMTimeParseAction();

    @Override
    protected final Action getAction() {
        return this.action;
    }

}
