package net.creichen.pm.analysis.reachingdefs;

import net.creichen.pm.models.defuse.Def;

import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.Name;

final class ReachingDefinition {

    private final Def definition;
    private final IBinding variableBinding;

    ReachingDefinition(final Def definition, final IBinding variableBinding) {
        this.definition = definition;
        this.variableBinding = variableBinding;
    }

    Def getDefinition() {
        return this.definition;
    }

    boolean matches(final Name name) {
        return this.variableBinding == name.resolveBinding();
    }
}