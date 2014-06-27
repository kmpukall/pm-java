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

public class PMProcessorBasedRefactoring extends ProcessorBasedRefactoring {

    RefactoringProcessor _processor;

    public PMProcessorBasedRefactoring(RefactoringProcessor processor) {
        super(processor);

        _processor = processor;
    }

    public RefactoringProcessor getProcessor() {

        return _processor;
    }

}
