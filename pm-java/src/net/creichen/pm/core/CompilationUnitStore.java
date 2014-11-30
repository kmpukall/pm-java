package net.creichen.pm.core;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.creichen.pm.api.PMCompilationUnit;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTRequestor;
import org.eclipse.jdt.core.dom.CompilationUnit;

public class CompilationUnitStore {

    private final Map<String, PMCompilationUnit> compilationUnits;
    private IJavaProject iJavaProject;

    /**
     * Private singleton constructor.
     */
    public CompilationUnitStore(IJavaProject iJavaProject) {
        this.iJavaProject = iJavaProject;
        this.compilationUnits = new HashMap<String, PMCompilationUnit>();
    }

    /**
     * Removes all compilation units from the store.
     */
    public void clear() {
        this.compilationUnits.clear();
    }

    /**
     * Returns the {@link PMCompilationUnit} that contains the given node.
     *
     * @param node
     *            any {@link ASTNode}
     * @return the containing unit.
     */
    public PMCompilationUnit forNode(final ASTNode node) {
        return this.compilationUnits.get(((ICompilationUnit) ((CompilationUnit) node.getRoot()).getJavaElement())
                .getHandleIdentifier());
    }

    /**
     * Returns the {@link PMCompilationUnit} associated with the given {@link ICompilationUnit}.
     *
     * @param iCompilationUnit
     * @return
     */
    public PMCompilationUnit get(final ICompilationUnit iCompilationUnit) {
        return this.compilationUnits.get(iCompilationUnit.getHandleIdentifier());
    }

    /**
     *
     * @return all {@link PMCompilationUnit}s in the store.
     */
    public Collection<PMCompilationUnit> getAll() {
        return this.compilationUnits.values();
    }

    void put(PMCompilationUnit compilationUnit) {
        this.compilationUnits.put(compilationUnit.getHandleIdentifier(), compilationUnit);
    }

    private void remove(PMCompilationUnit compilationUnit) {
        this.compilationUnits.remove(compilationUnit.getHandleIdentifier());
    }

    public void rename(PMCompilationUnit compilationUnit, String newName) {
        remove(compilationUnit);
        compilationUnit.rename(newName);
        put(compilationUnit);
    }

    public Set<ICompilationUnit> getSourceFiles() {
        final Set<ICompilationUnit> result = new HashSet<ICompilationUnit>();
        try {
            for (final IPackageFragment packageFragment : this.iJavaProject.getPackageFragments()) {
                if (packageFragment.getKind() == IPackageFragmentRoot.K_SOURCE
                        && packageFragment.containsJavaResources()) {
                    Collections.addAll(result, packageFragment.getCompilationUnits());

                }
            }
        } catch (final JavaModelException e) {
            e.printStackTrace();
        }
        return result;
    }

    void reset() {
        final Set<ICompilationUnit> sourceFiles = getSourceFiles();
        final ASTParser parser = ASTParser.newParser(AST.JLS4);
        parser.setProject(this.iJavaProject);
        parser.setResolveBindings(true);
        final ASTRequestor requestor = new ASTRequestor() {
            @Override
            public void acceptAST(final ICompilationUnit source, final CompilationUnit newCompilationUnit) {
                PMCompilationUnit pmCompilationUnit = CompilationUnitStore.this.get(source);

                // We don't handle deletions yet
                if (pmCompilationUnit == null) {
                    pmCompilationUnit = new PMCompilationUnitImpl(source, newCompilationUnit);
                    CompilationUnitStore.this.put(pmCompilationUnit);
                }
            }
        };
        parser.createASTs(sourceFiles.toArray(new ICompilationUnit[sourceFiles.size()]), new String[0], requestor, null);
    }

}
