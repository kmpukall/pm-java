/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm.inconsistencies;

import net.creichen.pm.PMCompilationUnit;
import net.creichen.pm.PMProject;

import org.eclipse.jdt.core.dom.ASTNode;

public class MissingDefinition extends Inconsistency {
    private final ASTNode definingNode;

    public MissingDefinition(final PMProject project, final PMCompilationUnit iCompilationUnit,
            final ASTNode usingNode, final ASTNode definingNode) {
        super(project, iCompilationUnit, usingNode);

        this.definingNode = definingNode;
    }

    public ASTNode getDefiningNode() {
        return this.definingNode;
    }

    @Override
    public String getHumanReadableDescription() {
        return "Definition (" + this.definingNode + ") should be used by " + getNode();
    }
}
