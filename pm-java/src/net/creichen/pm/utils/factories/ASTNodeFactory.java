package net.creichen.pm.utils.factories;

import java.util.function.Function;

import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.InfixExpression.Operator;

public class ASTNodeFactory {

    private static final AST AST_INSTANCE = AST.newAST(AST.JLS8);

    public static SimpleName createSimpleName(String name) {
        return AST_INSTANCE.newSimpleName(name);
    }

    public static MethodDeclaration newMethodDeclaration() {
        return AST_INSTANCE.newMethodDeclaration();
    }

    public static PrimitiveType newPrimitiveType(PrimitiveType.Code code) {
        return AST_INSTANCE.newPrimitiveType(code);
    }

    public static Block createBlock() {
        return AST_INSTANCE.newBlock();
    }

    public static ReturnStatement createReturnStatement() {
        return AST_INSTANCE.newReturnStatement();
    }

    public static ExpressionStatement newExpressionStatement(Expression ex) {
        return AST_INSTANCE.newExpressionStatement(ex);
    }

    public static FieldAccess newFieldAccess(String fieldName) {
        FieldAccess field = AST_INSTANCE.newFieldAccess();
        field.setName(createSimpleName(fieldName));
        return field;
    }

    public static NumberLiteral newNumberLiteral(String literal) {
        return AST_INSTANCE.newNumberLiteral(literal);
    }

    public static BooleanLiteral newBooleanLiteral(String literal) {
        return AST_INSTANCE.newBooleanLiteral(Boolean.valueOf(literal));
    }

    public static MethodInvocation newMethodInvocation() {
        return AST_INSTANCE.newMethodInvocation();
    }

    public static Assignment newAssignment() {
        return AST_INSTANCE.newAssignment();
    }

    public static Expression newNumericalInfixExpression(String literal) {
        return newInfixExpression(literal, ASTNodeFactory::newNumberLiteral);
    }

    public static Expression newBooleanInfixExpression(String literal) {
        return newInfixExpression(literal, ASTNodeFactory::newBooleanLiteral);
    }

    public static InfixExpression newInfixExpression(String expressionLiteral,
            Function<String, Expression> literalMapping) {
        String[] expressionSegments = expressionLiteral.split(" ");
        if (expressionSegments.length != 3) {
            throw new IllegalArgumentException("Cannot create an infix expression from literal " + expressionLiteral
                    + ": literal must have three segments separated by spaces!");
        }

        InfixExpression expression = AST_INSTANCE.newInfixExpression();
        expression.setLeftOperand(literalMapping.apply(expressionSegments[0]));
        expression.setOperator(Operator.toOperator(expressionSegments[1]));
        expression.setRightOperand(literalMapping.apply(expressionSegments[2]));
        return expression;
    }

    private ASTNodeFactory() {
        // private utility class constructor
    }

}
