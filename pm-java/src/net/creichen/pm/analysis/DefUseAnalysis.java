package net.creichen.pm.analysis;

import static net.creichen.pm.utils.Constants.SKIP_CHILDREN;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import net.creichen.pm.api.PMCompilationUnit;
import net.creichen.pm.models.defuse.DefUseModel;
import net.creichen.pm.models.defuse.Use;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.MethodDeclaration;

public class DefUseAnalysis {

    private final Set<Use> uses;

    public final Set<Use> getUses() {
        return this.uses;
    }

    public DefUseAnalysis(final Collection<PMCompilationUnit> compilationUnits) {
        this.uses = new HashSet<Use>();
        for (final PMCompilationUnit compilationUnit : compilationUnits) {
            compilationUnit.accept(new ASTVisitor() {
                @Override
                public boolean visit(final MethodDeclaration methodDeclaration) {

                    // There is nothing to analyze if we have an interface or
                    // abstract method
                    if (methodDeclaration.getBody() != null) {
                        final ReachingDefsAnalysis analysis = new ReachingDefsAnalysis(methodDeclaration);
                        DefUseAnalysis.this.uses.addAll(analysis.getUses());
                    }

                    return SKIP_CHILDREN;
                }
            });

        }
    }

    public DefUseModel getModel() {
        return new DefUseModel(this.uses);
    }

}
