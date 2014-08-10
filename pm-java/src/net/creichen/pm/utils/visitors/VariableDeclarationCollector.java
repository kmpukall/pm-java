package net.creichen.pm.utils.visitors;

import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

public class VariableDeclarationCollector extends CollectingASTVisitor<VariableDeclaration> {
    // visitor methods

    @Override
    public boolean visit(final AnonymousClassDeclaration anonymousClass) {
        return false;
    }

    @Override
    public boolean visit(final SingleVariableDeclaration singleVariableDeclaration) {
        addResult(singleVariableDeclaration);
        return true;
    }

    @Override
    public boolean visit(final VariableDeclarationFragment fragment) {
        addResult(fragment);
        return true;
    }
}