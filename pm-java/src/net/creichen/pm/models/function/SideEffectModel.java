package net.creichen.pm.models.function;

import java.util.ArrayList;
import java.util.List;

import net.creichen.pm.utils.visitors.collectors.SideEffectCollector;

import org.eclipse.jdt.core.dom.MethodDeclaration;

public class SideEffectModel {

    private List<SideEffect> sideEffects;

    public SideEffectModel(MethodDeclaration method) {
        this.sideEffects = new ArrayList<>();

        if (method.getBody() != null) {
            this.sideEffects = new SideEffectCollector().collectFrom(method);
        }
    }

    public List<SideEffect> getSideEffects() {
        return this.sideEffects;
    }

}
