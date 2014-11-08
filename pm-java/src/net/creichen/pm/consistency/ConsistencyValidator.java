package net.creichen.pm.consistency;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.creichen.pm.consistency.inconsistencies.Inconsistency;
import net.creichen.pm.core.PMException;
import net.creichen.pm.core.Project;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;

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

            final Set<Inconsistency> inconsistencySet = new HashSet<Inconsistency>();

            inconsistencySet.addAll(new NameModelConsistencyCheck(project).calculateInconsistencies(project
                    .getNameModel()));
            inconsistencySet.addAll(new DefUseModelConsistencyCheck(project).calculateInconsistencies(project
                    .getUDModel()));

            // delete previous markers
            for (final ICompilationUnit iCompilationUnit : project.getICompilationUnits()) {

                iCompilationUnit.getResource().deleteMarkers("org.eclipse.core.resources.problemmarker", false,
                        IResource.DEPTH_ZERO);
            }

            for (final Inconsistency inconsistency : inconsistencySet) {
                project.findPMCompilationUnitForNode(inconsistency.getNode()).createMarker(inconsistency,
                        project.getIJavaProject());
                this.currentInconsistencies.put(inconsistency.getID(), inconsistency);
            }
        } catch (final CoreException e) {
            e.printStackTrace();

            throw new PMException(e);
        }
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
