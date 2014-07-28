package net.creichen.pm.checkers;

import java.util.HashSet;
import java.util.Set;

import net.creichen.pm.api.PMCompilationUnit;
import net.creichen.pm.inconsistencies.Inconsistency;
import net.creichen.pm.inconsistencies.NameCapture;
import net.creichen.pm.inconsistencies.NameConflict;
import net.creichen.pm.inconsistencies.UnknownName;
import net.creichen.pm.models.NameModel;
import net.creichen.pm.models.Project;
import net.creichen.pm.utils.ASTUtil;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.SimpleName;

public class NameModelConsistencyCheck {

    private Project project;
    private NameModel model;

    public NameModelConsistencyCheck(final Project project) {
        this.project = project;
    }

    public Set<Inconsistency> calculateInconsistencies(final NameModel model) {
        this.model = model;
        final Set<Inconsistency> inconsistencies = new HashSet<Inconsistency>();
        for (final PMCompilationUnit compilationUnit : this.project.getPMCompilationUnits()) {
            inconsistencies.addAll(findInconsistenciesInCompilationUnit(compilationUnit));
        }
        return inconsistencies;
    }

    private Set<Inconsistency> findInconsistenciesInCompilationUnit(final PMCompilationUnit pmCompilationUnit) {
        final Set<Inconsistency> inconsistencies = new HashSet<Inconsistency>();
        final CompilationUnit compilationUnit = pmCompilationUnit.getCompilationUnit();

        final Set<SimpleName> simpleNamesInCompilationUnit = simpleNamesInCompilationUnit(compilationUnit);

        for (final SimpleName simpleName : simpleNamesInCompilationUnit) {

            final ASTNode declaringNode = this.project.findDeclaringNodeForName(simpleName);

            if (declaringNode != null) {
                final SimpleName declaringSimpleName = ASTUtil.simpleNameForDeclaringNode(declaringNode);

                final String declaringIdentifier = this.model.identifierForName(declaringSimpleName);

                final String usingIdentifier = this.model.identifierForName(simpleName);

                if (usingIdentifier == null) {
                    inconsistencies.add(new UnknownName(pmCompilationUnit, simpleName));
                } else {
                    if (declaringIdentifier != usingIdentifier || !declaringIdentifier.equals(usingIdentifier)) {
                        inconsistencies.add(new NameCapture(this.project, pmCompilationUnit, simpleName, null,
                                declaringNode));
                    }
                }
                if (!declaringSimpleName.getIdentifier().equals(simpleName.getIdentifier())) {
                    inconsistencies.add(new NameConflict(pmCompilationUnit, simpleName, declaringSimpleName
                            .getIdentifier()));
                }
            }
        }

        return inconsistencies;
    }

    private static Set<SimpleName> simpleNamesInCompilationUnit(final CompilationUnit compilationUnit) {
        final Set<SimpleName> result = new HashSet<SimpleName>();

        compilationUnit.accept(new ASTVisitor() {
            @Override
            public boolean visit(final SimpleName node) {
                result.add(node);

                return true;
            }
        });

        return result;
    }
}
