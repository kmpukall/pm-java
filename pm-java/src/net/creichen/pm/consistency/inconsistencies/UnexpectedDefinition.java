/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm.consistency.inconsistencies;

import org.eclipse.jdt.core.dom.ASTNode;

public class UnexpectedDefinition extends Inconsistency {
    private final ASTNode definingNode;

    public UnexpectedDefinition(final ASTNode usingNode, final ASTNode definingNode) {
        super(usingNode);
        this.definingNode = definingNode;
    }

    @Override
    public String getHumanReadableDescription() {
        return "Unexpected definition (" + this.definingNode + ") used by " + getNode();
    }
}
