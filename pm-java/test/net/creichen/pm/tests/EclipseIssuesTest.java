/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm.tests;

import net.creichen.pm.PMTest;
import net.creichen.pm.analysis.ASTQuery;

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class EclipseIssuesTest extends PMTest {

    @Test
    @Ignore
    public void testImpreciseBindingsKeysForDuplicateLocalVariables() {
        // A test to verify the claims about Eclipse's imprecise name analysis
        // that we make in the paper.

        String source = "public class S {void m(){int x; x++; int x; x--;}}";

        CompilationUnit compilationUnit = parseCompilationUnitFromSource(source, "S.java");

        SimpleName firstX = ASTQuery.simpleNameWithIdentifierInMethodInClassInCompilationUnit(
                "x", 0, "m", 0, "S", 0, compilationUnit);
        SimpleName secondX = ASTQuery.simpleNameWithIdentifierInMethodInClassInCompilationUnit(
                "x", 1, "m", 0, "S", 0, compilationUnit);

        SimpleName thirdX = ASTQuery.simpleNameWithIdentifierInMethodInClassInCompilationUnit(
                "x", 2, "m", 0, "S", 0, compilationUnit);
        SimpleName fourthX = ASTQuery.simpleNameWithIdentifierInMethodInClassInCompilationUnit(
                "x", 3, "m", 0, "S", 0, compilationUnit);

        /*
         * System.out.println("firstX binding key is " + firstX.resolveBinding().getKey());
         * System.out.println("firstX binding hash is " + firstX.resolveBinding().hashCode());
         * 
         * System.out.println("secondX binding key is " + secondX.resolveBinding().getKey());
         * System.out.println("secondX binding hash is " + secondX.resolveBinding().hashCode());
         * 
         * System.out.println("thirdX binding key is " + thirdX.resolveBinding().getKey());
         * System.out.println("thirdX binding hash is " + thirdX.resolveBinding().hashCode());
         * 
         * System.out.println("fourthX binding key is " + fourthX.resolveBinding().getKey());
         * System.out.println("fourthX binding hash is " + fourthX.resolveBinding().hashCode());
         */

        // We want to assert that the bindings objects are correct (that is, two
        // separate bindings, one for the first decl
        // and one for the second, but that they keys are equal.

        assertEquals(firstX.resolveBinding().getKey(), thirdX.resolveBinding().getKey());
        assertTrue(firstX.resolveBinding().hashCode() != thirdX.resolveBinding().hashCode());

        // No we want the make sure that the second use of X is bound to the
        // first declaration
        // and the fourth is bound to the second declaration

        assertEquals(firstX.resolveBinding(), secondX.resolveBinding());
        assertEquals(thirdX.resolveBinding(), fourthX.resolveBinding());

    }

    @Test
    @Ignore
    public void testNoBindingsKeysForDuplicateFields() {
        // A test to verify the claims about Eclipse's imprecise name analysis
        // that we make in the paper.

        String source = "public class S {int x; int x; void m(){x++;}}";

        CompilationUnit compilationUnit = parseCompilationUnitFromSource(source, "S.java");

        VariableDeclarationFragment firstXField = ASTQuery.fieldWithNameInClassInCompilationUnit(
                "x", 0, "S", 0, compilationUnit);
        assertTrue(firstXField != null);

        VariableDeclarationFragment secondXField = ASTQuery
                .fieldWithNameInClassInCompilationUnit("x", 1, "S", 0, compilationUnit);
        assertTrue(secondXField != null);

        // System.out.println("firstFieldX binding key is " +
        // firstXField.resolveBinding().getKey());
        // System.out.println("firstFieldX binding hash is " +
        // firstXField.resolveBinding().hashCode());

        // System.out.println("secondFieldX binding key is " +
        // secondXField.resolveBinding().getKey());
        // System.out.println("secondFieldX binding hash is " +
        // secondXField.resolveBinding().hashCode());

        SimpleName firstLocalX = ASTQuery
                .simpleNameWithIdentifierInMethodInClassInCompilationUnit("x", 0, "m", 0, "S", 0,
                        compilationUnit);

        assertTrue(firstLocalX.resolveBinding() == null);

        // System.out.println("firstLocalX binding key is " +
        // firstLocalX.resolveBinding().getKey());
        // System.out.println("firstLocalX binding hash is " +
        // firstLocalX.resolveBinding().hashCode());

    }
}
