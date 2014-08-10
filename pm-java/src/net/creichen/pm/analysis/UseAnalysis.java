package net.creichen.pm.analysis;

import java.util.Collection;
import java.util.HashSet;

import net.creichen.pm.utils.Timer;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.MethodDeclaration;

public class UseAnalysis {

    private final Collection<ASTNode> roots;

    public UseAnalysis(final Collection<ASTNode> roots) {
        this.roots = roots;
    }

    public Collection<Use> getCurrentUses() {
        Timer.sharedTimer().start("DUUD_CHAINS");

        final Collection<Use> uses = new HashSet<Use>();
        for (final ASTNode root : this.roots) {
            root.accept(new ASTVisitor() {
                @Override
                public boolean visit(final MethodDeclaration methodDeclaration) {

                    // There is nothing to analyze if we have an interface or
                    // abstract method
                    if (methodDeclaration.getBody() != null) {
                        final ReachingDefsAnalysis analysis = new ReachingDefsAnalysis(methodDeclaration);
                        uses.addAll(analysis.getUses());
                    }

                    return false; // don't visit children
                }
            });

        }
        Timer.sharedTimer().stop("DUUD_CHAINS");
        return uses;
    }

}
