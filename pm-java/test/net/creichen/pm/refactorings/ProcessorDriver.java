/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm.refactorings;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor;

public final class ProcessorDriver {

    static RefactoringStatus drive(final RefactoringProcessor eclipseProcessor) {

        final IProgressMonitor pm = new NullProgressMonitor();

        RefactoringStatus status = new RefactoringStatus();

        try {

            status = eclipseProcessor.checkInitialConditions(pm); // technically,
                                                                  // this
                                                                  // should be
                                                                  // called
                                                                  // before
                                                                  // drive()
                                                                  // is even
                                                                  // called

            if (status.getSeverity() < RefactoringStatus.ERROR) {
                final Change change = eclipseProcessor.createChange(pm);

                change.perform(pm);

            } else {

                System.err.println("Error in inital conditions for "
                        + eclipseProcessor.getClass().getName() + " : " + status);
            }

        } catch (final Exception e) {
            e.printStackTrace();
        }

        return status;
    }

    private ProcessorDriver() {
        // private utility class constructor
    }
}
