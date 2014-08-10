/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm;

import java.util.HashMap;
import java.util.Map;

import net.creichen.pm.consistency.ConsistencyValidator;
import net.creichen.pm.models.Project;

import org.eclipse.jdt.core.IJavaProject;

public final class Workspace {
    private static Workspace sharedWorkspace = null;

    public static synchronized Workspace sharedWorkspace() {

        if (sharedWorkspace == null) {
            sharedWorkspace = new Workspace();
        }

        return sharedWorkspace;
    }

    private final Map<IJavaProject, Project> projectMapping;

    private Workspace() {
        this.projectMapping = new HashMap<IJavaProject, Project>();

    }

    public synchronized Project projectForIJavaProject(final IJavaProject iJavaProject) {

        Project result = this.projectMapping.get(iJavaProject);

        if (result == null) {
            result = new Project(iJavaProject);
            this.projectMapping.put(iJavaProject, result);
            ConsistencyValidator.getInstance().reset();
        }

        return result;
    }

    public synchronized void removeProjectForIJavaProject(final IJavaProject iJavaProject) {
        this.projectMapping.remove(iJavaProject);
    }

}
