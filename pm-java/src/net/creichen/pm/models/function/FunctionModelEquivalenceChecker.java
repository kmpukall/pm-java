package net.creichen.pm.models.function;

import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.InfixExpression.Operator;

public class FunctionModelEquivalenceChecker {

    private ASTMatcher matcher = new ASTMatcher();

    public boolean checkEquivalence(FunctionModel m1, FunctionModel m2) {
        return compare(m1.getReturnType(), m2.getReturnType())
                && compare(m1.getReturnStatement(), m2.getReturnStatement());
    }

    private boolean matchNullSafe(ASTNode node1, ASTNode node2) {
        if (node1 == null) {
            return node2 == null;
        } else {
            return node1.subtreeMatch(this.matcher, node2);
        }
    }

    private boolean compare(Type t1, Type t2) {
        return matchNullSafe(t1, t2);
    }

    private boolean compare(ReturnStatement s1, ReturnStatement s2) {
        return matchNullSafe(s1, s2) || compare(s1.getExpression(), s2.getExpression());
    }

    private boolean compare(Expression e1, Expression e2) {
        if (e1 instanceof InfixExpression && e2 instanceof InfixExpression) {
            return compare((InfixExpression) e1, (InfixExpression) e2);
        }
        return false;
    }

    private boolean compare(InfixExpression e1, InfixExpression e2) {
        Operator operator = e1.getOperator();
        if (operator != e2.getOperator()) {
            return false;
        }

        if (isCommutative(operator)) {
            return matchNullSafe(e1.getLeftOperand(), e2.getRightOperand()) &&
                    matchNullSafe(e1.getRightOperand(), e2.getLeftOperand());
        }
        // TODO support other operators except for "+"
        return false;
    }

    private boolean isCommutative(Operator operator) {
        return operator == Operator.PLUS || operator == Operator.TIMES;
    }
}
