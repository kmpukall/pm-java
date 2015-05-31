package net.creichen.pm.models.function;

import static net.creichen.pm.utils.factories.ASTNodeFactory.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import net.creichen.pm.utils.ASTUtil;
import net.creichen.pm.utils.factories.ASTNodeFactory;

import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.junit.Before;
import org.junit.Test;

public class SideEffectModelTest {

    private MethodDeclaration m;

    @Before
    public void setUp() {
        this.m = newMethodDeclaration();
        this.m.setName(createSimpleName("m"));
    }

    @Test
    public void whenTheMethodIsEmpty_then_theModelHasNoSideEffects() {
        SideEffectModel model = new SideEffectModel(this.m);

        assertThat(model.getSideEffects(), is(emptyCollectionOf(SideEffect.class)));
    }

    @Test
    public void whenTheReturnIsAMethodCall_then_theModelHasASideEffect() {
        ASTUtil.setReturnStatement(this.m, ASTNodeFactory.newMethodInvocation());

        SideEffectModel model = new SideEffectModel(this.m);

        assertThat(model.getSideEffects(), hasSize(1));
        assertThat(model.getSideEffects().get(0).getType(), is(SideEffectType.METHOD_INVOCATION));
    }

    @Test
    public void whenTheMethodHasAMethodCall_then_theModelHasASideEffect() {
        ASTUtil.addMethodExpression(this.m, ASTNodeFactory.newMethodInvocation());

        SideEffectModel model = new SideEffectModel(this.m);

        assertThat(model.getSideEffects(), hasSize(1));
        assertThat(model.getSideEffects().get(0).getType(), is(SideEffectType.METHOD_INVOCATION));
    }

    @Test
    public void whenTheReturnIsAnAssignmentToAVariable_then_theModelDoesNotHaveASideEffect() {
        Assignment assignment = ASTNodeFactory.newAssignment();
        assignment.setLeftHandSide(createSimpleName("xy"));
        ASTUtil.setReturnStatement(this.m, assignment);

        SideEffectModel model = new SideEffectModel(this.m);

        assertThat(model.getSideEffects(), is(empty()));
    }

    @Test
    public void whenTheMethodHasAnAssignmentToAVariable_then_theModelDoesNotHaveASideEffect() {
        Assignment assignment = ASTNodeFactory.newAssignment();
        assignment.setLeftHandSide(createSimpleName("xy"));
        ASTUtil.addMethodExpression(this.m, assignment);

        SideEffectModel model = new SideEffectModel(this.m);

        assertThat(model.getSideEffects(), is(empty()));
    }

    @Test
    public void whenTheReturnIsAnAssignmentToAField_then_theModelHasASideEffect() {
        Assignment assignment = ASTNodeFactory.newAssignment();
        assignment.setLeftHandSide(newFieldAccess("xy"));
        ASTUtil.setReturnStatement(this.m, assignment);

        SideEffectModel model = new SideEffectModel(this.m);

        assertThat(model.getSideEffects(), hasSize(1));
        assertThat(model.getSideEffects().get(0).getType(), is(SideEffectType.ASSIGNMENT));
    }
}
