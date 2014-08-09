/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm.analysis;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import net.creichen.pm.tests.PMTest;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.junit.Test;

public class ASTQueryTest extends PMTest {

    @Test
    public void testFindClassByName() {
        final String source = "class S {int x; int f() {} } class T {} class S {int y; int f() {} }";
        final CompilationUnit compilationUnit = toCompilationUnit(source);
        final ASTNode expected = ASTQuery.nodeForSelectionInCompilationUnit(40, 28, compilationUnit);

        TypeDeclaration declaration = ASTQuery.findClassWithName("S", 1, compilationUnit);

        assertThat(declaration, is(equalTo(expected)));
    }

    @Test
    public void testFindFieldByName() {
        final String source = "class S {int x,y; int f() {} int y; int x,y,x; int y; int y,x; int x; }";
        final CompilationUnit compilationUnit = toCompilationUnit(source);
        // the finder finds the SimpleName, so we need to get the parent fragment
        final ASTNode expected = ASTQuery.nodeForSelectionInCompilationUnit(60, 1, compilationUnit).getParent();

        VariableDeclarationFragment field = ASTQuery.findFieldByName("x", 3, "S", 0,
                compilationUnit);

        assertThat(field, is(equalTo(expected)));
    }

    @Test
    public void testFindLocalByName() {
        final String source = "class S {int x,y; int f() {int x,y; try {} catch(Exception x){} while(1) {int y,x;} } }";
        final CompilationUnit compilationUnit = toCompilationUnit(source);
        // the finder finds the SimpleName, so we need to get the parent fragment
        final ASTNode expected = ASTQuery.nodeForSelectionInCompilationUnit(
                "class S {int x,y; int f() {int x,y; try {} catch(Exception x){} while(1) {int y,".length(),
                "x".length(), compilationUnit).getParent();

        VariableDeclaration declaration = ASTQuery.localWithNameInMethodInClassInCompilationUnit("x", 2, "f", 0, "S",
                0, compilationUnit);

        assertThat(declaration, is(equalTo(expected)));
    }

    @Test
    public void testFindMethodByName() {
        final String source = "class S {int x; int f() {} } class S {int y; int f() {} int f() {} }";
        final CompilationUnit compilationUnit = toCompilationUnit(source);
        final ASTNode expected = ASTQuery.nodeForSelectionInCompilationUnit(56, 10, compilationUnit);

        MethodDeclaration method = ASTQuery.methodWithNameInClassInCompilationUnit("f", 1, "S", 1, compilationUnit);

        assertThat(method, is(equalTo(expected)));
    }

    @Test
    public void testFindSimpleNameByIdentifier() {
        final String source = "class S {int x,y; int f(int x) {int x,y; try {x = y + 1;} catch(Exception x){} while(1) {int y,x; x--;} } }";
        final CompilationUnit compilationUnit = toCompilationUnit(source);
        final ASTNode expected = ASTQuery.nodeForSelectionInCompilationUnit(98, 1, compilationUnit);

        SimpleName simpleName = ASTQuery.simpleNameWithIdentifierInMethodInClassInCompilationUnit("x", 4, "f", 0, "S",
                0, compilationUnit);

        assertThat(simpleName, is(equalTo(expected)));
    }

}
