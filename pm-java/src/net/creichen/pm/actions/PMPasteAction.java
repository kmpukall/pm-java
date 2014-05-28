/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

*******************************************************************************/

package net.creichen.pm.actions;

import java.util.List;








import net.creichen.pm.PMPasteboard;
import net.creichen.pm.PMProject;
import net.creichen.pm.PMWorkspace;
import net.creichen.pm.selection.PMInsertionPoint;
import net.creichen.pm.steps.PMPasteStep;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.ChildListPropertyDescriptor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor;
import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;










public class PMPasteAction extends PMAction {

	@Override
	public RefactoringProcessor newProcessor() {
		
		return null;
	}

	@Override
	public UserInputWizardPage newWizardInputPage(RefactoringProcessor processor) {
		return null;
	}

	protected int insertIndexOfSelectionInList(ITextSelection textSelection, List list) {
		int insertIndex = list.size();
		
		for (int i = 0; i < list.size(); i++) {
			ASTNode child = (ASTNode)list.get(i);
			
			if (textSelection.getOffset() <= child.getStartPosition())
				insertIndex = i;
		}
		
		System.out.println("insertIndex is " + insertIndex);
		
		return insertIndex;
	}
	
	public void run(IAction action) {
		System.err.println("In PMPasteAction run()");
		
		
		if (getSelection() instanceof ITextSelection) {
				
			ITextSelection textSelection = (ITextSelection)getSelection();
			
			ICompilationUnit iCompilationUnit = currentICompilationUnit();
			
			PMProject project = PMWorkspace.sharedWorkspace().projectForIJavaProject(iCompilationUnit.getJavaProject());
			
			
			PMInsertionPoint insertionPoint = new PMInsertionPoint((CompilationUnit)project.findASTRootForICompilationUnit(iCompilationUnit), textSelection.getOffset());
			
			ASTNode selectedNode = insertionPoint.insertionParent();//project.nodeForSelection((ITextSelection)getSelection(), iCompilationUnit);
			
			PMPasteboard pasteboard = project.getPasteboard();
			
			if (insertionPoint.isSaneInsertionPoint() && 
					(selectedNode instanceof Block && pasteboard.containsOnlyNodesOfClass(Statement.class))
					|| (selectedNode instanceof TypeDeclaration && pasteboard.containsOnlyNodesOfClass(BodyDeclaration.class)
					   )
			   ) {
				
				
				ChildListPropertyDescriptor childProperty = insertionPoint.insertionProperty();
				
				
				int insertIndex = insertionPoint.insertionIndex();
				
				
				
				PMPasteStep pasteStep = new PMPasteStep(project, selectedNode, childProperty, insertIndex);
				
				pasteStep.applyAllAtOnce();
				
				
				
			} else {
				System.err.println("PMPasteAction must be run a block or a class definition");
				
				showErrorDialog("PM Paste Error", "Paste must be run a block or a class definition");
			}
			
		} else {		
			System.err.println("PMPasteAction must be run on a text selection.");
			
			showErrorDialog("PM Paste Error", "PMPasteAction must be run on a text selection.");
		}
	}
	
	
}
