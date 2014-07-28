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

import net.creichen.pm.PMTest;
import net.creichen.pm.Project;
import net.creichen.pm.Workspace;
import net.creichen.pm.checkers.ConsistencyValidator;
import net.creichen.pm.inconsistencies.Inconsistency;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.junit.Test;

public class DelegateProcessorTest extends PMTest {
    @Test
    public void testAddDelegateToSimpleMethodInvocation() throws JavaModelException {
        final String source = "public class S {void m(){S s = new S();s.getClass(); m();}}";

        final ICompilationUnit compilationUnit = createNewCompilationUnit("", "S.java", source);

        final DelegateProcessor delegateProcessor = new DelegateProcessor(new TextSelection(53, 3), compilationUnit);

        delegateProcessor.setDelegateIdentifier("s");

        final Project pmProject = Workspace.sharedWorkspace().projectForIJavaProject(getIJavaProject());

        ProcessorDriver.drive(delegateProcessor);

        assertTrue(compilationUnitSourceMatchesSource("public class S {void m(){S s = new S();s.getClass(); s.m();}}",
                compilationUnit.getSource()));

        final Collection<Inconsistency> inconsistencies = ConsistencyValidator.getInstance().getInconsistencies();

        assertEquals(0, inconsistencies.size());
    }

    @Test
    public void testAddSuperDelegateToSimpleMethodInvocation() throws JavaModelException {
        final String source = "public class S {void m(){S s; m();}}";

        final ICompilationUnit compilationUnit = createNewCompilationUnit("", "S.java", source);

        final DelegateProcessor delegateProcessor = new DelegateProcessor(new TextSelection(30, 3), compilationUnit);

        delegateProcessor.setDelegateIdentifier("super");

        final Project pmProject = Workspace.sharedWorkspace().projectForIJavaProject(getIJavaProject());

        ProcessorDriver.drive(delegateProcessor);

        assertTrue(compilationUnitSourceMatchesSource("public class S {void m(){S s; super.m();}}",
                compilationUnit.getSource()));

        final Collection<Inconsistency> inconsistencies = ConsistencyValidator.getInstance().getInconsistencies();

        assertEquals(0, inconsistencies.size());
    }

    @Test
    public void testDelegateToField() throws JavaModelException {
        final String source = "public class S {S s; void m(){s.getClass(); m();}}";

        final ICompilationUnit compilationUnit = createNewCompilationUnit("", "S.java", source);

        final DelegateProcessor delegateProcessor = new DelegateProcessor(new TextSelection(44, 3), compilationUnit);

        delegateProcessor.setDelegateIdentifier("s");

        final Project pmProject = Workspace.sharedWorkspace().projectForIJavaProject(getIJavaProject());

        ProcessorDriver.drive(delegateProcessor);

        assertTrue(compilationUnitSourceMatchesSource("public class S {S s; void m(){s.getClass(); s.m();}}",
                compilationUnit.getSource()));

        final Collection<Inconsistency> inconsistencies = ConsistencyValidator.getInstance().getInconsistencies();

        assertEquals(0, inconsistencies.size());
    }

    @Test
    public void testDelegateToFieldInSuperClassWithPackages() throws JavaModelException {
        // String superSource =
        // "package t; public class Super {Super s; void m() { } }";

        final String subSource = "package t; public class Sub extends Super {void g() {m();}}";

        // ICompilationUnit superCompilationUnit = createNewCompilationUnit("t",
        // "Super.java", superSource);
        final ICompilationUnit subCompilationUnit = createNewCompilationUnit("t", "Sub.java", subSource);

        final DelegateProcessor delegateProcessor = new DelegateProcessor(new TextSelection(
                "package t; public class Sub extends Super {void g() {".length(), "m()".length()), subCompilationUnit);

        delegateProcessor.setDelegateIdentifier("s");

        final Project pmProject = Workspace.sharedWorkspace().projectForIJavaProject(getIJavaProject());

        ProcessorDriver.drive(delegateProcessor);

        assertTrue(compilationUnitSourceMatchesSource("package t; public class Sub extends Super {void g() {s.m();}}",
                subCompilationUnit.getSource()));

        final Collection<Inconsistency> inconsistencies = ConsistencyValidator.getInstance().getInconsistencies();

        assertEquals(0, inconsistencies.size());
    }

    @Test
    public void testRemoveDelegateFromSimpleMethodInvocation() throws JavaModelException {
        final String source = "public class S {void m(){S s; s.m();}}";

        final ICompilationUnit compilationUnit = createNewCompilationUnit("", "S.java", source);

        final DelegateProcessor delegateProcessor = new DelegateProcessor(new TextSelection(30, 5), compilationUnit);

        delegateProcessor.setDelegateIdentifier("");

        final Project pmProject = Workspace.sharedWorkspace().projectForIJavaProject(getIJavaProject());

        final RefactoringStatus status = ProcessorDriver.drive(delegateProcessor);

        assertTrue(status.getSeverity() < RefactoringStatus.ERROR);

        assertTrue(compilationUnitSourceMatchesSource("public class S {void m(){S s; m();}}",
                compilationUnit.getSource()));

        final Collection<Inconsistency> inconsistencies = ConsistencyValidator.getInstance().getInconsistencies();

        assertEquals(0, inconsistencies.size());
    }
}
