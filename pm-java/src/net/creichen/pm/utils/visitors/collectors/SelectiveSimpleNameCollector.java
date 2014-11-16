package net.creichen.pm.utils.visitors.collectors;

import static net.creichen.pm.utils.Constants.VISIT_CHILDREN;

import java.util.Collections;
import java.util.Map;

import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.SimpleName;

public final class SelectiveSimpleNameCollector extends AbstractCollector<SimpleName> {
    private final String identifier;
    private Map<Name, String> identifiersForNames = Collections.emptyMap();

    public SelectiveSimpleNameCollector(final String identifier) {
        this.identifier = identifier;
    }

    public SelectiveSimpleNameCollector(final String identifier, final Map<Name, String> identifiersForNames) {
        this.identifier = identifier;
        this.identifiersForNames = identifiersForNames;
    }

    @Override
    public boolean visit(final SimpleName visitedNode) {
        String candidate;
        if (this.identifiersForNames.containsKey(visitedNode)) {
            candidate = this.identifiersForNames.get(visitedNode);
        } else {
            candidate = visitedNode.getIdentifier();
        }
        if (this.identifier.equals(candidate)) {
            addResult(visitedNode);
        }
        return VISIT_CHILDREN;
    }

}