package net.creichen.pm.models.function;


public class SideEffect {

    private SideEffectType type;

    public SideEffect(SideEffectType type) {
        this.type = type;
    }

    public SideEffectType getType() {
        return this.type;
    }

}
