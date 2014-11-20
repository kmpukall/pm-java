/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm.core;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.core.IJavaProject;

public final class Workspace {
    private static Workspace sharedWorkspace = null;

    private final Map<IJavaProject, Project> projectMapping;

    private Workspace() {
        this.projectMapping = new HashMap<IJavaProject, Project>();
    }

    public Project getProject(final IJavaProject iJavaProject) {
        if (!this.projectMapping.containsKey(iJavaProject)) {
            createProject(iJavaProject);
        }
        return this.projectMapping.get(iJavaProject);
    }

    public void createProject(final IJavaProject javaProject) {
        this.projectMapping.put(javaProject, new Project(javaProject));
    }

    public void removeProject(final IJavaProject iJavaProject) {
        this.projectMapping.remove(iJavaProject);
    }

    public static Workspace getInstance() {
        if (sharedWorkspace == null) {
            sharedWorkspace = new Workspace();
        }
        return sharedWorkspace;
    }

}
