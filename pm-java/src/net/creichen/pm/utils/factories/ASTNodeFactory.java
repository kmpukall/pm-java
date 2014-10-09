package net.creichen.pm.utils.factories;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.SimpleName;

public class ASTNodeFactory {

    private static final AST AST_INSTANCE = AST.newAST(AST.JLS4);

    public static SimpleName createSimpleName(String name) {
        return AST_INSTANCE.newSimpleName(name);
    }

    private ASTNodeFactory() {
        // private utility class constructor
    }

}
