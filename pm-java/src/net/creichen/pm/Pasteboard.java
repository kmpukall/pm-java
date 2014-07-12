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

public class Pasteboard {

    private List<ASTNode> pasteboardRoots;

    Pasteboard(final PMProject project) {
        this.pasteboardRoots = null;
    }

    private void clearPasteboard() {
        // clear model information via _project

        this.pasteboardRoots = null;
    }

    public boolean containsOnlyNodesOfClass(final Class<?> someClass) {
        for (final ASTNode node : this.pasteboardRoots) {
            if (!someClass.isInstance(node)) {
                return false;
            }
        }

        return true;
    }

    public List<ASTNode> getPasteboardRoots() {
        return this.pasteboardRoots;
    }

    public void setPasteboardRoot(final ASTNode root) {
        final List<ASTNode> roots = new ArrayList<ASTNode>();

        roots.add(root);

        setPasteboardRoots(roots);
    }

    public void setPasteboardRoots(final List<ASTNode> pasteboardRoots) {
        if (this.pasteboardRoots != null) {
            clearPasteboard();
        }

        this.pasteboardRoots = pasteboardRoots;

    }

}
