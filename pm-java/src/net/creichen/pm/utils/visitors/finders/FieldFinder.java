package net.creichen.pm.utils.visitors.finders;

import static net.creichen.pm.utils.APIWrapperUtil.fragments;
import static net.creichen.pm.utils.factories.PredicateFactory.hasVariableName;

import java.util.List;

import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import com.google.common.base.Predicate;

public class FieldFinder extends AbstractFinder<FieldDeclaration> {

    private final Predicate<VariableDeclaration> matcher;

    public FieldFinder(String name) {
        this.matcher = hasVariableName(name);
    }

    @Override
    public boolean visit(FieldDeclaration node) {
        List<VariableDeclarationFragment> fragments = fragments(node);
        for (VariableDeclarationFragment fragment : fragments) {
            if (this.matcher.apply(fragment)) {
                setResult(node);
                stopSearching();
            }
        }
        return true;

    }

}
