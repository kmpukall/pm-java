/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm.analysis;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.jdt.core.dom.SimpleName;

public class Use {

    private final SimpleName simpleName;

    private final Set<Def> reachingDefinitions;

    public Use(final SimpleName simpleName) {
        this.simpleName = simpleName;

        this.reachingDefinitions = new HashSet<Def>();
    }

    public void addReachingDefinition(final Def reachingDef) {
        if (!this.reachingDefinitions.contains(reachingDef)) {
            this.reachingDefinitions.add(reachingDef);

            // not sure if we want reachingDef == null to mean unitialized or
            // real reaching def object that is marked as unitialized
            if (reachingDef != null) {
                reachingDef.addUse(this);
            }
        }
    }

    public Set<Def> getReachingDefinitions() {
        return this.reachingDefinitions;
    }

    public SimpleName getSimpleName() {
        return this.simpleName;
    }

}
