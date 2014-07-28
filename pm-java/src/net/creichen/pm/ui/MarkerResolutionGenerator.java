/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm.ui;

import net.creichen.pm.Workspace;
import net.creichen.pm.checkers.ConsistencyValidator;
import net.creichen.pm.inconsistencies.Inconsistency;
import net.creichen.pm.models.Project;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolutionGenerator;

public class MarkerResolutionGenerator implements IMarkerResolutionGenerator {
    private static final class AcceptBehavioralChangeQuickFix implements IMarkerResolution {
        private final Inconsistency inconsistency;
        private Project project;

        private AcceptBehavioralChangeQuickFix(final Inconsistency inconsistency, Project project) {
            this.inconsistency = inconsistency;
            this.project = project;
        }

        @Override
        public String getLabel() {
            return "Accept behavioral change.";
        }

        @Override
        public void run(final IMarker marker) {
            this.inconsistency.acceptBehavioralChange();
            ConsistencyValidator.getInstance().rescanForInconsistencies(this.project);
        }
    }

    public static final String INCONSISTENCY_ID = "pm-inconsistency-id";

    public static final String PROJECT_ID = "pm-project-id";

    public static final String ACCEPTS_BEHAVIORAL_CHANGE = "pm-accepts-behavioral-change";

    @Override
    public IMarkerResolution[] getResolutions(final IMarker marker) {
        System.out.println("Getting resolutions");
        try {
            final Project project = Workspace.sharedWorkspace().projectForIJavaProject(
                    (IJavaProject) JavaCore.create((String) marker.getAttribute(PROJECT_ID)));
            final String inconsistencyID = (String) marker.getAttribute(INCONSISTENCY_ID);
            final Inconsistency inconsistency = ConsistencyValidator.getInstance().getInconsistency(inconsistencyID);
            final IMarkerResolution[] result = new IMarkerResolution[1];
            result[0] = new AcceptBehavioralChangeQuickFix(inconsistency, project);
            return result;
        } catch (final CoreException e) {
            return new IMarkerResolution[0];
        }
    }

}
