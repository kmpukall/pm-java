package net.creichen.pm.consistency;

import static net.creichen.pm.utils.APIWrapperUtil.statements;
import static net.creichen.pm.utils.ASTQuery.findClassByName;
import static net.creichen.pm.utils.ASTQuery.findMethodByName;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.Collection;

import net.creichen.pm.consistency.inconsistencies.Inconsistency;
import net.creichen.pm.consistency.inconsistencies.UnknownUse;
import net.creichen.pm.tests.PMTest;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.junit.Test;

public class DefUseModelConsistencyCheckTest extends PMTest {

    @Test
    public void whenAUseIsAddedLater_then_theCheckReturnsAnUnknownUse() {
        ICompilationUnit iCompilationUnit = createCompilationUnit("", "S.java", "public class S { int m() {} }");
        CompilationUnit compilationUnit = getProject().getCompilationUnit(iCompilationUnit);

        TypeDeclaration s = findClassByName("S", compilationUnit);
        MethodDeclaration m = findMethodByName("m", s);
        AST ast = m.getAST();
        ReturnStatement returnStatement = ast.newReturnStatement();
        SimpleName x = ast.newSimpleName("x");
        returnStatement.setExpression(x);
        statements(m.getBody()).add(returnStatement);

        Collection<Inconsistency> inconsistencies = new DefUseModelConsistencyCheck(getProject())
        .calculateInconsistencies(getProject().getUDModel());

        assertThat(inconsistencies.size(), is(1));
        Inconsistency inconsistency = inconsistencies.iterator().next();
        assertThat(inconsistency, is(instanceOf(UnknownUse.class)));
        assertThat(inconsistency.getNode(), is((ASTNode) x));
    }
}
