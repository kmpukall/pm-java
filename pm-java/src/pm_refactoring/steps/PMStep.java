/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

*******************************************************************************/

package pm_refactoring.steps;

import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.NullChange;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.text.edits.TextEdit;


import pm_refactoring.PMProject;
import pm_refactoring.PMWorkspace;


public class PMStep {
	PMProject _project;
	
	PMStep(PMProject project) {
		_project = project;
	}
	
	
	//need method to test for errors before asking for changes
	
	public Map<ICompilationUnit, ASTRewrite> calculateTextualChange() {
		return null;
	}
	
	
	public void performASTChange() {
		
	}
	
	public void updateAfterReparse() {
		
	}
	
	
	public void cleanup() {
		//called regardless of whether updateAfterReparse() was called
	}
	
	
	public void applyAllAtOnce() {
		
		
		
		Map<ICompilationUnit, ASTRewrite> rewrites = calculateTextualChange();
		
		for (ICompilationUnit compilationUnitToRewrite : rewrites.keySet()) {
			PMWorkspace.applyRewrite(rewrites.get(compilationUnitToRewrite), compilationUnitToRewrite);
		}
		
		
		performASTChange();
		
		_project.updateToNewVersionsOfICompilationUnits();
		
		updateAfterReparse();
		
		cleanup();
		
		_project.rescanForInconsistencies();
	}
	
	public Change createCompositeChange(String changeDescription) {
		
		Map<ICompilationUnit, ASTRewrite> rewrites = calculateTextualChange();
		
		Change result = new NullChange();
		
		try {			
			if (rewrites.size() > 0) {				 
				 CompositeChange combinedChange = new PMCompositeChange(changeDescription);
	
				 for (ICompilationUnit compilationUnitToChange: rewrites.keySet()) {
					 ASTRewrite rewrite = rewrites.get(compilationUnitToChange);				 
			 
					TextEdit astEdit = rewrite.rewriteAST();
			
					TextFileChange localChange = new PMTextFileChange(changeDescription, (IFile)compilationUnitToChange.getResource());
					
					localChange.setTextType("java");
					localChange.setEdit(astEdit);
					
					combinedChange.add(localChange);										
				 }
	
				 
				 result = combinedChange;
			}
		
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return result;
	}
	
	public class PMCompositeChange extends CompositeChange {
		public PMCompositeChange(String name) {
			super(name);
		}
		
		public Change perform(IProgressMonitor pm) throws CoreException {
			
			
			
 
			Change result = super.perform(pm);
			
			performASTChange();
			
			_project.updateToNewVersionsOfICompilationUnits();
			
			updateAfterReparse();
			
			cleanup();
			
			_project.rescanForInconsistencies();
			
			return result;
		}
		
	}
	
	public class PMTextFileChange extends TextFileChange {
		public PMTextFileChange(String name, IFile file) {
			super(name, file);
		}
		
		
		//This will do the text change but not the ast changes
		
		public Change perform(IProgressMonitor pm) throws CoreException {
			
		
			
			//In future, we might as well do the text replacement parts of the
			//change ourselves, too (since this will make sure that we do the
			//same thing in all situations), but for now we let the superclass do it
			Change result = super.perform(pm);
			

			
			return result;
		}
		
	}
}
