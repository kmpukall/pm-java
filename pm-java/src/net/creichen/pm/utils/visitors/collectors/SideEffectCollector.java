package net.creichen.pm.utils.visitors.collectors;

import net.creichen.pm.models.function.SideEffect;
import net.creichen.pm.models.function.SideEffectType;

import org.eclipse.jdt.core.dom.*;

public class SideEffectCollector extends AbstractCollector<SideEffect> {

    @Override
    public boolean visit(MethodInvocation node) {
        addResult(new SideEffect(SideEffectType.METHOD_INVOCATION));
        return true;
    }

    @Override
    public boolean visit(Assignment node) {
        if (node.getLeftHandSide() instanceof FieldAccess) {
            addResult(new SideEffect(SideEffectType.ASSIGNMENT));
        }
        return true;
    }

}
