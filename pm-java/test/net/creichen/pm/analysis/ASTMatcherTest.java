/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm.analysis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import net.creichen.pm.utils.ASTQuery;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.SimpleName;
import org.junit.Before;
import org.junit.Test;

public class ASTMatcherTest {

    private AST ast;
    private ASTParser parser;

    @Before
    public void setUp() {
        this.ast = AST.newAST(AST.JLS4);
        this.parser = ASTParser.newParser(AST.JLS4);
    }

    @Test
    public void testMatchesIsomorphicSimpleNames() {
        ASTNode foo1 = this.ast.newSimpleName("Foo");
        ASTNode foo2 = this.ast.newSimpleName("Foo");

        ASTMatcher matcher = new ASTMatcher(foo1, foo2);

        assertTrue(matcher.matches());
        assertEquals(1, matcher.isomorphicNodes().size());
        assertSame(matcher.isomorphicNodes().get(foo1), foo2);
    }

    @Test
    public void testMatchesNonIsomorphicSimpleNames() {
        ASTNode foo = this.ast.newSimpleName("Foo");
        ASTNode bar = this.ast.newSimpleName("Bar");

        ASTMatcher matcher = new ASTMatcher(foo, bar);

        assertFalse(matcher.matches());
        assertEquals(0, matcher.isomorphicNodes().size());
    }

    @Test
    public void testMatchesIsomorphicCompilationUnits() {
        this.parser.setSource("public class S { void m() {int x; x++; int y; y++;} }".toCharArray());
        CompilationUnit compilationUnit1 = (CompilationUnit) this.parser.createAST(null);
        this.parser.setSource("public class S { void m() {int x; x++; int y; y++;} }".toCharArray());
        CompilationUnit compilationUnit2 = (CompilationUnit) this.parser.createAST(null);

        ASTMatcher matcher = new ASTMatcher(compilationUnit1, compilationUnit2);

        assertTrue(matcher.matches());
        Map<ASTNode, ASTNode> isomorphicNodes = matcher.isomorphicNodes();
        assertSame(isomorphicNodes.get(compilationUnit1), compilationUnit2);
        SimpleName secondY1 = ASTQuery.findSimpleNameByIdentifier("y", 1, "m", 0, "S", 0,
                compilationUnit1);
        SimpleName secondY2 = ASTQuery.findSimpleNameByIdentifier("y", 1, "m", 0, "S", 0,
                compilationUnit2);
        assertNotNull(isomorphicNodes.get(secondY1));
        assertSame(isomorphicNodes.get(secondY1), secondY2);
        assertNotNull(isomorphicNodes.get(secondY1.getParent()));
        assertSame(isomorphicNodes.get(secondY1.getParent()), secondY2.getParent());
        assertNotNull(isomorphicNodes.get(secondY1.getParent().getParent()));
        assertSame(isomorphicNodes.get(secondY1.getParent().getParent()), secondY2.getParent().getParent());
        assertNotNull(isomorphicNodes.get(secondY1.getParent().getParent().getParent()));
        assertSame(isomorphicNodes.get(secondY1.getParent().getParent().getParent()), secondY2.getParent().getParent()
                .getParent());
        assertNotNull(isomorphicNodes.get(secondY1.getParent().getParent().getParent().getParent()));
        assertSame(isomorphicNodes.get(secondY1.getParent().getParent().getParent().getParent()), secondY2.getParent()
                .getParent().getParent().getParent());

    }

    @Test
    public void testMatchesNonIsomorphicInSimplePropertyCompilationUnits() {
        this.parser.setSource("public class S { void m() {int x; x++; int y; y++;} }".toCharArray());
        CompilationUnit compilationUnit1 = (CompilationUnit) this.parser.createAST(null);
        this.parser.setSource("public class S { void m() {int x; x++; int y; x++;} }".toCharArray());
        CompilationUnit compilationUnit2 = (CompilationUnit) this.parser.createAST(null);

        ASTMatcher matcher = new ASTMatcher(compilationUnit1, compilationUnit2);

        assertFalse(matcher.matches());

    }

    @Test
    public void testMatchesNonIsomorphicInChildListPropertyCompilationUnits() {
        this.parser.setSource("public class S { void m() {int x; x++; int y; y++;} }".toCharArray());
        CompilationUnit compilationUnit1 = (CompilationUnit) this.parser.createAST(null);
        this.parser.setSource("public class S { void m() {int x; x++; int y;} }".toCharArray());
        CompilationUnit compilationUnit2 = (CompilationUnit) this.parser.createAST(null);

        ASTMatcher matcher = new ASTMatcher(compilationUnit1, compilationUnit2);

        assertFalse(matcher.matches());
    }
}
