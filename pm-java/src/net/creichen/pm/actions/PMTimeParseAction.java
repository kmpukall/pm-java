/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm.actions;

import net.creichen.pm.PMProject;
import net.creichen.pm.Timer;
import net.creichen.pm.PMWorkspace;

import org.eclipse.jface.action.IAction;
import org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor;
import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;

public class PMTimeParseAction extends Action {

    @Override
    public RefactoringProcessor newProcessor() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public UserInputWizardPage newWizardInputPage(final RefactoringProcessor processor) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void run(final IAction action) {
        final PMProject project = PMWorkspace.sharedWorkspace().projectForIJavaProject(
                currentICompilationUnit().getJavaProject());

        for (int i = 0; i < 10; i++) {

            Timer.sharedTimer().start("JUST_PARSE");

            // project.updateToNewVersionsOfICompilationUnits();

            project.justParseMeasurement(false);

            Timer.sharedTimer().stop("JUST_PARSE");

            final double elapsedSeconds = Timer.sharedTimer().accumulatedSecondsForKey(
                    "JUST_PARSE");

            System.out.println("Time to just parse is " + elapsedSeconds);

            Timer.sharedTimer().clear("JUST_PARSE");
        }

        for (int i = 0; i < 10; i++) {

            Timer.sharedTimer().start("PARSE_BINDINGS");

            // project.updateToNewVersionsOfICompilationUnits();

            project.justParseMeasurement(true);

            Timer.sharedTimer().stop("PARSE_BINDINGS");

            final double elapsedSeconds = Timer.sharedTimer().accumulatedSecondsForKey(
                    "PARSE_BINDINGS");

            System.out.println("Time to just parse with bindings is " + elapsedSeconds);

            Timer.sharedTimer().clear("PARSE_BINDINGS");
        }

        for (int i = 0; i < 10; i++) {

            Timer.sharedTimer().start("PARSE_BINDINGS_UPDATE");

            // project.updateToNewVersionsOfICompilationUnits();

            project.updateToNewVersionsOfICompilationUnits();

            Timer.sharedTimer().stop("PARSE_BINDINGS_UPDATE");

            final double elapsedSeconds = Timer.sharedTimer().accumulatedSecondsForKey(
                    "PARSE_BINDINGS_UPDATE");

            System.out.println("Time parse and update model is " + elapsedSeconds);

            Timer.sharedTimer().clear("PARSE_BINDINGS_UPDATE");

            System.out.println("Model equivalence time is "
                    + Timer.sharedTimer().accumulatedSecondsForKey("INCONSISTENCIES"));

            Timer.sharedTimer().clear("INCONSISTENCIES");

            System.out.println("DU/UD time is "
                    + Timer.sharedTimer().accumulatedSecondsForKey("DUUD_CHAINS"));

            Timer.sharedTimer().clear("DUUD_CHAINS");

            // System.out.println("NODE_REPLACEMENT time is " +
            // PMTimer.sharedTimer().accumulatedSecondsForKey("NODE_REPLACEMENT"));
            // PMTimer.sharedTimer().clear("NODE_REPLACEMENT");

            System.out.println("PARSE_INTERNAL time is "
                    + Timer.sharedTimer().accumulatedSecondsForKey("PARSE_INTERNAL"));
            Timer.sharedTimer().clear("PARSE_INTERNAL");

            // System.out.println("PUT_HASH time is " +
            // PMTimer.sharedTimer().accumulatedSecondsForKey("PUT_HASH"));
            // PMTimer.sharedTimer().clear("PUT_HASH");

            // System.out.println("SUBTREE_BYTES time is " +
            // PMTimer.sharedTimer().accumulatedSecondsForKey("SUBTREE_BYTES"));
            // PMTimer.sharedTimer().clear("SUBTREE_BYTES");
        }

    }
}
