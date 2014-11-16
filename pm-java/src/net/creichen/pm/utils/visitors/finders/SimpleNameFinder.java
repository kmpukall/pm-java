package net.creichen.pm.utils.visitors.finders;

import static net.creichen.pm.utils.Constants.VISIT_CHILDREN;

import org.eclipse.jdt.core.dom.SimpleName;

public final class SimpleNameFinder extends AbstractFinder<SimpleName> {
    private final String identifier;

    public SimpleNameFinder(final String identifier) {
        this.identifier = identifier;
    }

    @Override
    public boolean visit(final SimpleName visitedNode) {
        if (this.identifier.equals(visitedNode.getIdentifier())) {
            setResult(visitedNode);
            stopSearching();
        }
        return VISIT_CHILDREN;
    }

}