package net.creichen.pm.models.function;

import static net.creichen.pm.tests.Matchers.equivalentTo;
import static net.creichen.pm.utils.factories.ASTNodeFactory.*;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import net.creichen.pm.utils.ASTUtil;

import org.eclipse.jdt.core.dom.*;
import org.junit.Before;
import org.junit.Test;

public class FunctionModelEquivalenceCheckerTest {

    private MethodDeclaration method1;
    private MethodDeclaration method2;

    @Before
    public void setUp() {
        this.method1 = newMethodDeclaration();
        this.method2 = newMethodDeclaration();

        this.method1.setName(createSimpleName("method1"));
        this.method2.setName(createSimpleName("method2"));

        this.method1.setReturnType2(newPrimitiveType(PrimitiveType.INT));
        this.method2.setReturnType2(newPrimitiveType(PrimitiveType.INT));
    }

    @Test
    public void whenTwoNewMethodDeclarationsAreCompared_then_theyAreEquivalent() {
        assertThat(newMethodDeclaration(), is(equivalentTo(newMethodDeclaration())));
    }

    @Test
    public void whenTheReturnTypeDiffers_then_theyAreNotEquivalent() {
        this.method1.setReturnType2(newPrimitiveType(PrimitiveType.VOID));
        this.method2.setReturnType2(newPrimitiveType(PrimitiveType.INT));

        assertThat(this.method1, is(not(equivalentTo(this.method2))));
    }

    @Test
    public void whenReturnedConstantsAreDifferent_then_theyAreNotEquivalent() {
        ASTUtil.setReturnStatement(this.method1, newNumberLiteral("5"));
        ASTUtil.setReturnStatement(this.method2, newNumberLiteral("3"));

        assertThat(this.method1, is(not(equivalentTo(this.method2))));
    }

    @Test
    public void whenReturnedAdditionExpressionsAreEqual_then_theyAreEquivalent() {
        ASTUtil.setReturnStatement(this.method1, newNumericalInfixExpression("3 + 5"));
        ASTUtil.setReturnStatement(this.method2, newNumericalInfixExpression("3 + 5"));

        assertThat(this.method1, is(equivalentTo(this.method2)));
    }

    @Test
    public void whenReturnedSubtractionExpressionsAreEqual_then_theyAreEquivalent() {
        ASTUtil.setReturnStatement(this.method1, newNumericalInfixExpression("2 - 1"));
        ASTUtil.setReturnStatement(this.method2, newNumericalInfixExpression("2 - 1"));

        assertThat(this.method1, is(equivalentTo(this.method2)));
    }

    @Test
    public void whenReturnedMultiplicationExpressionsAreEqual_then_theyAreEquivalent() {
        ASTUtil.setReturnStatement(this.method1, newNumericalInfixExpression("3 * 5"));
        ASTUtil.setReturnStatement(this.method2, newNumericalInfixExpression("3 * 5"));

        assertThat(this.method1, is(equivalentTo(this.method2)));
    }

    @Test
    public void whenReturnedDivisionExpressionsAreEqual_then_theyAreEquivalent() {
        ASTUtil.setReturnStatement(this.method1, newNumericalInfixExpression("24 / 3"));
        ASTUtil.setReturnStatement(this.method2, newNumericalInfixExpression("24 / 3"));

        assertThat(this.method1, is(equivalentTo(this.method2)));
    }

    @Test
    public void whenReturnedAdditionExpressionsAreNotEqual1_then_theyAreNotEquivalent() {
        ASTUtil.setReturnStatement(this.method1, newNumericalInfixExpression("1 + 2"));
        ASTUtil.setReturnStatement(this.method2, newNumericalInfixExpression("3 + 4"));

        assertThat(this.method1, is(not(equivalentTo(this.method2))));
    }

    @Test
    public void whenReturnedAdditionExpressionsAreNotEqual2_then_theyAreNotEquivalent() {
        ASTUtil.setReturnStatement(this.method1, newNumericalInfixExpression("1 + 2"));
        ASTUtil.setReturnStatement(this.method2, newNumericalInfixExpression("1 + 4"));

        assertThat(this.method1, is(not(equivalentTo(this.method2))));
    }

    @Test
    public void whenReturnedAdditionExpressionsAreNotEqual3_then_theyAreNotEquivalent() {
        ASTUtil.setReturnStatement(this.method1, newNumericalInfixExpression("1 + 2"));
        ASTUtil.setReturnStatement(this.method2, newNumericalInfixExpression("3 + 2"));

        assertThat(this.method1, is(not(equivalentTo(this.method2))));
    }

    @Test
    public void whenReturnedMultiplicationExpressionsAreNotEqual1_then_theyAreNotEquivalent() {
        ASTUtil.setReturnStatement(this.method1, newNumericalInfixExpression("1 * 2"));
        ASTUtil.setReturnStatement(this.method2, newNumericalInfixExpression("3 * 4"));

        assertThat(this.method1, is(not(equivalentTo(this.method2))));
    }

