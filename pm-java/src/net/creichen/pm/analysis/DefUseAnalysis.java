package net.creichen.pm.analysis;

import static net.creichen.pm.utils.Constants.SKIP_CHILDREN;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import net.creichen.pm.analysis.reachingdefs.ReachingDefsAnalysis;
import net.creichen.pm.api.PMCompilationUnit;
import net.creichen.pm.models.defuse.DefUseModel;
import net.creichen.pm.models.defuse.Use;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.MethodDeclaration;

public class DefUseAnalysis {

    private final class ReachingDefsAnalyzer extends ASTVisitor {
        private Set<Use> uses = new HashSet<Use>();

        @Override
        public boolean visit(final MethodDeclaration methodDeclaration) {
            // There is nothing to analyze if we have an interface or
            // abstract method
            if (methodDeclaration.getBody() != null) {
                final ReachingDefsAnalysis analysis = new ReachingDefsAnalysis(methodDeclaration);
                this.uses.addAll(analysis.getUses());
            }

            return SKIP_CHILDREN;
        }

        public Set<Use> getResults() {
            return this.uses;
        }
    }

    private final Set<Use> uses;

    public final Set<Use> getUses() {
        return this.uses;
    }

    public DefUseAnalysis(final Collection<PMCompilationUnit> compilationUnits) {
        ReachingDefsAnalyzer analyzer = new ReachingDefsAnalyzer();
        for (final PMCompilationUnit compilationUnit : compilationUnits) {
            compilationUnit.accept(analyzer);
        }
        this.uses = analyzer.getResults();
    }

    public DefUseModel getModel() {
        return new DefUseModel(this.uses);
    }

}
