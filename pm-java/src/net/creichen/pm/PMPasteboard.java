/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

*******************************************************************************/

package net.creichen.pm;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;

public class PMPasteboard {

	List<ASTNode> _pasteboardRoots;
	
	PMProject _project;
	
	
	
	public PMPasteboard(PMProject project) {
		_pasteboardRoots = null;
		
		_project = project;
	}
	
	public List<ASTNode> getPasteboardRoots() {
		return _pasteboardRoots;
	}
	
	
	public void setPasteboardRoot(ASTNode root) {
		List<ASTNode> roots = new ArrayList<ASTNode>();
		
		roots.add(root);
		
		setPasteboardRoots(roots);
	}
	
	public void setPasteboardRoots(List<ASTNode> pasteboardRoots) {
		if (_pasteboardRoots != null)
			clearPasteboard();
		
		_pasteboardRoots = pasteboardRoots;
		
		
		
		
		
		
	}
	
	public void clearPasteboard() {
		//clear model information via _project
		
		_pasteboardRoots = null;
	}
	
	
	
	
	public boolean containsOnlyNodesOfClass(Class someClass) {
		for (ASTNode node: _pasteboardRoots) {
			if (!someClass.isInstance(node))
				return false;
		}
		
		return true;
	}
	

	
}
