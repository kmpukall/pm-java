/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm.actions;

import net.creichen.pm.PMRenameInputPage;
import net.creichen.pm.PMRenameProcessor;

import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor;
import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

/**
 * Our sample action implements workbench action delegate. The action proxy will be created by the
 * workbench and shown in the UI. When the user tries to use the action, this delegate will be
 * created and execution will be delegated to it.
 * 
 * @see IWorkbenchWindowActionDelegate
 */
public class PMRenameAction extends PMAction {

    @Override
    public RefactoringProcessor newProcessor() {
        return new PMRenameProcessor((ITextSelection) getSelection(), currentICompilationUnit());
    }

    @Override
    public UserInputWizardPage newWizardInputPage(final RefactoringProcessor processor) {
        return new PMRenameInputPage((PMRenameProcessor) processor);
    }

}
