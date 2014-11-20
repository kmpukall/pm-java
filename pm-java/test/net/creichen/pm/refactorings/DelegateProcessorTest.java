/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm.refactorings;

import static net.creichen.pm.tests.Matchers.hasNoInconsistencies;
import static net.creichen.pm.tests.Matchers.hasSource;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.assertThat;
import net.creichen.pm.tests.PMTest;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.junit.Test;

public class DelegateProcessorTest extends PMTest {
    @Test
    public void testAddDelegateToSimpleMethodInvocation() throws JavaModelException {
        final String source = "public class S {void m(){S s = new S();s.getClass(); m();}}";

        final ICompilationUnit s = createCompilationUnit("", "S.java", source);

        final DelegateProcessor delegateProcessor = new DelegateProcessor(new TextSelection(53, 3), s);

        delegateProcessor.setDelegateIdentifier("s");

        ProcessorDriver.drive(delegateProcessor);

        assertThat(s, hasSource("public class S {void m(){S s = new S();s.getClass(); s.m();}}"));
        assertThat(getProject(), hasNoInconsistencies());
    }

    @Test
    public void testAddSuperDelegateToSimpleMethodInvocation() throws JavaModelException {
        final String source = "public class S {void m(){S s; m();}}";

        final ICompilationUnit s = createCompilationUnit("", "S.java", source);

        final DelegateProcessor delegateProcessor = new DelegateProcessor(new TextSelection(30, 3), s);

        delegateProcessor.setDelegateIdentifier("super");

        ProcessorDriver.drive(delegateProcessor);

        assertThat(s, hasSource("public class S {void m(){S s; super.m();}}"));
        assertThat(getProject(), hasNoInconsistencies());
    }

    @Test
    public void testDelegateToField() throws JavaModelException {
        final String source = "public class S {S s; void m(){s.getClass(); m();}}";

        final ICompilationUnit s = createCompilationUnit("", "S.java", source);

        final DelegateProcessor delegateProcessor = new DelegateProcessor(new TextSelection(44, 3), s);

        delegateProcessor.setDelegateIdentifier("s");

        ProcessorDriver.drive(delegateProcessor);

        assertThat(s, hasSource("public class S {S s; void m(){s.getClass(); s.m();}}"));
        assertThat(getProject(), hasNoInconsistencies());
    }

    @Test
    public void testDelegateToFieldInSuperClassWithPackages() throws JavaModelException {
        // String superSource =
        // "package t; public class Super {Super s; void m() { } }";

        final String subSource = "package t; public class Sub extends Super {void g() {m();}}";

        // ICompilationUnit superCompilationUnit = createNewCompilationUnit("t",
        // "Super.java", superSource);
        final ICompilationUnit sub = createCompilationUnit("t", "Sub.java", subSource);

        final DelegateProcessor delegateProcessor = new DelegateProcessor(new TextSelection(
                "package t; public class Sub extends Super {void g() {".length(), "m()".length()), sub);

        delegateProcessor.setDelegateIdentifier("s");

        ProcessorDriver.drive(delegateProcessor);

        assertThat(sub, hasSource("package t; public class Sub extends Super {void g() {s.m();}}"));
        assertThat(getProject(), hasNoInconsistencies());
    }

    @Test
    public void testRemoveDelegateFromSimpleMethodInvocation() throws JavaModelException {
        final String source = "public class S {void m(){S s; s.m();}}";

        final ICompilationUnit s = createCompilationUnit("", "S.java", source);

        final DelegateProcessor delegateProcessor = new DelegateProcessor(new TextSelection(30, 5), s);

        delegateProcessor.setDelegateIdentifier("");
        final RefactoringStatus status = ProcessorDriver.drive(delegateProcessor);

        assertThat(status.getSeverity(), is(lessThan(RefactoringStatus.ERROR)));
        assertThat(s, hasSource("public class S {void m(){S s; m();}}"));
        assertThat(getProject(), hasNoInconsistencies());
    }
}
