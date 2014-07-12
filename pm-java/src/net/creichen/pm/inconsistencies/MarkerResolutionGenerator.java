/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm.inconsistencies;

import net.creichen.pm.PMProject;
import net.creichen.pm.PMWorkspace;

import org.eclipse.core.resources.IMarker;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolutionGenerator;

public class MarkerResolutionGenerator implements IMarkerResolutionGenerator {
    private static final class AcceptBehavioralChangeQuickFix implements IMarkerResolution {
        private final Inconsistency inconsistency;

        private AcceptBehavioralChangeQuickFix(final Inconsistency inconsistency) {
            this.inconsistency = inconsistency;
        }

        @Override
        public String getLabel() {
            return "Accept behavioral change.";
        }

        @Override
        public void run(final IMarker marker) {
            this.inconsistency.acceptBehavioralChange();

        }
    }

    public static final String INCONSISTENCY_ID = "pm-inconsistency-id";

    public static final String PROJECT_ID = "pm-project-id";

    public static final String ACCEPTS_BEHAVIORAL_CHANGE = "pm-accepts-behavioral-change";

    @Override
    public IMarkerResolution[] getResolutions(final IMarker marker) {
        System.out.println("Getting resolutions");

        try {

            final PMProject project = PMWorkspace.sharedWorkspace().projectForIJavaProject(
                    (IJavaProject) JavaCore.create((String) marker.getAttribute(PROJECT_ID)));

            final String inconsistencyID = (String) marker.getAttribute(INCONSISTENCY_ID);

            final Inconsistency inconsistency = project.getInconsistencyWithKey(inconsistencyID);

            final IMarkerResolution[] result = new IMarkerResolution[1];
            result[0] = new AcceptBehavioralChangeQuickFix(inconsistency);

            return result;
        } catch (final Exception e) {
            return new IMarkerResolution[0];
        }
    }

}
