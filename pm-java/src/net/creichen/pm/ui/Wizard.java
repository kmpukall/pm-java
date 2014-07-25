/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm.ui;

import org.eclipse.ltk.core.refactoring.participants.ProcessorBasedRefactoring;
import org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;
import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;

public class Wizard extends RefactoringWizard {

	private final UserInputWizardPage wizardPage;

	public Wizard(final RefactoringProcessor processor, final UserInputWizardPage wizardPage) {
		super(new ProcessorBasedRefactoring(processor), CHECK_INITIAL_CONDITIONS_ON_OPEN);
		this.wizardPage = wizardPage;
	}

	@Override
	protected void addUserInputPages() {
		if (this.wizardPage != null) {
			addPage(this.wizardPage);
		}
	}

	public RefactoringProcessor getProcessor() {
		return ((ProcessorBasedRefactoring) getRefactoring()).getProcessor();
	}

	@Override
	public boolean performCancel() {
		super.performCancel();

		/*
		 * ((PMProcessor)getProcessor()).textChangeWasNotApplied();
		 */

		return true;
	}

	@Override
	public boolean performFinish() {

		final boolean result = super.performFinish();

		/*
		 * if (result) {
		 *
		 * ((PMProcessor)getProcessor()).textChangeWasApplied();
		 *
		 * PMProject project = PMWorkspace.sharedWorkspace().projectForIJavaProject
		 * (((PMProcessor)getProcessor ()).getICompilationUnit().getJavaProject());
		 *
		 * project.updateToNewVersionsOfICompilationUnits();
		 *
		 *
		 * } else { ((PMProcessor)getProcessor()).textChangeWasNotApplied(); }
		 *
		 * System.out.println("STEP time is " +
		 * PMTimer.sharedTimer().accumulatedSecondsForKey("STEP"));
		 * PMTimer.sharedTimer().clear("STEP");
		 */
		return result;
	}
}
