package net.creichen.pm.steps;

import static net.creichen.pm.utils.APIWrapperUtil.getStructuralProperty;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.creichen.pm.api.PMCompilationUnit;
import net.creichen.pm.core.Project;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ChildListPropertyDescriptor;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

public class AddStep extends AbstractStep {

    private ASTNode parent;
    private ChildListPropertyDescriptor property;
    private ASTNode node;

    public AddStep(Project project, ASTNode parent, ChildListPropertyDescriptor property, ASTNode node) {
        super(project);
        this.parent = parent;
        this.property = property;
        this.node = node;
    }

    @Override
    public Map<PMCompilationUnit, ASTRewrite> calculateTextualChange() {
        final Map<PMCompilationUnit, ASTRewrite> result = new HashMap<PMCompilationUnit, ASTRewrite>();

        final ASTRewrite astRewrite = ASTRewrite.create(this.parent.getAST());
        final ASTNode copiedNode = ASTNode.copySubtree(this.parent.getAST(), this.node);

        final ListRewrite lrw = astRewrite.getListRewrite(this.parent, this.property);
        lrw.insertAt(copiedNode, 0, null /* textEditGroup */);

        result.put(getProject().findPMCompilationUnitForNode(this.parent), astRewrite);

        return result;
    }

    @Override
    public void performASTChange() {
        final List<ASTNode> childList = getStructuralProperty(this.property, this.parent);
        final ASTNode copiedNode = ASTNode.copySubtree(this.parent.getAST(), this.node);
        childList.add(copiedNode);
    }

}
