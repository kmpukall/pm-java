/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm.steps;

import java.util.Map;

import net.creichen.pm.PMProject;
import net.creichen.pm.PMWorkspace;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.NullChange;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.text.edits.TextEdit;

public class PMStep {
    public class PMCompositeChange extends CompositeChange {
        public PMCompositeChange(final String name) {
            super(name);
        }

        @Override
        public Change perform(final IProgressMonitor pm) throws CoreException {

            final Change result = super.perform(pm);

            performASTChange();

            getProject().updateToNewVersionsOfICompilationUnits();

            updateAfterReparse();

            cleanup();

            getProject().rescanForInconsistencies();

            return result;
        }

    }

    public class PMTextFileChange extends TextFileChange {
        public PMTextFileChange(final String name, final IFile file) {
            super(name, file);
        }

        // This will do the text change but not the ast changes

        @Override
        public Change perform(final IProgressMonitor pm) throws CoreException {

            // In future, we might as well do the text replacement parts of the
            // change ourselves, too (since this will make sure that we do the
            // same thing in all situations), but for now we let the superclass
            // do it
            final Change result = super.perform(pm);

            return result;
        }

    }

    // need method to test for errors before asking for changes

    private final PMProject project;

    PMStep(final PMProject project) {
        this.project = project;
    }

    public void applyAllAtOnce() {

        final Map<ICompilationUnit, ASTRewrite> rewrites = calculateTextualChange();

        for (final ICompilationUnit compilationUnitToRewrite : rewrites.keySet()) {
            PMWorkspace.applyRewrite(rewrites.get(compilationUnitToRewrite),
                    compilationUnitToRewrite);
        }

        performASTChange();

        this.project.updateToNewVersionsOfICompilationUnits();

        updateAfterReparse();

        cleanup();

        this.project.rescanForInconsistencies();
    }

    public Map<ICompilationUnit, ASTRewrite> calculateTextualChange() {
        return null;
    }

    public void cleanup() {
        // called regardless of whether updateAfterReparse() was called
    }

    public Change createCompositeChange(final String changeDescription) {

        final Map<ICompilationUnit, ASTRewrite> rewrites = calculateTextualChange();

        Change result = new NullChange();

        try {
            if (rewrites.size() > 0) {
                final CompositeChange combinedChange = new PMCompositeChange(changeDescription);

                for (final ICompilationUnit compilationUnitToChange : rewrites.keySet()) {
                    final ASTRewrite rewrite = rewrites.get(compilationUnitToChange);

                    final TextEdit astEdit = rewrite.rewriteAST();

                    final TextFileChange localChange = new PMTextFileChange(changeDescription,
                            (IFile) compilationUnitToChange.getResource());

                    localChange.setTextType("java");
                    localChange.setEdit(astEdit);

                    combinedChange.add(localChange);
                }

                result = combinedChange;
            }

        } catch (final Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    PMProject getProject() {
        return this.project;
    }

    public void performASTChange() {

    }

    public void updateAfterReparse() {

    }

}
