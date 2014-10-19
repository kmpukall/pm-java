package net.creichen.pm.utils.visitors.finders;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;

public abstract class AbstractFinder<T extends ASTNode> extends ASTVisitor {

    private T result;
    boolean hasResult;

    protected final void setResult(T result) {
        if (!this.hasResult) {
            this.result = result;
        }
    }

    protected void stopSearching() {
        this.hasResult = true;
    }

    public T findOn(final ASTNode node) {
        node.accept(this);
        return this.result;
    }

}