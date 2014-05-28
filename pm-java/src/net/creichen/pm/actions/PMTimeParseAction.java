/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

*******************************************************************************/

package net.creichen.pm.actions;


import net.creichen.pm.PMProject;
import net.creichen.pm.PMTimer;
import net.creichen.pm.PMWorkspace;

import org.eclipse.jface.action.IAction;
import org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor;
import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;

public class PMTimeParseAction extends PMAction {

	@Override
	public RefactoringProcessor newProcessor() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public UserInputWizardPage newWizardInputPage(RefactoringProcessor processor) {
		// TODO Auto-generated method stub
		return null;
	}

	public void run(IAction action) {
		PMProject project = PMWorkspace.sharedWorkspace().projectForIJavaProject(currentICompilationUnit().getJavaProject());
	
		for (int i = 0; i < 10; i++) {
			
			PMTimer.sharedTimer().start("JUST_PARSE");
			
			//project.updateToNewVersionsOfICompilationUnits();
			
			project.justParseMeasurement(false);
			
			
			PMTimer.sharedTimer().stop("JUST_PARSE");
			
			double elapsedSeconds = PMTimer.sharedTimer().accumulatedSecondsForKey("JUST_PARSE");
			
			
			System.out.println("Time to just parse is " + elapsedSeconds);
			
			PMTimer.sharedTimer().clear("JUST_PARSE");
		}
		
		for (int i = 0; i < 10; i++) {
			
			PMTimer.sharedTimer().start("PARSE_BINDINGS");
			
			//project.updateToNewVersionsOfICompilationUnits();
			
			project.justParseMeasurement(true);
			
			
		
			PMTimer.sharedTimer().stop("PARSE_BINDINGS");
			
			double elapsedSeconds = PMTimer.sharedTimer().accumulatedSecondsForKey("PARSE_BINDINGS");
			
			System.out.println("Time to just parse with bindings is " + elapsedSeconds);
			
			PMTimer.sharedTimer().clear("PARSE_BINDINGS");
		}
		
		
		
		for (int i = 0; i < 10; i++) {
			
			PMTimer.sharedTimer().start("PARSE_BINDINGS_UPDATE");
			
			//project.updateToNewVersionsOfICompilationUnits();
			
			project.updateToNewVersionsOfICompilationUnits();
			
			
			PMTimer.sharedTimer().stop("PARSE_BINDINGS_UPDATE");
			
			double elapsedSeconds = PMTimer.sharedTimer().accumulatedSecondsForKey("PARSE_BINDINGS_UPDATE");
			
			System.out.println("Time parse and update model is " + elapsedSeconds);
			
			PMTimer.sharedTimer().clear("PARSE_BINDINGS_UPDATE");
			
			System.out.println("Model equivalence time is " + PMTimer.sharedTimer().accumulatedSecondsForKey("INCONSISTENCIES"));
			
			PMTimer.sharedTimer().clear("INCONSISTENCIES");
			
			System.out.println("DU/UD time is " + PMTimer.sharedTimer().accumulatedSecondsForKey("DUUD_CHAINS"));

			PMTimer.sharedTimer().clear("DUUD_CHAINS");
			
			//System.out.println("NODE_REPLACEMENT time is " + PMTimer.sharedTimer().accumulatedSecondsForKey("NODE_REPLACEMENT"));
			//PMTimer.sharedTimer().clear("NODE_REPLACEMENT");
			
			System.out.println("PARSE_INTERNAL time is " + PMTimer.sharedTimer().accumulatedSecondsForKey("PARSE_INTERNAL"));
			PMTimer.sharedTimer().clear("PARSE_INTERNAL");
			
			//System.out.println("PUT_HASH time is " + PMTimer.sharedTimer().accumulatedSecondsForKey("PUT_HASH"));
			//PMTimer.sharedTimer().clear("PUT_HASH");
			
			//System.out.println("SUBTREE_BYTES time is " + PMTimer.sharedTimer().accumulatedSecondsForKey("SUBTREE_BYTES"));
			//PMTimer.sharedTimer().clear("SUBTREE_BYTES");
		}
		
		
	}
}
