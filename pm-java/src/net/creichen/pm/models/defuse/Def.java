/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm.models.defuse;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTNode;

public class Def {

    private final ASTNode definingNode;

    private final Set<Use> uses = new HashSet<Use>();

    public Def(final ASTNode definingNode) {
        this.definingNode = definingNode;
    }

    public void addUse(final Use use) {
        this.uses.add(use);
    }

    public ASTNode getDefiningNode() {
        return this.definingNode;
    }

    public Set<Use> getUses() {
        return this.uses;
    }

    @Override
    public String toString() {
        return "Definition at: " + this.definingNode + " [ " + this.uses.size() + " uses]";
    }
}
