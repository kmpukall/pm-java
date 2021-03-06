/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm.steps;

import static net.creichen.pm.tests.Matchers.hasNoInconsistencies;
import static net.creichen.pm.tests.Matchers.hasSource;
import static net.creichen.pm.utils.APIWrapperUtil.statements;
import static net.creichen.pm.utils.ASTQuery.findClassByName;
import static net.creichen.pm.utils.ASTQuery.findFieldByName;
import static net.creichen.pm.utils.ASTQuery.findMethodByName;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import net.creichen.pm.api.PMCompilationUnit;
import net.creichen.pm.data.Pasteboard;
import net.creichen.pm.tests.PMTest;
import net.creichen.pm.utils.ASTQuery;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.junit.Test;

public class CutStepTest extends PMTest {

    private ICompilationUnit iCompilationUnit;

    @Test
    public void testCutDeclarationButNotReference() throws JavaModelException {
        TypeDeclaration type = createClass("public class S {void m(){int x; x = 1;}}");
        MethodDeclaration methodDeclaration = findMethodByName("m", type);
        List<Statement> statements = statements(methodDeclaration.getBody());

        new CutStep(getProject(), statements.get(0)).apply();

        assertThat(this.iCompilationUnit, hasSource("public class S {void m(){x = 1;}}"));

    }

    @Test
    public void testCutMethodDeclaration() throws JavaModelException {
        TypeDeclaration s = createClass("public class S {void m(){}}");
        MethodDeclaration m = findMethodByName("m", s);

        new CutStep(getProject(), m).apply();

        assertThat(this.iCompilationUnit, hasSource("public class S {}"));
        assertThat(getProject(), hasNoInconsistencies());
    }

    @Test
    public void testCutField() throws JavaModelException {
        TypeDeclaration type = createClass("public class S {S s; void m(){System.out.println(s);}}");
        FieldDeclaration fieldDeclaration = findFieldByName("s", type);

        new CutStep(getProject(), fieldDeclaration).apply();

        assertThat(this.iCompilationUnit, hasSource("public class S {void m(){System.out.println(s);}}"));
    }

    @Test
    public void testCutFieldWithReference() throws JavaModelException {
        TypeDeclaration type = createClass("public class S {int x; void m(){x = 1;}}");

        FieldDeclaration fieldDeclaration = findFieldByName("x", type);

        new CutStep(getProject(), fieldDeclaration).apply();

        assertThat(this.iCompilationUnit, hasSource("public class S {void m(){x = 1;}}"));
    }

    @Test
    public void testCutMethod() throws JavaModelException {
        TypeDeclaration type = createClass("public class S {S s; void m(){System.out.println(s);}}");
        MethodDeclaration methodDeclaration = findMethodByName("m", type);

        new CutStep(getProject(), methodDeclaration).apply();

        assertThat(this.iCompilationUnit, hasSource("public class S {S s;}"));
    }

    @Test
    public void testCutMultipleStatements() throws JavaModelException {
        TypeDeclaration type = createClass("public class S {void m(){int x,y; int a; a = 1; y = 3; x = 2;}}");
        MethodDeclaration methodDeclaration = findMethodByName("m", type);
        List<Statement> statements = statements(methodDeclaration.getBody());

        List<ASTNode> nodesToCut = new ArrayList<ASTNode>();
        nodesToCut.add(statements.get(2));
        nodesToCut.add(statements.get(3));

        new CutStep(getProject(), nodesToCut).apply();

        assertThat(this.iCompilationUnit, hasSource("public class S {void m(){int x,y; int a; x = 2;}}"));
        assertEquals(Pasteboard.getInstance().getPasteboardRoots().size(), 2);
        assertTrue(Pasteboard.getInstance().containsOnlyNodesOfClass(Statement.class));
    }

    @Test
    public void testCutStatement() throws JavaModelException {
        TypeDeclaration type = createClass("public class S {S s; void m(){System.out.println(s);}}");
        MethodDeclaration methodDeclaration = ASTQuery.findMethodByName("m", type);
        Statement firstStatement = statements(methodDeclaration.getBody()).get(0);

        new CutStep(getProject(), firstStatement).apply();

        assertThat(this.iCompilationUnit, hasSource("public class S {S s; void m(){}}"));
    }

    @Test
    public void testInstantiation() {
        TypeDeclaration type = createClass("public class S {S s; void m(){s.getClass(); m();}}");
        MethodDeclaration methodDeclaration = findMethodByName("m", type);

        new CutStep(getProject(), methodDeclaration);
    }

    private TypeDeclaration createClass(final String source) {
        this.iCompilationUnit = createCompilationUnit("", "S.java", source);
        PMCompilationUnit compilationUnit = getProject().getPMCompilationUnit(this.iCompilationUnit);
        return findClassByName("S", compilationUnit);
    }

}
