package net.creichen.pm.models.function;

import static net.creichen.pm.utils.APIWrapperUtil.statements;

import java.util.List;

import org.eclipse.jdt.core.dom.*;

public class FunctionModel {

    private MethodDeclaration method;

    public FunctionModel(MethodDeclaration method) {
        this.method = method;
    }

    public Type getReturnType() {
        return this.method.getReturnType2();
    }

    public ReturnStatement getReturnStatement() {
        Block body = this.method.getBody();
        if (body != null) {
            List<Statement> statements = statements(body);
            if (statements.get(0) instanceof ReturnStatement) {
                return (ReturnStatement) statements.get(0);
            }
        }
        return null;
    }

}
