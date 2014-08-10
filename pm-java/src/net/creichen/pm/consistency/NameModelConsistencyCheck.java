package net.creichen.pm.consistency;

import java.util.HashSet;
import java.util.Set;

import net.creichen.pm.api.PMCompilationUnit;
import net.creichen.pm.consistency.inconsistencies.Inconsistency;
import net.creichen.pm.consistency.inconsistencies.NameCapture;
import net.creichen.pm.consistency.inconsistencies.NameConflict;
import net.creichen.pm.consistency.inconsistencies.UnknownName;
import net.creichen.pm.core.Project;
import net.creichen.pm.models.NameModel;
import net.creichen.pm.utils.ASTQuery;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.SimpleName;

class NameModelConsistencyCheck {

    private Project project;

    public NameModelConsistencyCheck(final Project project) {
        this.project = project;
    }

    public Set<Inconsistency> calculateInconsistencies(final NameModel model) {
        final Set<Inconsistency> inconsistencies = new HashSet<Inconsistency>();
        for (final PMCompilationUnit compilationUnit : this.project.getPMCompilationUnits()) {
            inconsistencies.addAll(findInconsistenciesInCompilationUnit(model, compilationUnit));
        }
        return inconsistencies;
    }

    private Set<Inconsistency> findInconsistenciesInCompilationUnit(final NameModel nameModel,
            final PMCompilationUnit pmCompilationUnit) {
        final Set<Inconsistency> inconsistencies = new HashSet<Inconsistency>();
        final CompilationUnit compilationUnit = pmCompilationUnit.getCompilationUnit();

        final Set<SimpleName> simpleNamesInCompilationUnit = simpleNamesInCompilationUnit(compilationUnit);

        for (final SimpleName simpleName : simpleNamesInCompilationUnit) {

            final ASTNode declaringNode = this.project.findDeclaringNodeForName(simpleName);

            if (declaringNode != null) {
                final SimpleName declaringSimpleName = ASTQuery.getSimpleName(declaringNode);

                final String declaringIdentifier = nameModel.getIdentifierForName(declaringSimpleName);

                final String usingIdentifier = nameModel.getIdentifierForName(simpleName);

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
