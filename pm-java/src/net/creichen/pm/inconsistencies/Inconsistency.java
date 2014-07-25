/******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm.inconsistencies;

import net.creichen.pm.api.PMCompilationUnit;
import net.creichen.pm.api.PMProject;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.ui.IMarkerResolution;

public class Inconsistency {
    private final PMProject project;
    private final ASTNode node;
    private final String id;

    Inconsistency(final PMProject project, final PMCompilationUnit pmCompilationUnit,
            final ASTNode node) {
        this.project = project;
        this.node = node;
        this.id = java.util.UUID.randomUUID().toString();
    }

    public void acceptBehavioralChange() {
        throw new RuntimeException("Un-implemented acceptBehavioralChange()");
    }

    public boolean allowsAcceptBehavioralChange() {
        return false;
    }

    public String getHumanReadableDescription() {
        return "Unknown inconsistency for " + this.node.getClass();
    }

    public String getID() {
        return this.id;
    }

    public ASTNode getNode() {
        return this.node;
    }

    protected PMProject getProject() {
        return this.project;
    }

    /**
     * Determine all quick fixes for this inconsistency.
     * 
     * @return The relevant quick fixes. The default implementation returns none.
     */
    public IMarkerResolution[] getQuickFixes() {
        return new IMarkerResolution[0];
    }

    @Override
    public String toString() {
        return getHumanReadableDescription();
    }

}
