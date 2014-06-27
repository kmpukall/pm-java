/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm.actions;

import net.creichen.pm.PMSplitProcessor;

import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor;
import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;

public class PMSplitAction extends PMAction {

    @Override
    public RefactoringProcessor newProcessor() {
        return new PMSplitProcessor((ITextSelection) getSelection(), currentICompilationUnit());
    }

    @Override
    public UserInputWizardPage newWizardInputPage(final RefactoringProcessor processor) {
        return null; // No input page currently needed for split temporary
    }

}