    @Test
    public void whenReturnedMultiplicationExpressionsAreNotEqual2_then_theyAreNotEquivalent() {
        ASTUtil.setReturnStatement(this.method1, newNumericalInfixExpression("1 * 2"));
        ASTUtil.setReturnStatement(this.method2, newNumericalInfixExpression("1 * 4"));

        assertThat(this.method1, is(not(equivalentTo(this.method2))));
    }

    @Test
    public void whenReturnedMultiplicationExpressionsAreNotEqual3_then_theyAreNotEquivalent() {
        ASTUtil.setReturnStatement(this.method1, newNumericalInfixExpression("1 * 2"));
        ASTUtil.setReturnStatement(this.method2, newNumericalInfixExpression("3 * 2"));

        assertThat(this.method1, is(not(equivalentTo(this.method2))));
    }

    @Test
    public void whenReturnedSubtractionExpressionsAreNotEqual1_then_theyAreNotEquivalent() {
        ASTUtil.setReturnStatement(this.method1, newNumericalInfixExpression("1 - 2"));
        ASTUtil.setReturnStatement(this.method2, newNumericalInfixExpression("3 - 4"));

        assertThat(this.method1, is(not(equivalentTo(this.method2))));
    }

    @Test
    public void whenReturnedSubtractionExpressionsAreNotEqual2_then_theyAreNotEquivalent() {
        ASTUtil.setReturnStatement(this.method1, newNumericalInfixExpression("1 - 2"));
        ASTUtil.setReturnStatement(this.method2, newNumericalInfixExpression("1 - 4"));

        assertThat(this.method1, is(not(equivalentTo(this.method2))));
    }

    @Test
    public void whenReturnedSubtractionExpressionsAreNotEqual3_then_theyAreNotEquivalent() {
        ASTUtil.setReturnStatement(this.method1, newNumericalInfixExpression("1 - 2"));
        ASTUtil.setReturnStatement(this.method2, newNumericalInfixExpression("3 - 2"));

        assertThat(this.method1, is(not(equivalentTo(this.method2))));
    }

    @Test
    public void whenReturnedDivisionExpressionsAreNotEqual1_then_theyAreNotEquivalent() {
        ASTUtil.setReturnStatement(this.method1, newNumericalInfixExpression("1 / 2"));
        ASTUtil.setReturnStatement(this.method2, newNumericalInfixExpression("3 / 4"));

        assertThat(this.method1, is(not(equivalentTo(this.method2))));
    }

    @Test
    public void whenReturnedDivisionExpressionsAreNotEqual2_then_theyAreNotEquivalent() {
        ASTUtil.setReturnStatement(this.method1, newNumericalInfixExpression("1 / 2"));
        ASTUtil.setReturnStatement(this.method2, newNumericalInfixExpression("1 / 4"));

        assertThat(this.method1, is(not(equivalentTo(this.method2))));
    }

    @Test
    public void whenReturnedDivisionExpressionsAreNotEqual3_then_theyAreNotEquivalent() {
        ASTUtil.setReturnStatement(this.method1, newNumericalInfixExpression("1 / 2"));
        ASTUtil.setReturnStatement(this.method2, newNumericalInfixExpression("3 / 2"));

        assertThat(this.method1, is(not(equivalentTo(this.method2))));
    }

    @Test
    public void whenReturnedAdditionExpressionsAreSameExceptForOperandOrder_then_theyAreEquivalent() {
        ASTUtil.setReturnStatement(this.method1, newNumericalInfixExpression("3 + 5"));
        ASTUtil.setReturnStatement(this.method2, newNumericalInfixExpression("5 + 3"));

        assertThat(this.method1, is(equivalentTo(this.method2)));
    }

    @Test
    public void whenReturnedMultiplicationExpressionsAreSameExceptForOperandOrder_then_theyAreEquivalent() {
        ASTUtil.setReturnStatement(this.method1, newNumericalInfixExpression("3 * 5"));
        ASTUtil.setReturnStatement(this.method2, newNumericalInfixExpression("5 * 3"));

        assertThat(this.method1, is(equivalentTo(this.method2)));
    }

    @Test
    public void whenReturnedBooleanExpressionsAreTheSame_then_theyAreEquivalent() {
        this.method1.setReturnType2(newPrimitiveType(PrimitiveType.BOOLEAN));
        this.method2.setReturnType2(newPrimitiveType(PrimitiveType.BOOLEAN));
        ASTUtil.setReturnStatement(this.method1, newBooleanInfixExpression("true && false"));
        ASTUtil.setReturnStatement(this.method2, newBooleanInfixExpression("true && false"));

        assertThat(this.method1, is(equivalentTo(this.method2)));
    }

    @Test
    public void whenReturnedBooleanExpressionsAreNotTheSame_then_theyAreNotEquivalent() {
        this.method1.setReturnType2(newPrimitiveType(PrimitiveType.BOOLEAN));
        this.method2.setReturnType2(newPrimitiveType(PrimitiveType.BOOLEAN));
        ASTUtil.setReturnStatement(this.method1, newBooleanInfixExpression("false && true"));
        ASTUtil.setReturnStatement(this.method2, newBooleanInfixExpression("true && false"));

        assertThat(this.method1, is(not(equivalentTo(this.method2))));
    }
}
