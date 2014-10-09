package net.creichen.pm.consistency;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.creichen.pm.consistency.inconsistencies.Inconsistency;
import net.creichen.pm.core.PMException;
import net.creichen.pm.core.Project;
import net.creichen.pm.ui.MarkerResolutionGenerator;
import net.creichen.pm.utils.Timer;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.dom.ASTNode;

public class ConsistencyValidator {

    private static ConsistencyValidator instance;

    public static ConsistencyValidator getInstance() {
        if (instance == null) {
            instance = new ConsistencyValidator();
        }
        return instance;
    }

    private final Map<String, Inconsistency> currentInconsistencies;

    public ConsistencyValidator() {
        this.currentInconsistencies = new HashMap<String, Inconsistency>();
    }

    public void rescanForInconsistencies(final Project project) {
        try {
            this.currentInconsistencies.clear();

            Timer.sharedTimer().start("INCONSISTENCIES");

            final Set<Inconsistency> inconsistencySet = new HashSet<Inconsistency>();

            inconsistencySet.addAll(new NameModelConsistencyCheck(project).calculateInconsistencies(project
                    .getNameModel()));
            inconsistencySet.addAll(new DefUseModelConsistencyCheck(project).calculateInconsistencies(project
                    .getUDModel()));

            Timer.sharedTimer().stop("INCONSISTENCIES");

            // delete previous markers
            for (final ICompilationUnit iCompilationUnit : project.getICompilationUnits()) {

                iCompilationUnit.getResource().deleteMarkers("org.eclipse.core.resources.problemmarker", false,
                        IResource.DEPTH_ZERO);
            }

            for (final Inconsistency inconsistency : inconsistencySet) {
                final IResource resource = project.findPMCompilationUnitForNode(inconsistency.getNode())
                        .getICompilationUnit().getResource();
                ConsistencyValidator.createMarker(resource, inconsistency, project.getIJavaProject());
                this.currentInconsistencies.put(inconsistency.getID(), inconsistency);
            }
        } catch (final CoreException e) {
            e.printStackTrace();

            throw new PMException(e);
        }
    }

    public static void createMarker(final IResource resource, final Inconsistency inconsistency,
            final IJavaProject iJavaProject) throws CoreException {
        final IMarker marker = resource.createMarker("org.eclipse.core.resources.problemmarker");

        marker.setAttribute(MarkerResolutionGenerator.INCONSISTENCY_ID, inconsistency.getID());
        marker.setAttribute(MarkerResolutionGenerator.PROJECT_ID, iJavaProject.getHandleIdentifier());

        marker.setAttribute(MarkerResolutionGenerator.ACCEPTS_BEHAVIORAL_CHANGE,
                inconsistency.allowsAcceptBehavioralChange());

        marker.setAttribute(IMarker.MESSAGE, inconsistency.getHumanReadableDescription());
        marker.setAttribute(IMarker.TRANSIENT, true);

        final ASTNode node = inconsistency.getNode();
        marker.setAttribute(IMarker.CHAR_START, node.getStartPosition());
        marker.setAttribute(IMarker.CHAR_END, node.getStartPosition() + node.getLength());
    }

    public Collection<Inconsistency> getInconsistencies() {
        return this.currentInconsistencies.values();
    }

    public Inconsistency getInconsistency(final String key) {
        return this.currentInconsistencies.get(key);
    }

    public void reset() {
        this.currentInconsistencies.clear();
    }

}
