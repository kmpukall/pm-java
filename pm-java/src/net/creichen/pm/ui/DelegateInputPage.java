/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm.ui;

import net.creichen.pm.refactorings.DelegateProcessor;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public class DelegateInputPage extends UserInputWizardPage {
    private Text fNameField;

    private final DelegateProcessor processor;

    public DelegateInputPage(final DelegateProcessor processor) {
        super("PM Delegate Input Page");

        this.processor = processor;
    }

    @Override
    public void createControl(final Composite parent) {
        final Composite result = new Composite(parent, SWT.NONE);
        setControl(result);

        GridLayout layout = new GridLayout();
        layout.numColumns = 2;
        result.setLayout(layout);

        final Label label = new Label(result, SWT.NONE);

        label.setText("&Delegate to identifier:");

        this.fNameField = createNameField(result);

        final Composite composite = new Composite(result, SWT.NONE);

        layout = new GridLayout();

        layout.marginHeight = 0;
        layout.marginWidth = 0;
        layout.numColumns = 1;

        composite.setLayout(layout);
        composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        this.fNameField.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(final ModifyEvent event) {

                handleInputChanged();

            }

        });

        this.fNameField.setFocus();

        this.fNameField.selectAll();

        handleInputChanged();

    }

    private Text createNameField(final Composite result) {
        final Text field = new Text(result, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
        field.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        return field;
    }

    private void handleInputChanged() {
        this.processor.setDelegateIdentifier(this.fNameField.getText());

        final RefactoringStatus status = new RefactoringStatus();

        setPageComplete(!status.hasError());

        final int severity = status.getSeverity();

        final String message = status.getMessageMatchingSeverity(severity);

        if (severity >= RefactoringStatus.INFO) {

            setMessage(message, severity);

        } else {

            setMessage("", NONE); //$NON-NLS-1$

        }

    }

}
