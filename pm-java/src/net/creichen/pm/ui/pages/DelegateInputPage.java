/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm.ui.pages;

import net.creichen.pm.refactorings.DelegateProcessor;

public class DelegateInputPage extends AbstractWizardPage {

    private final DelegateProcessor processor;

    public DelegateInputPage(final DelegateProcessor processor) {
        super("PM Delegate Input Page");

        this.processor = processor;
    }

    @Override
    protected String getLabel() {
        return "&Delegate to identifier:";
    }

    @Override
    protected void handleNewInput(final String text) {
        this.processor.setDelegateIdentifier(text);
    }
}
