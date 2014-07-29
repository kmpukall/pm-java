/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm.ui;

import net.creichen.pm.refactorings.PMRenameProcessor;

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

    protected void handleNewInput(String text) {
        this.processor.setNewName(text);
    }
}
