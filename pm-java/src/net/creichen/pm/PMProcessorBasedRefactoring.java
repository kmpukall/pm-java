/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm;

import org.eclipse.ltk.core.refactoring.participants.ProcessorBasedRefactoring;
import org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor;

//Needed because ProcessorBasedRefactoring is abstract and stupid

class PMProcessorBasedRefactoring extends ProcessorBasedRefactoring {

    private final RefactoringProcessor processor;

    public PMProcessorBasedRefactoring(final RefactoringProcessor processor) {
        super(processor);
        this.processor = processor;
    }

    @Override
    public RefactoringProcessor getProcessor() {
        return this.processor;
    }

}
