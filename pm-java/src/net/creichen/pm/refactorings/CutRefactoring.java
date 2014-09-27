package net.creichen.pm.refactorings;

import java.util.List;

import net.creichen.pm.core.Project;
import net.creichen.pm.steps.CutStep;

import org.eclipse.jdt.core.dom.ASTNode;

public class CutRefactoring {

    private List<ASTNode> selectedNodes;
    private Project project;

    public CutRefactoring(final Project project, final List<ASTNode> selectedNodes) {
        this.project = project;
        this.selectedNodes = selectedNodes;
    }

    public void apply() {
        new CutStep(this.project, this.selectedNodes).apply();
    }
}
