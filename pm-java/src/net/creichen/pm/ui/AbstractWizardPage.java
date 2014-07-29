package net.creichen.pm.ui;

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

public abstract class AbstractWizardPage extends UserInputWizardPage {

    private Text fNameField;

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

    @Override
    public void createControl(final Composite parent) {

        final Composite result = new Composite(parent, SWT.NONE);
        setControl(result);
        GridLayout layout = new GridLayout();
        layout.numColumns = 2;
        this.fNameField = createNameField(result, getLabel());
        result.setLayout(layout);

        final Composite composite = new Composite(result, SWT.NONE);
        GridLayout compositeLayout = new GridLayout();
        compositeLayout.marginHeight = 0;
        compositeLayout.marginWidth = 0;
        compositeLayout.numColumns = 1;
        composite.setLayout(compositeLayout);
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

    protected abstract String getLabel();

    protected Text getTextField() {
        return this.fNameField;
    }

    protected void handleInputChanged() {
        String text = getTextField().getText();
        handleNewInput(text);
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

    protected abstract void handleNewInput(String text);

}
