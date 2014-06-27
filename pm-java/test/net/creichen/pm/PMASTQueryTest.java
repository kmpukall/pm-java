/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import net.creichen.pm.tests.PMTest;

import org.eclipse.jdt.core.dom.*;
import org.junit.Test;

public class PMASTQueryTest extends PMTest {

    @Test
    public void testFindClassByName() {

        final String source = "class S {int x; int f() {} } class T {} class S {int y; int f() {} }";

        final CompilationUnit compilationUnit = parseCompilationUnitFromSource(source);

        final ASTNode secondS = PMASTQuery.nodeForSelectionInCompilationUnit(40, 28,
                compilationUnit);

        assertTrue(secondS instanceof TypeDeclaration);
        assertEquals(secondS, PMASTQuery.classWithNameInCompilationUnit("S", 1, compilationUnit));
    }

    @Test
    public void testFindFieldByName() {

        final String source = "class S {int x,y; int f() {} int y; int x,y,x; int y; int y,x; int x; }";

        final CompilationUnit compilationUnit = parseCompilationUnitFromSource(source);

        final ASTNode field = PMASTQuery.nodeForSelectionInCompilationUnit(60, 1, compilationUnit)
                .getParent(); // the finder finds the
                              // SimpleName, so we need to get
                              // the parent fragment

        assertTrue(field instanceof VariableDeclarationFragment);

        assertEquals(field,
                PMASTQuery.fieldWithNameInClassInCompilationUnit("x", 3, "S", 0, compilationUnit));
    }

    @Test
    public void testFindLocalByName() {

        final String source = "class S {int x,y; int f() {int x,y; try {} catch(Exception x){} while(1) {int y,x;} } }";

        final CompilationUnit compilationUnit = parseCompilationUnitFromSource(source);

        final ASTNode local = PMASTQuery.nodeForSelectionInCompilationUnit(80, 1, compilationUnit)
                .getParent(); // the finder finds the
                              // SimpleName, so we need to get
                              // the parent fragment

        assertTrue(local instanceof VariableDeclarationFragment);

        assertEquals(local, PMASTQuery.localWithNameInMethodInClassInCompilationUnit("x", 2, "f",
                0, "S", 0, compilationUnit));
    }

    @Test
    public void testFindMethodByName() {

        final String source = "class S {int x; int f() {} } class S {int y; int f() {} int f() {} }";

        final CompilationUnit compilationUnit = parseCompilationUnitFromSource(source);

        final ASTNode secondF = PMASTQuery.nodeForSelectionInCompilationUnit(56, 10,
                compilationUnit);

        assertTrue(secondF instanceof MethodDeclaration);
        assertEquals(secondF,
                PMASTQuery.methodWithNameInClassInCompilationUnit("f", 1, "S", 1, compilationUnit));
    }

    @Test
    public void testFindSimpleNameByIdentifier() {

        final String source = "class S {int x,y; int f(int x) {int x,y; try {x = y + 1;} catch(Exception x){} while(1) {int y,x; x--;} } }";

        final CompilationUnit compilationUnit = parseCompilationUnitFromSource(source);

        final ASTNode simpleName = PMASTQuery.nodeForSelectionInCompilationUnit(98, 1,
                compilationUnit);

        assertTrue(simpleName instanceof SimpleName);

        assertEquals(simpleName,
                PMASTQuery.simpleNameWithIdentifierInMethodInClassInCompilationUnit("x", 4, "f", 0,
                        "S", 0, compilationUnit));
    }

}
