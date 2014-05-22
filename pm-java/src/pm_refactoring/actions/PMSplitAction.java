/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

*******************************************************************************/

package pm_refactoring.actions;

import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor;
import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;


import pm_refactoring.PMSplitProcessor;

public class PMSplitAction extends PMAction {

	public RefactoringProcessor newProcessor() {
		return new PMSplitProcessor((ITextSelection)getSelection(), currentICompilationUnit());
	}

	public UserInputWizardPage newWizardInputPage(RefactoringProcessor processor) {
		return null; //No input page currently needed for split temporary
	}

}
