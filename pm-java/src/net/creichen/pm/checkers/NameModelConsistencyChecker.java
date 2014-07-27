package net.creichen.pm.checkers;

import java.util.HashSet;
import java.util.Set;

import net.creichen.pm.Project;
import net.creichen.pm.api.PMCompilationUnit;
import net.creichen.pm.inconsistencies.Inconsistency;
import net.creichen.pm.inconsistencies.NameCapture;
import net.creichen.pm.inconsistencies.NameConflict;
import net.creichen.pm.inconsistencies.UnknownName;
import net.creichen.pm.models.NameModel;
import net.creichen.pm.utils.ASTUtil;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.SimpleName;

public class NameModelConsistencyChecker {

	private Project project;
	private NameModel model;

	public NameModelConsistencyChecker(final Project project) {
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

			// declaringModel.getCompilationUnit().findDeclaringNode(simpleName.resolveBinding());
			final ASTNode declaringNode = this.project.findDeclaringNodeForName(simpleName);

			if (declaringNode != null) {
				final SimpleName declaringSimpleName = ASTUtil.simpleNameForDeclaringNode(declaringNode);

				final String declaringIdentifier = this.model.identifierForName(declaringSimpleName);

				final String usingIdentifier = this.model.identifierForName(simpleName);

				if (usingIdentifier == null) {
					inconsistencies.add(new UnknownName(pmCompilationUnit, simpleName));
				} else {
					if (declaringIdentifier != usingIdentifier || !declaringIdentifier.equals(usingIdentifier)) {

						// System.err.println("Capture of " + simpleName +
						// " by " +
						// ((CompilationUnit)declaringSimpleName.getRoot()).getJavaElement());

						// System.err.println("Declaring identifier is " +
						// declaringIdentifier + " usingIdentifier is " +
						// usingIdentifier);

						// System.err.println("Declaring simpleName is " +
						// declaringSimpleName);
						// System.err.println("Using binding is " +
						// simpleName.resolveBinding().hashCode() + " of class "
						// + simpleName.resolveBinding().getClass());
						// System.err.println("Declaring binding is " +
						// declaringSimpleName.resolveBinding().hashCode());

						// System.err.println("Using binding.getVariableDeclaration is "
						// +
						// ((IVariableBinding)simpleName.resolveBinding()).getVariableDeclaration().hashCode()
						// + " of class " +
						// simpleName.resolveBinding().getClass());
						// System.err.println("Declaring binding.getVariableDeclaration is "
						// +
						// ((IVariableBinding)declaringSimpleName.resolveBinding()).getVariableDeclaration().hashCode());

						// don't have quick way to figure out what the declaring
						// node should have been yet

						inconsistencies.add(new NameCapture(this.project, pmCompilationUnit, simpleName, null,
								declaringNode));
					}
				}

				if (!declaringSimpleName.getIdentifier().equals(simpleName.getIdentifier())) {
					inconsistencies.add(new NameConflict(pmCompilationUnit, simpleName, declaringSimpleName
							.getIdentifier()));
				}

			}
			// FIXME(dcc)
			// System.err.println("!!! ignoring inconsistencies for " +
			// simpleName + "  in " + iCompilationUnit.getHandleIdentifier()
			// + " because can't find declaring node");

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
