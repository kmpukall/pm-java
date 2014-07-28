/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm.utils;

import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;

public class Pasteboard {

    private static Pasteboard instance;

    public static Pasteboard getInstance() {
        if (instance == null) {
            instance = new Pasteboard();
        }
        return instance;
    }

    private List<ASTNode> pasteboardRoots;

    public Pasteboard() {
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

    public void setPasteboardRoots(final List<ASTNode> pasteboardRoots) {
        this.pasteboardRoots = pasteboardRoots;

    }

}
