package net.creichen.pm.utils.visitors.collectors;

import static net.creichen.pm.utils.Constants.VISIT_CHILDREN;

import org.eclipse.jdt.core.dom.Assignment;

public class AssignmentCollector extends AbstractCollector<Assignment> {

    @Override
    public boolean visit(final Assignment visitedAssignment) {
        addResult(visitedAssignment);
        return VISIT_CHILDREN;
    }
}
