/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm.models;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.jdt.core.dom.SimpleName;

public class Use {

    private final SimpleName name;
    private final Set<Def> reachingDefinitions = new HashSet<Def>();

    public Use(final SimpleName name) {
        this.name = name;
    }

    public void addReachingDefinition(final Def reachingDef) {
        this.reachingDefinitions.add(reachingDef);

        // not sure if we want reachingDef == null to mean unitialized or
        // real reaching def object that is marked as unitialized
        if (reachingDef != null && !reachingDef.getUses().contains(this)) {
            reachingDef.addUse(this);
        }
    }

    public Set<Def> getReachingDefinitions() {
        return this.reachingDefinitions;
    }

    public SimpleName getSimpleName() {
        return this.name;
    }

    @Override
    public String toString() {
        return "Use of '" + this.name + "', " + this.reachingDefinitions.size() + " reaching definitions";
    }
}
