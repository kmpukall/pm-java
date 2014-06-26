package net.creichen.pm.commands;

import net.creichen.pm.actions.PMAction;
import net.creichen.pm.actions.PMDelegateAction;

public class DelegateHandler extends AbstractActionWrapper {

	private PMAction action = new PMDelegateAction();

	protected PMAction getAction() {
		return action;
	}

}
