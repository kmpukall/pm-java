package net.creichen.pm.utils.visitors;

import org.eclipse.jdt.core.dom.Assignment;

public class AssignmentCollector extends CollectingASTVisitor<Assignment> {

    @Override
    public boolean visit(final Assignment visitedAssignment) {
        addResult(visitedAssignment);
        return true;
    }
}
