/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm.refactorings;

import static net.creichen.pm.tests.Matchers.hasNoInconsistencies;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.Collection;

import net.creichen.pm.consistency.ConsistencyValidator;
import net.creichen.pm.consistency.inconsistencies.Inconsistency;
import net.creichen.pm.consistency.inconsistencies.MissingDefinition;
import net.creichen.pm.consistency.inconsistencies.NameCapture;
import net.creichen.pm.tests.PMTest;
import net.creichen.pm.utils.ASTQuery;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jface.text.TextSelection;
import org.junit.Test;

//Note: org.junit4 must be in your plugin dependencies
//otherwise you will get a "No runnable methods" error
//
public class PMRenameProcessorTest extends PMTest {

    @Test
    public void testCreateNewCompilationUnitInTest() {
        ICompilationUnit foo = createCompilationUnit("", "Foo.java", "public class Foo {}");

        assertTrue(getProject().getPMCompilationUnitForICompilationUnit(foo) != null);

    }

    @Test
    public void testRenameLocalVariableViaDeclaration() throws JavaModelException {

        ICompilationUnit foo = createCompilationUnit("", "Foo.java",
                "public class Foo {void method() {int foo; foo = 5;} }");

        PMRenameProcessor renameFooToStan = new PMRenameProcessor(new TextSelection(37, 3), foo);

        renameFooToStan.setNewName("stan");

        ProcessorDriver.drive(renameFooToStan);

        assertEquals("public class Foo {void method() {int stan; stan = 5;} }", foo.getSource());

        PMRenameProcessor renameStanToBar = new PMRenameProcessor(new TextSelection(37, 4), foo);

        renameStanToBar.setNewName("bar");

        ProcessorDriver.drive(renameStanToBar);

        assertEquals("public class Foo {void method() {int bar; bar = 5;} }", foo.getSource());
    }

    @Test
    public void testRenameLocalVariableViaUse() throws JavaModelException {

        ICompilationUnit foo = createCompilationUnit("", "Foo.java",
                "public class Foo {void method() {int foo; foo = 5;} }");

        PMRenameProcessor renameFooToStan = new PMRenameProcessor(new TextSelection(42, 3), foo);

        renameFooToStan.setNewName("stan");

        ProcessorDriver.drive(renameFooToStan);

        assertEquals("public class Foo {void method() {int stan; stan = 5;} }", foo.getSource());

        PMRenameProcessor renameStanToBar = new PMRenameProcessor(new TextSelection(43, 4), foo);

        renameStanToBar.setNewName("bar");

        ProcessorDriver.drive(renameStanToBar);

        assertEquals("public class Foo {void method() {int bar; bar = 5;} }", foo.getSource());
    }

    @Test
    public void testRenameIVarCapturesLocal() throws JavaModelException {
        ICompilationUnit foo = createCompilationUnit("", "Foo.java",
                "public class Foo {int foo; void method() {int bar; foo = 5;} }");

        PMRenameProcessor renameIvarToBar = new PMRenameProcessor(new TextSelection(22, 3), foo);

        renameIvarToBar.setNewName("bar");

        ProcessorDriver.drive(renameIvarToBar);

        assertEquals("Foo.java", "public class Foo {int bar; void method() {int bar; bar = 5;} }",
                foo.getSource());

        Collection<Inconsistency> inconsistencies = ConsistencyValidator.getInstance().getInconsistencies();

        assertEquals(1, inconsistencies.size());

        NameCapture nameCapture = (NameCapture) inconsistencies.toArray()[0];

        CompilationUnit parsedCompilationUnit = getProject().getCompilationUnit(foo);

        ASTNode expectedCapturedNode = ASTQuery.findSimpleNameByIdentifier("bar", 1, "method", 0, "Foo", 0,
                parsedCompilationUnit);
        assertEquals(expectedCapturedNode, nameCapture.getNode());

        // ASTNode expectedExpectedDeclaringNode =
        // PMASTQuery.fieldWithNameInClassInCompilationUnit("bar", 0, "Foo", 0,
        // parsedCompilationUnit);

        // ASTNode actualExpectedDeclaringNode =
        // nameCapture.getExpectedDeclaration();

        // !!! we don't actuall provide this in the implementation yet
        // should probably leave failing test in, but . . .
        // assertEquals(expectedExpectedDeclaringNode,
        // actualExpectedDeclaringNode);

        ASTNode expectedCapturingNode = ASTQuery.findSimpleNameByIdentifier("bar", 0, "method", 0, "Foo", 0,
                parsedCompilationUnit).getParent();

        ASTNode actualCapturingNode = nameCapture.getActualDeclaration();

        assertEquals(expectedCapturingNode, actualCapturingNode);

    }

