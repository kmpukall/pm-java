package net.creichen.pm.refactorings;

import net.creichen.pm.core.Project;
import net.creichen.pm.steps.PasteStep;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ChildListPropertyDescriptor;

public class PasteRefactoring {

    private Project project;
    private ASTNode parent;
    private ChildListPropertyDescriptor property;
    private int index;

    public PasteRefactoring(final Project project, final ASTNode parent, final ChildListPropertyDescriptor property,
            final int index) {
        this.project = project;
        this.parent = parent;
        this.property = property;
        this.index = index;
    }

    public void apply() {
        new PasteStep(this.project, this.parent, this.property, this.index).applyAllAtOnce();
    }
}
