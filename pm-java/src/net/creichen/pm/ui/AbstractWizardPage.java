package net.creichen.pm.ui;

import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public abstract class AbstractWizardPage extends UserInputWizardPage {

    public AbstractWizardPage(final String name) {
        super(name);
    }

    protected Text createNameField(final Composite result, final String labelText) {
        final Label label = new Label(result, SWT.NONE);
        label.setText(labelText);
        final Text field = new Text(result, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
        field.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        return field;
    }

}
