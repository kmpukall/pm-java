/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.SimpleName;
import org.junit.Before;
import org.junit.Test;

public class PMASTMatcherTest {

    private AST ast;
    private ASTParser parser;

    @Before
    public void setUp() {
        ast = AST.newAST(AST.JLS4);
        parser = ASTParser.newParser(AST.JLS4);
    }

    @Test
    public void testMatchIsomorphicSimpleNames() {
        ASTNode foo1 = ast.newSimpleName("Foo");
        ASTNode foo2 = ast.newSimpleName("Foo");

        PMASTMatcher matcher = new PMASTMatcher(foo1, foo2);

        assertTrue(matcher.match());
        assertEquals(1, matcher.isomorphicNodes().size());
        assertSame(matcher.isomorphicNodes().get(foo1), foo2);
    }

    @Test
    public void testMatchNonIsomorphicSimpleNames() {
        ASTNode foo = ast.newSimpleName("Foo");
        ASTNode bar = ast.newSimpleName("Bar");

        PMASTMatcher matcher = new PMASTMatcher(foo, bar);

        assertFalse(matcher.match());
        assertEquals(0, matcher.isomorphicNodes().size());
    }

    @Test
    public void testMatchIsomorphicCompilationUnits() {
        parser.setSource("public class S { void m() {int x; x++; int y; y++;} }".toCharArray());
        CompilationUnit compilationUnit1 = (CompilationUnit) parser.createAST(null);
        parser.setSource("public class S { void m() {int x; x++; int y; y++;} }".toCharArray());
        CompilationUnit compilationUnit2 = (CompilationUnit) parser.createAST(null);

        PMASTMatcher matcher = new PMASTMatcher(compilationUnit1, compilationUnit2);

        assertTrue(matcher.match());
        Map<ASTNode, ASTNode> isomorphicNodes = matcher.isomorphicNodes();
        assertSame(isomorphicNodes.get(compilationUnit1), compilationUnit2);
        SimpleName secondY1 = PMASTQuery.simpleNameWithIdentifierInMethodInClassInCompilationUnit(
                "y", 1, "m", 0, "S", 0, compilationUnit1);
        SimpleName secondY2 = PMASTQuery.simpleNameWithIdentifierInMethodInClassInCompilationUnit(
                "y", 1, "m", 0, "S", 0, compilationUnit2);
        assertNotNull(isomorphicNodes.get(secondY1));
        assertSame(isomorphicNodes.get(secondY1), secondY2);
        assertNotNull(isomorphicNodes.get(secondY1.getParent()));
        assertSame(isomorphicNodes.get(secondY1.getParent()), secondY2.getParent());
        assertNotNull(isomorphicNodes.get(secondY1.getParent().getParent()));
        assertSame(isomorphicNodes.get(secondY1.getParent().getParent()), secondY2.getParent()
                .getParent());
        assertNotNull(isomorphicNodes.get(secondY1.getParent().getParent().getParent()));
        assertSame(isomorphicNodes.get(secondY1.getParent().getParent().getParent()), secondY2
                .getParent().getParent().getParent());
        assertNotNull(isomorphicNodes.get(secondY1.getParent().getParent().getParent().getParent()));
        assertSame(isomorphicNodes.get(secondY1.getParent().getParent().getParent().getParent()),
                secondY2.getParent().getParent().getParent().getParent());

    }

    @Test
    public void testMatchNonIsomorphicInSimplePropertyCompilationUnits() {
        parser.setSource("public class S { void m() {int x; x++; int y; y++;} }".toCharArray());
        CompilationUnit compilationUnit1 = (CompilationUnit) parser.createAST(null);
        parser.setSource("public class S { void m() {int x; x++; int y; x++;} }".toCharArray());
        CompilationUnit compilationUnit2 = (CompilationUnit) parser.createAST(null);

        PMASTMatcher matcher = new PMASTMatcher(compilationUnit1, compilationUnit2);

        assertFalse(matcher.match());

    }

    @Test
    public void testMatchNonIsomorphicInChildListPropertyCompilationUnits() {
        parser.setSource("public class S { void m() {int x; x++; int y; y++;} }".toCharArray());
        CompilationUnit compilationUnit1 = (CompilationUnit) parser.createAST(null);
        parser.setSource("public class S { void m() {int x; x++; int y;} }".toCharArray());
        CompilationUnit compilationUnit2 = (CompilationUnit) parser.createAST(null);

        PMASTMatcher matcher = new PMASTMatcher(compilationUnit1, compilationUnit2);

        assertFalse(matcher.match());
    }
}
