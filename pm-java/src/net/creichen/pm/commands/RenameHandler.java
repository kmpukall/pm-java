package net.creichen.pm.commands;

import net.creichen.pm.actions.PMAction;
import net.creichen.pm.actions.PMRenameAction;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.handlers.HandlerUtil;

public class RenameHandler extends AbstractHandler {

	private PMAction renameAction = new PMRenameAction();

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		renameAction.init(HandlerUtil.getActiveWorkbenchWindow(event));
		renameAction.selectionChanged(null,
				HandlerUtil.getCurrentSelection(event));
		renameAction.run(null);
		return null;
	}

}
