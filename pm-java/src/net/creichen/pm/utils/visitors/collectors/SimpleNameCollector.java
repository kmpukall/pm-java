package net.creichen.pm.utils.visitors.collectors;

import org.eclipse.jdt.core.dom.SimpleName;

public class SimpleNameCollector extends AbstractCollector<SimpleName> {

    @Override
    public boolean visit(final SimpleName visitedSimpleName) {
        addResult(visitedSimpleName);
        return true;
    }
}
