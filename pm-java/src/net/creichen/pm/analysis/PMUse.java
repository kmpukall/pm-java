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

public class PMUse {

    SimpleName _simpleName;

    Set<PMDef> _reachingDefinitions;

    public PMUse(SimpleName simpleName) {
        _simpleName = simpleName;

        _reachingDefinitions = new HashSet<PMDef>();
    }

    public void addReachingDefinition(PMDef reachingDef) {
        if (!_reachingDefinitions.contains(reachingDef)) {
            _reachingDefinitions.add(reachingDef);

            // not sure if we want reachingDef == null to mean unitialized or
            // real reaching def object that is marked as unitialized
            if (reachingDef != null)
                reachingDef.addUse(this);
        }
    }

    public SimpleName getSimpleName() {
        return _simpleName;
    }

    public Set<PMDef> getReachingDefinitions() {
        return _reachingDefinitions;
    }

}
