package net.creichen.pm.utils.visitors;

import java.util.Map;

import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.SimpleName;

public final class SelectiveSimpleNameFinder extends CollectingASTVisitor<SimpleName> {
    private final String identifier;
    private final Map<Name, String> identifiersForNames;

    public SelectiveSimpleNameFinder(final String identifier, final Map<Name, String> identifiersForNames) {
        this.identifier = identifier;
        this.identifiersForNames = identifiersForNames;
    }

    @Override
    public boolean visit(final SimpleName visitedNode) {
        if (this.identifier.equals(this.identifiersForNames.get(visitedNode))) {
            addResult(visitedNode);
        }
        return true;
    }

}