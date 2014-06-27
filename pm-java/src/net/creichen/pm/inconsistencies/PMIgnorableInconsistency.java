/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm.inconsistencies;

import net.creichen.pm.PMCompilationUnit;
import net.creichen.pm.PMProject;

import org.eclipse.core.resources.IMarker;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.ui.IMarkerResolution;

public abstract class PMIgnorableInconsistency extends PMInconsistency {

    private class IgnoreMeResolution implements IMarkerResolution {
        @Override
        public String getLabel() {
            return "Accept change";
        }

        @Override
        public void run(final IMarker marker) {
            System.out.println("FIXME");
        }

    }

    public PMIgnorableInconsistency(final PMProject project,
            final PMCompilationUnit compilationUnit, final ASTNode node) {
        super(project, compilationUnit, node);
    }

    // My current guess is that we will keep a set of ignored inconsistencies.
    // This will require us to override hashCode and equals (I'm only overriding
    // equals below).
    // The main problem is that we need to compare AST nodes from different
    // passes... need to figure out
    // how to do that.
    @Override
    public abstract boolean equals(Object o);

    /**
     * Determine all quick fixes for this inconsistency
     * 
     * @return The relevant quick fixes. The default implementation yields a single quick fix that
     *         ignores this particular inconsistency.
     */
    @Override
    public IMarkerResolution[] getQuickFixes() {
        return new IMarkerResolution[] { new IgnoreMeResolution() };
    }
}