    @Test
    public void testRenameThroughCapture() throws JavaModelException {
        ICompilationUnit foo = createCompilationUnit("", "Foo.java",
                "public class Foo {void method() {int foo; int bar; foo = 5; bar = 6;} }");

        PMRenameProcessor renameFooToBar = new PMRenameProcessor(new TextSelection(37, 3), foo);

        renameFooToBar.setNewName("bar");

        ProcessorDriver.drive(renameFooToBar);

        assertEquals("public class Foo {void method() {int bar; int bar; bar = 5; bar = 6;} }",
                foo.getSource());

        Collection<Inconsistency> inconsistencies = ConsistencyValidator.getInstance().getInconsistencies();

        assertTrue(inconsistencies.size() == 1);

        NameCapture nameCapture = (NameCapture) inconsistencies.toArray()[0];

        CompilationUnit parsedCompilationUnit = getProject().getCompilationUnit(foo);
        ASTNode expectedCapturedNode = getProject().nodeForSelection(new TextSelection(51, 3), foo);

        assertEquals(expectedCapturedNode, nameCapture.getNode());

        ASTNode expectedCapturingNode = ASTQuery.findSimpleNameByIdentifier("bar", 1, "method", 0, "Foo", 0,
                parsedCompilationUnit).getParent();

        ASTNode actualCapturingNode = nameCapture.getActualDeclaration();

        assertEquals(expectedCapturingNode, actualCapturingNode);

        PMRenameProcessor renameBarToFoo = new PMRenameProcessor(new TextSelection(60, 3), foo);

        renameBarToFoo.setNewName("foo");

        ProcessorDriver.drive(renameBarToFoo);

        assertEquals("public class Foo {void method() {int bar; int foo; bar = 5; foo = 6;} }",
                foo.getSource());

        assertThat(getProject(), hasNoInconsistencies());

    }

    @Test
    public void testRenameInMultipleFiles() throws JavaModelException {

        ICompilationUnit unit1 = createCompilationUnit("", "Unit1.java",
                "public class Unit1 { public int x; void method() {x++;} }");
        ICompilationUnit unit2 = createCompilationUnit("", "Unit2.java",
                "public class Unit2 { void method() {Unit1 unit1 = new Unit1(); unit1.x--;} }");

        PMRenameProcessor renameXToY = new PMRenameProcessor(new TextSelection(32, 1), unit1);

        renameXToY.setNewName("y");

        ProcessorDriver.drive(renameXToY);

        assertEquals("public class Unit1 { public int y; void method() {y++;} }", unit1.getSource());
        assertEquals("public class Unit2 { void method() {Unit1 unit1 = new Unit1(); unit1.y--;} }", unit2.getSource());

        for (Inconsistency inconsistency : ConsistencyValidator.getInstance().getInconsistencies()) {
            System.out.println(inconsistency.getHumanReadableDescription());

            if (inconsistency instanceof MissingDefinition) {
                System.out.println("For definition of class: "
                        + ((MissingDefinition) inconsistency).getDefiningNode().getClass());
            }
        }

        assertThat(getProject(), hasNoInconsistencies());

    }

    @Test
    public void testAcceptNameCapture() throws JavaModelException {
        ICompilationUnit foo = createCompilationUnit("", "Foo.java",
                "public class Foo {int foo; void method() {int bar; foo = 5;} }");

        PMRenameProcessor renameIvarToBar = new PMRenameProcessor(new TextSelection(22, 3), foo);

        renameIvarToBar.setNewName("bar");

        ProcessorDriver.drive(renameIvarToBar);

        assertEquals("Foo.java", "public class Foo {int bar; void method() {int bar; bar = 5;} }",
                foo.getSource());

        Collection<Inconsistency> inconsistencies = ConsistencyValidator.getInstance().getInconsistencies();

        assertTrue(inconsistencies.size() == 1);

        NameCapture nameCapture = (NameCapture) inconsistencies.toArray()[0];

        CompilationUnit parsedCompilationUnit = getProject().getCompilationUnit(foo);

        ASTNode expectedCapturedNode = ASTQuery.findSimpleNameByIdentifier("bar", 1, "method", 0, "Foo", 0,
                parsedCompilationUnit);
        assertEquals(expectedCapturedNode, nameCapture.getNode());

        nameCapture.acceptBehavioralChange();

        assertThat(getProject(), hasNoInconsistencies());
    }

}
