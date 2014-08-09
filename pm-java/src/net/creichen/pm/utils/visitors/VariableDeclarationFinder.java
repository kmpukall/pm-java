package net.creichen.pm.utils.visitors;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

public class VariableDeclarationFinder extends ASTVisitor {
    private List<VariableDeclaration> results = new ArrayList<VariableDeclaration>();

    public List<VariableDeclaration> result() {
        return this.results;
    }

    // visitor methods

    @Override
    public boolean visit(final AnonymousClassDeclaration anonymousClass) {
        return false;
    }

    @Override
    public boolean visit(final SingleVariableDeclaration singleVariableDeclaration) {
        this.results.add(singleVariableDeclaration);
        return true;
    }

    @Override
    public boolean visit(final VariableDeclarationFragment fragment) {
        this.results.add(fragment);
        return true;
    }
}