/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm.steps;

import java.util.Collections;
import java.util.Map;

import net.creichen.pm.api.PMCompilationUnit;
import net.creichen.pm.consistency.ConsistencyValidator;
import net.creichen.pm.core.Project;
import net.creichen.pm.utils.ASTUtil;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.NullChange;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.text.edits.TextEdit;

abstract class AbstractStep {
    private final class PMCompositeChange extends CompositeChange {
        private PMCompositeChange(final String name) {
            super(name);
        }

        @Override
        public Change perform(final IProgressMonitor pm) throws CoreException {
            final Change result = super.perform(pm);
            performASTChange();
            getProject().update();
            ConsistencyValidator.getInstance().reset();
            updateAfterReparse();
            ConsistencyValidator.getInstance().rescanForInconsistencies(getProject());
            return result;
        }

    }

    // need method to test for errors before asking for changes

    private final Project project;

    AbstractStep(final Project project) {
        this.project = project;
    }

    public void apply() {

        final Map<PMCompilationUnit, ASTRewrite> rewrites = calculateTextualChange();

        for (final PMCompilationUnit compilationUnitToRewrite : rewrites.keySet()) {
            ASTUtil.applyRewrite(rewrites.get(compilationUnitToRewrite), compilationUnitToRewrite);
        }

        performASTChange();

        this.project.update();
        ConsistencyValidator.getInstance().reset();

        updateAfterReparse();

        ConsistencyValidator.getInstance().rescanForInconsistencies(this.project);
    }

    public Map<PMCompilationUnit, ASTRewrite> calculateTextualChange() {
        return Collections.emptyMap();
    }

    public Change createCompositeChange(final String changeDescription) {

        final Map<PMCompilationUnit, ASTRewrite> rewrites = calculateTextualChange();

        Change result = new NullChange();

        try {
            if (rewrites.size() > 0) {
                final CompositeChange combinedChange = new PMCompositeChange(changeDescription);

                for (final PMCompilationUnit compilationUnitToChange : rewrites.keySet()) {
                    final ASTRewrite rewrite = rewrites.get(compilationUnitToChange);

                    final TextEdit astEdit = rewrite.rewriteAST();

                    final TextFileChange localChange = new TextFileChange(changeDescription,
                            (IFile) compilationUnitToChange.getResource());

                    localChange.setTextType("java");
                    localChange.setEdit(astEdit);

                    combinedChange.add(localChange);
                }

                result = combinedChange;
            }

        } catch (final JavaModelException e) {
            e.printStackTrace();
        }

        return result;
    }

    Project getProject() {
        return this.project;
    }

    public abstract void performASTChange();

    public void updateAfterReparse() {

    }

}
