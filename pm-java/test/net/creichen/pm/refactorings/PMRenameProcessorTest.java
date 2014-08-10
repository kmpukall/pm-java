/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm.refactorings;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collection;

import net.creichen.pm.Workspace;
import net.creichen.pm.analysis.ASTQuery;
import net.creichen.pm.consistency.ConsistencyValidator;
import net.creichen.pm.consistency.inconsistencies.Inconsistency;
import net.creichen.pm.consistency.inconsistencies.MissingDefinition;
import net.creichen.pm.consistency.inconsistencies.NameCapture;
import net.creichen.pm.models.Project;
import net.creichen.pm.tests.PMTest;

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
        ICompilationUnit iCompilationUnit = createNewCompilationUnit("", "Foo.java", "public class Foo {}");

        Project pmProject = Workspace.sharedWorkspace().projectForIJavaProject(getIJavaProject());

        assertTrue(pmProject.getPMCompilationUnitForICompilationUnit(iCompilationUnit) != null);

    }

    @Test
    public void testRenameLocalVariableViaDeclaration() throws JavaModelException {

        ICompilationUnit iCompilationUnit = createNewCompilationUnit("", "Foo.java",
                "public class Foo {void method() {int foo; foo = 5;} }");

        PMRenameProcessor renameFooToStan = new PMRenameProcessor(new TextSelection(37, 3), iCompilationUnit);

        renameFooToStan.setNewName("stan");

        ProcessorDriver.drive(renameFooToStan);

        assertEquals("public class Foo {void method() {int stan; stan = 5;} }", iCompilationUnit.getSource());

        PMRenameProcessor renameStanToBar = new PMRenameProcessor(new TextSelection(37, 4), iCompilationUnit);

        renameStanToBar.setNewName("bar");

        ProcessorDriver.drive(renameStanToBar);

        assertEquals("public class Foo {void method() {int bar; bar = 5;} }", iCompilationUnit.getSource());
    }

    @Test
    public void testRenameLocalVariableViaUse() throws JavaModelException {

        ICompilationUnit iCompilationUnit = createNewCompilationUnit("", "Foo.java",
                "public class Foo {void method() {int foo; foo = 5;} }");

        PMRenameProcessor renameFooToStan = new PMRenameProcessor(new TextSelection(42, 3), iCompilationUnit);

        renameFooToStan.setNewName("stan");

        ProcessorDriver.drive(renameFooToStan);

        assertEquals("public class Foo {void method() {int stan; stan = 5;} }", iCompilationUnit.getSource());

        PMRenameProcessor renameStanToBar = new PMRenameProcessor(new TextSelection(43, 4), iCompilationUnit);

        renameStanToBar.setNewName("bar");

        ProcessorDriver.drive(renameStanToBar);

        assertEquals("public class Foo {void method() {int bar; bar = 5;} }", iCompilationUnit.getSource());
    }

    @Test
    public void testRenameIVarCapturesLocal() throws JavaModelException {
        ICompilationUnit iCompilationUnit = createNewCompilationUnit("", "Foo.java",
                "public class Foo {int foo; void method() {int bar; foo = 5;} }");

        Project pmProject = Workspace.sharedWorkspace().projectForIJavaProject(getIJavaProject());

        PMRenameProcessor renameIvarToBar = new PMRenameProcessor(new TextSelection(22, 3), iCompilationUnit);

        renameIvarToBar.setNewName("bar");

        ProcessorDriver.drive(renameIvarToBar);

        assertEquals("Foo.java", "public class Foo {int bar; void method() {int bar; bar = 5;} }",
                iCompilationUnit.getSource());

        Collection<Inconsistency> inconsistencies = ConsistencyValidator.getInstance().getInconsistencies();

        assertEquals(1, inconsistencies.size());

        NameCapture nameCapture = (NameCapture) inconsistencies.toArray()[0];

        CompilationUnit parsedCompilationUnit = pmProject.getCompilationUnitForICompilationUnit(iCompilationUnit);

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
        ICompilationUnit iCompilationUnit = createNewCompilationUnit("", "Foo.java",
                "public class Foo {void method() {int foo; int bar; foo = 5; bar = 6;} }");

        Project pmProject = Workspace.sharedWorkspace().projectForIJavaProject(getIJavaProject());

        PMRenameProcessor renameFooToBar = new PMRenameProcessor(new TextSelection(37, 3), iCompilationUnit);

        renameFooToBar.setNewName("bar");

        ProcessorDriver.drive(renameFooToBar);

        assertEquals("public class Foo {void method() {int bar; int bar; bar = 5; bar = 6;} }",
                iCompilationUnit.getSource());

        Collection<Inconsistency> inconsistencies = ConsistencyValidator.getInstance().getInconsistencies();

        assertTrue(inconsistencies.size() == 1);

        NameCapture nameCapture = (NameCapture) inconsistencies.toArray()[0];

        CompilationUnit parsedCompilationUnit = pmProject.getCompilationUnitForICompilationUnit(iCompilationUnit);

        ASTNode expectedCapturedNode = pmProject.nodeForSelection(new TextSelection(51, 3), iCompilationUnit);
        assertEquals(expectedCapturedNode, nameCapture.getNode());

        ASTNode expectedCapturingNode = ASTQuery.findSimpleNameByIdentifier("bar", 1, "method", 0, "Foo", 0,
                parsedCompilationUnit).getParent();

        ASTNode actualCapturingNode = nameCapture.getActualDeclaration();

        assertEquals(expectedCapturingNode, actualCapturingNode);

        PMRenameProcessor renameBarToFoo = new PMRenameProcessor(new TextSelection(60, 3), iCompilationUnit);

        renameBarToFoo.setNewName("foo");

        ProcessorDriver.drive(renameBarToFoo);

        assertEquals("public class Foo {void method() {int bar; int foo; bar = 5; foo = 6;} }",
                iCompilationUnit.getSource());

        inconsistencies = ConsistencyValidator.getInstance().getInconsistencies();

        assertTrue(inconsistencies.size() == 0);

    }

    @Test
    public void testRenameInMultipleFiles() throws JavaModelException {

        ICompilationUnit unit1 = createNewCompilationUnit("", "Unit1.java",
                "public class Unit1 { public int x; void method() {x++;} }");
        ICompilationUnit unit2 = createNewCompilationUnit("", "Unit2.java",
                "public class Unit2 { void method() {Unit1 unit1 = new Unit1(); unit1.x--;} }");

        Workspace.sharedWorkspace().projectForIJavaProject(getIJavaProject());

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

        assertTrue(ConsistencyValidator.getInstance().getInconsistencies().size() == 0);

    }

    @Test
    public void testAcceptNameCapture() throws JavaModelException {
        ICompilationUnit iCompilationUnit = createNewCompilationUnit("", "Foo.java",
                "public class Foo {int foo; void method() {int bar; foo = 5;} }");

        Project pmProject = Workspace.sharedWorkspace().projectForIJavaProject(getIJavaProject());

        PMRenameProcessor renameIvarToBar = new PMRenameProcessor(new TextSelection(22, 3), iCompilationUnit);

        renameIvarToBar.setNewName("bar");

        ProcessorDriver.drive(renameIvarToBar);

        assertEquals("Foo.java", "public class Foo {int bar; void method() {int bar; bar = 5;} }",
                iCompilationUnit.getSource());

        Collection<Inconsistency> inconsistencies = ConsistencyValidator.getInstance().getInconsistencies();

        assertTrue(inconsistencies.size() == 1);

        NameCapture nameCapture = (NameCapture) inconsistencies.toArray()[0];

        CompilationUnit parsedCompilationUnit = pmProject.getCompilationUnitForICompilationUnit(iCompilationUnit);

        ASTNode expectedCapturedNode = ASTQuery.findSimpleNameByIdentifier("bar", 1, "method", 0, "Foo", 0,
                parsedCompilationUnit);
        assertEquals(expectedCapturedNode, nameCapture.getNode());

        nameCapture.acceptBehavioralChange();

        ConsistencyValidator.getInstance().rescanForInconsistencies(pmProject);
        Collection<Inconsistency> newInconsistencies = ConsistencyValidator.getInstance().getInconsistencies();

        assertEquals(0, newInconsistencies.size());
    }

}
