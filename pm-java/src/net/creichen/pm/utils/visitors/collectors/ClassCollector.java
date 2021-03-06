package net.creichen.pm.utils.visitors.collectors;

import static net.creichen.pm.utils.Constants.VISIT_CHILDREN;

import org.eclipse.jdt.core.dom.TypeDeclaration;

public class ClassCollector extends AbstractCollector<TypeDeclaration> {

    private String className;

    public ClassCollector(String className) {
        this.className = className;
    }

    @Override
    public boolean visit(TypeDeclaration node) {
        if (!node.isInterface() && node.getName().getIdentifier().equals(this.className)) {
            addResult(node);
        }
        return VISIT_CHILDREN;
    }
}
