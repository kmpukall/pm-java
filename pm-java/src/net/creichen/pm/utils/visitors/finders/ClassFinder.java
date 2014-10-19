package net.creichen.pm.utils.visitors.finders;

import org.eclipse.jdt.core.dom.TypeDeclaration;

public class ClassFinder extends AbstractFinder<TypeDeclaration> {

    private String className;

    public ClassFinder(String className) {
        this.className = className;
    }

    @Override
    public boolean visit(TypeDeclaration node) {
        if (!node.isInterface() && node.getName().getIdentifier().equals(this.className)) {
            setResult(node);
            stopSearching();
        }
        return true;
    }

}
