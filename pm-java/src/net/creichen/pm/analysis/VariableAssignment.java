package net.creichen.pm.analysis;

import net.creichen.pm.models.defuse.Def;

import org.eclipse.jdt.core.dom.IBinding;

public final class VariableAssignment {

    private final Def definition;

    private final IBinding variableBinding;

    VariableAssignment(final Def definition, final IBinding variableBinding) {
        this.definition = definition;
        this.variableBinding = variableBinding;
    }

    public Def getDefinition() {
        return this.definition;
    }

    public IBinding getVariableBinding() {
        return this.variableBinding;
    }
}