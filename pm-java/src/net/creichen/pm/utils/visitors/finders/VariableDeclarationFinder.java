package net.creichen.pm.utils.visitors.finders;

import static net.creichen.pm.utils.Constants.SKIP_CHILDREN;
import static net.creichen.pm.utils.Constants.VISIT_CHILDREN;
import static net.creichen.pm.utils.factories.PredicateFactory.hasVariableName;

import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import com.google.common.base.Predicate;

public class VariableDeclarationFinder extends AbstractFinder<VariableDeclaration> {

    private Predicate<VariableDeclaration> filter;

    public VariableDeclarationFinder(final String variableName) {
        this.filter = hasVariableName(variableName);
    }

    @Override
    public boolean visit(final AnonymousClassDeclaration anonymousClass) {
        return SKIP_CHILDREN;
    }

    @Override
    public boolean visit(final SingleVariableDeclaration singleVariableDeclaration) {
        if (this.filter.apply(singleVariableDeclaration)) {
            setResult(singleVariableDeclaration);
            stopSearching();
        }
        return VISIT_CHILDREN;
    }

    @Override
    public boolean visit(final VariableDeclarationFragment fragment) {
        if (this.filter.apply(fragment)) {
            setResult(fragment);
            stopSearching();
        }
        return VISIT_CHILDREN;
    }
}