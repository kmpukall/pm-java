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
import org.eclipse.ui.IMarkerResolution;

public class PMInconsistency {
    protected PMProject _project;
    protected PMCompilationUnit _pmCompilationUnit;
    protected ASTNode _node;
    protected String _id;

    public PMInconsistency(PMProject project, PMCompilationUnit pmCompilationUnit, ASTNode node) {
        _project = project;
        _pmCompilationUnit = pmCompilationUnit;
        _node = node;
        _id = java.util.UUID.randomUUID().toString();
    }

    public String getHumanReadableDescription() {
        return "Unknown inconsistency for " + _node.getClass();
    }

    public ASTNode getNode() {
        return _node;
    }

    public String toString() {
        return getHumanReadableDescription();
    }

    /**
     * Determine all quick fixes for this inconsistency
     * 
     * @return The relevant quick fixes. The default implementation returns none.
     */
    public IMarkerResolution[] getQuickFixes() {
        return new IMarkerResolution[0];
    }

    public String getID() {
        return this._id;
    }

    public boolean allowsAcceptBehavioralChange() {
        return false;
    }

    public void acceptBehavioralChange() {
        throw new RuntimeException("Un-implemented acceptBehavioralChange()");
    }

}
