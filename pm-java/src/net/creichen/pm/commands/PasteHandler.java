package net.creichen.pm.commands;

import net.creichen.pm.actions.Action;
import net.creichen.pm.actions.PasteAction;

public class PasteHandler extends AbstractActionWrapper {

    private final Action action = new PasteAction();

    @Override
    protected Action getAction() {
        return this.action;
    }

}
