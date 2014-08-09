package net.creichen.pm.utils.visitors;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;

abstract class CollectingASTVisitor<T extends ASTNode> extends ASTVisitor {

    private final List<T> results;

    protected CollectingASTVisitor() {
        super();
        this.results = new ArrayList<T>();
    }

    public final List<T> getResults() {
        return this.results;
    }

    protected void addResult(final T result) {
        this.results.add(result);
    }

}
