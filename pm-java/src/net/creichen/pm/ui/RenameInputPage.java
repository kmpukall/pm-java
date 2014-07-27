/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm.ui;

import net.creichen.pm.refactorings.PMRenameProcessor;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;

public class RenameInputPage extends AbstractWizardPage {

    private final PMRenameProcessor processor;

    public RenameInputPage(final PMRenameProcessor processor) {
        super("PM Refactoring Input Page");

        this.processor = processor;
    }

    @Override
    protected String getLabel() {
        return "&New name:";
    }

    @Override
    protected void handleInputChanged() {
        this.processor.setNewName(this.getTextField().getText());
        final RefactoringStatus status = new RefactoringStatus();
        setPageComplete(!status.hasError());
        final int severity = status.getSeverity();
        final String message = status.getMessageMatchingSeverity(severity);
        if (severity >= RefactoringStatus.INFO) {
            setMessage(message, severity);
        } else {
            setMessage("", NONE);
        }
    }
}
