package net.creichen.pm.utils.visitors.collectors;

import java.util.ArrayList;
import java.util.List;

import net.creichen.pm.api.PMCompilationUnit;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;

abstract class AbstractCollector<T> extends ASTVisitor {

    private final List<T> results;

    protected AbstractCollector() {
        super();
        this.results = new ArrayList<T>();
    }

    public final List<T> getResults() {
        return this.results;
    }

    protected void addResult(final T result) {
        this.results.add(result);
    }

    public List<T> collectFrom(ASTNode node) {
        node.accept(this);
        return getResults();
    }

    public List<T> collectFrom(PMCompilationUnit compilationUnit) {
        compilationUnit.accept(this);
        return getResults();
    }

}
