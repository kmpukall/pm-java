package net.creichen.pm.utils.visitors;

import org.eclipse.jdt.core.dom.SimpleName;

public class SimpleNameCollector extends CollectingASTVisitor<SimpleName> {

    @Override
    public boolean visit(final SimpleName visitedSimpleName) {
        addResult(visitedSimpleName);
        return true;
    }
}
