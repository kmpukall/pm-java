package net.creichen.pm.utils.visitors.finders;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.TypeDeclaration;

public final class SurroundingNodeFinder extends AbstractFinder<ASTNode> {
    private final int position;

    public SurroundingNodeFinder(final int position) {
        this.position = position;
    }

    @Override
    public boolean visit(final Block node) {
        return visitInternal(node);
    }

    @Override
    public boolean visit(final TypeDeclaration node) {
        return visitInternal(node);
    }

    private boolean visitInternal(final ASTNode node) {
        if (node.getStartPosition() < this.position && this.position < node.getStartPosition() + node.getLength()) {
            setResult(node);
            return true;
        }
        return false;
    }
}