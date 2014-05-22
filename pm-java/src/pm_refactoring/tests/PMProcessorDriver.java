/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

*******************************************************************************/

package pm_refactoring.tests;



import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor;


public class PMProcessorDriver {

	static RefactoringStatus drive(RefactoringProcessor eclipseProcessor) {
		
		
		IProgressMonitor pm = new NullProgressMonitor();
		
		RefactoringStatus status = new RefactoringStatus();
		
		try {
			
			status = eclipseProcessor.checkInitialConditions(pm); //technically, this should be called before drive() is even called
			
			if (status.getSeverity() < RefactoringStatus.ERROR) {
				Change change = eclipseProcessor.createChange(pm);
				
				change.perform(pm);
				
				
				
			} else {
				
				System.err.println("Error in inital conditions for " + eclipseProcessor.getClass().getName() + " : " + status);
			}
			
			
		} catch (Exception e) {
			e.printStackTrace();		
		}
		
		return status;
	}
}
