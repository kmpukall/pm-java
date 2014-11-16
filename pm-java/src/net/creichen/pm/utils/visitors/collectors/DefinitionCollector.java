package net.creichen.pm.utils.visitors.collectors;

import static net.creichen.pm.utils.Constants.VISIT_CHILDREN;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

public final class DefinitionCollector extends AbstractCollector<ASTNode> {

    @Override
    public boolean visit(final Assignment assignment) {
        // plain-old x = y + 1

        if (isAnalyzable(assignment.getLeftHandSide())) {
            addResult(assignment);
        }
        return VISIT_CHILDREN;
    }

    @Override
    public boolean visit(final PostfixExpression postfixExpression) {
        // all postfix operators are definitions
        // x++
        if (isAnalyzable(postfixExpression.getOperand())) {
            addResult(postfixExpression);
        }
        return VISIT_CHILDREN;
    }

    @Override
    public boolean visit(final PrefixExpression prefixExpression) {
        // Can't have things like ! being definitions
        if ((prefixExpression.getOperator() == PrefixExpression.Operator.INCREMENT || prefixExpression.getOperator() == PrefixExpression.Operator.DECREMENT)
                && isAnalyzable(prefixExpression.getOperand())) {
            addResult(prefixExpression);
        }

        return VISIT_CHILDREN;
    }

    @Override
    public boolean visit(final SingleVariableDeclaration singleVariableDeclaration) {
        // Used in parameter lists and catch clauses
        // There is an implicit definition here

        addResult(singleVariableDeclaration);
        return VISIT_CHILDREN;
    }

    @Override
    public boolean visit(final VariableDeclarationFragment variableDeclarationFragment) {
        // int x, y, z = 7; //etc

        addResult(variableDeclarationFragment);
        return VISIT_CHILDREN;
    }

    private boolean isAnalyzable(final ASTNode node) {
        return node instanceof SimpleName;
    }

}