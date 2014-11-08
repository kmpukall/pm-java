package net.creichen.pm.consistency;

import java.util.HashSet;
import java.util.Set;

import net.creichen.pm.api.PMCompilationUnit;
import net.creichen.pm.consistency.inconsistencies.Inconsistency;
import net.creichen.pm.consistency.inconsistencies.NameCapture;
import net.creichen.pm.consistency.inconsistencies.NameConflict;
import net.creichen.pm.consistency.inconsistencies.UnknownName;
import net.creichen.pm.core.Project;
import net.creichen.pm.models.name.NameModel;
import net.creichen.pm.utils.ASTQuery;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
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
            final PMCompilationUnit compilationUnit) {
        final Set<Inconsistency> inconsistencies = new HashSet<Inconsistency>();

        final Set<SimpleName> simpleNamesInCompilationUnit = simpleNamesInCompilationUnit(compilationUnit);

        for (final SimpleName simpleName : simpleNamesInCompilationUnit) {

            final ASTNode declaringNode = this.project.findDeclaringNodeForName(simpleName);

            if (declaringNode != null) {
                final SimpleName declaringSimpleName = ASTQuery.resolveSimpleName(declaringNode);

                final String declaringIdentifier = nameModel.getIdentifier(declaringSimpleName);

                final String usingIdentifier = nameModel.getIdentifier(simpleName);

                if (usingIdentifier == null) {
                    inconsistencies.add(new UnknownName(simpleName));
                } else {
                    if (declaringIdentifier != usingIdentifier || !declaringIdentifier.equals(usingIdentifier)) {
                        inconsistencies.add(new NameCapture(this.project, simpleName, null, declaringNode));
                    }
                }
                if (!declaringSimpleName.getIdentifier().equals(simpleName.getIdentifier())) {
                    inconsistencies.add(new NameConflict(simpleName, declaringSimpleName.getIdentifier()));
                }
            }
        }

        return inconsistencies;
    }

    private static Set<SimpleName> simpleNamesInCompilationUnit(final PMCompilationUnit compilationUnit) {
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
