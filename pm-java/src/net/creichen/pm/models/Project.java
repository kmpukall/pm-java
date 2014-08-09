/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm.models;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.creichen.pm.analysis.ASTMatcher;
import net.creichen.pm.analysis.ASTQuery;
import net.creichen.pm.analysis.NodeReferenceStore;
import net.creichen.pm.analysis.RDefsAnalysis;
import net.creichen.pm.analysis.Use;
import net.creichen.pm.api.PMCompilationUnit;
import net.creichen.pm.utils.Timer;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTRequestor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jface.text.ITextSelection;

public class Project {

    private class PMCompilationUnitImplementation implements PMCompilationUnit {

        private byte[] sourceDigest;

        private CompilationUnit compilationUnit;
        private ICompilationUnit iCompilationUnit;

        public PMCompilationUnitImplementation(final ICompilationUnit iCompilationUnit,
                final CompilationUnit compilationUnit) {
            updatePair(iCompilationUnit, compilationUnit);
        }

        @Override
        public CompilationUnit getCompilationUnit() {
            return this.compilationUnit;
        }

        @Override
        public ICompilationUnit getICompilationUnit() {
            return this.iCompilationUnit;
        }

        @Override
        public String getSource() {
            try {
                return this.iCompilationUnit.getSource();
            } catch (final JavaModelException e) {
                e.printStackTrace();
                return null;
            }
        }

        // we parse more than one compilation unit at once (since this makes it
        // faster) in project and then pass
        // the newly parsed ast to to the pmcompilationunit with this method.

        @Override
        public void rename(final String newName) {
            try {
                final IPackageFragment parentPackageFragment = (IPackageFragment) this.iCompilationUnit.getParent();
                Project.this.pmCompilationUnits.remove(this.iCompilationUnit.getHandleIdentifier());
                this.iCompilationUnit.rename(newName + ".java", false, null);
                final ICompilationUnit newICompilationUnit = parentPackageFragment
                        .getCompilationUnit(newName + ".java");
                this.iCompilationUnit = newICompilationUnit;
                Project.this.pmCompilationUnits.put(newICompilationUnit.getHandleIdentifier(), this);
            } catch (final JavaModelException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }

        public boolean isSourceUnchanged() {
            return Arrays.equals(calculateHashForSource(getSource()), this.sourceDigest);
        }

        private byte[] calculateHashForSource(final String source) {
            try {
                final MessageDigest digest = java.security.MessageDigest.getInstance("SHA");
                digest.update(source.getBytes()); // Encoding issues here?
                return digest.digest();
            } catch (final NoSuchAlgorithmException e) {
                // this shouldn't happen in a sane environment
                e.printStackTrace();
                return null;
            }
        }

        private void updateHash(final String source) {
            this.sourceDigest = calculateHashForSource(source);
        }

        protected void updatePair(final ICompilationUnit iCompilationUnit, final CompilationUnit compilationUnit) {
            this.compilationUnit = compilationUnit;
            this.iCompilationUnit = iCompilationUnit;

            updateHash(getSource());
        }
    }

    // Wow, there are two of these
    // Need to clean this up.

    private final IJavaProject iJavaProject;

    private final Map<String, PMCompilationUnitImplementation> pmCompilationUnits; // keyed
    // off
    // ICompilationUnit.getHandleIdentifier

    private final List<ProjectListener> projectListeners;

    private DefUseModel udModel;

    private NameModel nameModel;

    public Project(final IJavaProject iJavaProject) {
        this.iJavaProject = iJavaProject;
        this.pmCompilationUnits = new HashMap<String, PMCompilationUnitImplementation>();
        this.projectListeners = new ArrayList<ProjectListener>();
        updateModelData(true);

    }

    public ASTNode findDeclaringNodeForName(final Name nameNode) {
        final CompilationUnit usingCompilationUnit = (CompilationUnit) nameNode.getRoot();
        final IBinding nameBinding = nameNode.resolveBinding();

        // It appears that name nodes like m in foo.m() have nil bindings here
        // (but not always??)
        // we'll want to do the analysis through the method invocation's
        // resolveMethodBinding() to catch capture here
        // in the future
        if (nameBinding != null) {
            final IJavaElement elementForBinding = nameBinding.getJavaElement();

            // Some name's bindings may not not have java elements (e.g.
            // "length" in an array.length)
            // For now we ignore these, but in the future we need a way to make
            // sure that array hasn't
            // been switched to have another type that also has a "length"
            // element

            if (elementForBinding != null) {
                // System.out.println("java elementForBinding for " + nameNode +
                // " in " + nameNode.getParent().getClass().getName() + " is " +
                // elementForBinding);

                final ICompilationUnit declaringICompilationUnit = (ICompilationUnit) elementForBinding
                        .getAncestor(IJavaElement.COMPILATION_UNIT);

                // we may not have the source to declaring compilation unit
                // (e.g. for System.out.println())
                // in this case file-level representation would be an
                // IClassFile, not an ICompilation unit
                // in this case we return null since we can't get an ASTNode
                // from an IClassFile

                if (declaringICompilationUnit != null) {
                    final CompilationUnit declaringCompilationUnit = getCompilationUnitForICompilationUnit(declaringICompilationUnit);
                    ASTNode declaringNode = declaringCompilationUnit.findDeclaringNode(nameBinding);

                    if (declaringNode == null) {
                        declaringNode = usingCompilationUnit.findDeclaringNode(nameNode.resolveBinding().getKey());
                    }

                    return declaringNode;
                }
            }
        }
        return null;

    }

    public PMCompilationUnit findPMCompilationUnitForNode(final ASTNode node) {
        return this.pmCompilationUnits.get(((ICompilationUnit) ((CompilationUnit) node.getRoot()).getJavaElement())
                .getHandleIdentifier());
    }

    public Collection<ASTNode> getASTRoots() {
        final Collection<ASTNode> roots = new HashSet<ASTNode>();
        for (final PMCompilationUnit pmCompilationUnit : this.pmCompilationUnits.values()) {
            roots.add(pmCompilationUnit.getCompilationUnit());
        }
        return roots;
    }

    public CompilationUnit getCompilationUnitForICompilationUnit(final ICompilationUnit iCompilationUnit) {
        return this.pmCompilationUnits.get(iCompilationUnit.getHandleIdentifier()).getCompilationUnit();
    }

    public Set<ICompilationUnit> getICompilationUnits() {
        return getSourceFilesForProject(this.iJavaProject);
    }

    public IJavaProject getIJavaProject() {
        return this.iJavaProject;
    }

    public NameModel getNameModel() {
        return this.nameModel;
    }

    public PMCompilationUnit getPMCompilationUnitForICompilationUnit(final ICompilationUnit iCompilationUnit) {
        return this.pmCompilationUnits.get(iCompilationUnit.getHandleIdentifier());
    }

    public Collection<PMCompilationUnit> getPMCompilationUnits() {
        final Collection<PMCompilationUnit> result = new HashSet<PMCompilationUnit>();
        result.addAll(this.pmCompilationUnits.values());
        return result;
    }

    public DefUseModel getUDModel() {
        return this.udModel;
    }

    // For measurement purposes only
    public void justParseMeasurement(final boolean resolveBindings) {
        final Set<ICompilationUnit> iCompilationUnits = getSourceFilesForProject(this.iJavaProject);

        final ASTParser parser = ASTParser.newParser(AST.JLS4);
        parser.setProject(this.iJavaProject);
        parser.setResolveBindings(resolveBindings);
        parser.createASTs(iCompilationUnits.toArray(new ICompilationUnit[iCompilationUnits.size()]), new String[0],
                new ASTRequestor() {
                    @Override
                    public void acceptAST(final ICompilationUnit source, final CompilationUnit ast) {
                    }
                }, null);

    }

    public ASTNode nodeForSelection(final ITextSelection selection, final ICompilationUnit iCompilationUnit) {
        final CompilationUnit compilationUnit = getCompilationUnitForICompilationUnit(iCompilationUnit);

        final ASTNode selectedNode = ASTQuery.findNodeForSelection(selection.getOffset(),
                selection.getLength(), compilationUnit);

        return selectedNode;
    }

    public boolean recursivelyReplaceNodeWithCopy(final ASTNode node, final ASTNode copy) {
        Timer.sharedTimer().start("NODE_REPLACEMENT");

        // It's kind of silly that we have to match twice
        final ASTMatcher astMatcher = new ASTMatcher(node, copy);
        final boolean matches = astMatcher.matches();
        if (matches) {
            final Map<ASTNode, ASTNode> isomorphicNodes = astMatcher.isomorphicNodes();
            for (final ASTNode oldNode : isomorphicNodes.keySet()) {
                final ASTNode newNode = isomorphicNodes.get(oldNode);
                if (oldNode instanceof SimpleName) {
                    this.nameModel.replaceNameWithName((SimpleName) oldNode, (SimpleName) newNode);
                }
                NodeReferenceStore.getInstance().replaceOldNodeVersionWithNewVersion(oldNode, newNode);
            }
        } else {
            System.err.println("Copy [" + copy + "] does not structurally match original [" + node + "]");
            throw new RuntimeException("Copy not does structurally match original");
        }

        Timer.sharedTimer().stop("NODE_REPLACEMENT");
        return matches;
    }

    public boolean sourcesAreOutOfSync() {
        for (final ICompilationUnit iCompilationUnit : getSourceFilesForProject(this.iJavaProject)) {
            if (!((PMCompilationUnitImplementation) getPMCompilationUnitForICompilationUnit(iCompilationUnit))
                    .isSourceUnchanged()) {
                return true;
            }
        }

        return false;
    }

    public void syncSources() {
        if (sourcesAreOutOfSync()) {
            updateModelData(true);
        }
    }

    public void updateToNewVersionsOfICompilationUnits() {
        updateModelData(false);
    }

    private Set<ICompilationUnit> allKnownICompilationUnits() {
        final Set<ICompilationUnit> result = new HashSet<ICompilationUnit>();

        for (final PMCompilationUnit pmCompilationUnit : getPMCompilationUnits()) {
            result.add(pmCompilationUnit.getICompilationUnit());
        }

        return result;
    }

    private void resetModel() {
        Collection<Use> currentUses = RDefsAnalysis.getCurrentUses(getASTRoots());
        this.udModel = new DefUseModel(currentUses);
        this.nameModel = new NameModel(this);
    }

    private void updateModelData(final boolean reset) {
        final Set<ICompilationUnit> iCompilationUnits = getSourceFilesForProject(this.iJavaProject);
        final Set<ICompilationUnit> previouslyKnownCompilationUnits = allKnownICompilationUnits();

        final boolean resetAll;
        // In future we will be smarter about detecting add/remove of
        // compilation units
        // and updating the models accordingly
        // for now we punt and have this reset the model
        if (!reset && !iCompilationUnits.equals(previouslyKnownCompilationUnits)) {
            System.err
                    .println("Previously known ICompilationUnits does not match current ICompilationUnits so resetting!!!");

            this.pmCompilationUnits.clear();
            resetAll = true;
        } else {
            resetAll = reset;
        }

        final ASTParser parser = ASTParser.newParser(AST.JLS4);
        parser.setProject(this.iJavaProject);

        parser.setResolveBindings(true);

        final ASTRequestor requestor = new ASTRequestor() {
            @Override
            public void acceptAST(final ICompilationUnit source, final CompilationUnit newCompilationUnit) {
                Timer.sharedTimer().start("PARSE_INTERNAL");

                PMCompilationUnitImplementation pmCompilationUnit = Project.this.pmCompilationUnits.get(source
                        .getHandleIdentifier());

                // We don't handle deletions yet
                if (pmCompilationUnit == null) {
                    pmCompilationUnit = new PMCompilationUnitImplementation(source, newCompilationUnit);
                    Project.this.pmCompilationUnits.put(source.getHandleIdentifier(), pmCompilationUnit);
                }

                if (!resetAll) {
                    final CompilationUnit oldCompilationUnit = getCompilationUnitForICompilationUnit(source);

                    if (recursivelyReplaceNodeWithCopy(oldCompilationUnit, newCompilationUnit)) {
                        pmCompilationUnit.updatePair(source, newCompilationUnit);

                    } else {
                        System.err.println("Couldn't update to new version of compilation unit!");
                        System.err.println("Old compilation unit: " + oldCompilationUnit);
                        System.err.println("New compilation unit: " + newCompilationUnit);

                        resetModel();
                    }

                }

                Timer.sharedTimer().stop("PARSE_INTERNAL");
            }
        };

        parser.createASTs(iCompilationUnits.toArray(new ICompilationUnit[iCompilationUnits.size()]), new String[0],
                requestor, null);

        if (resetAll) {
            resetModel();
        }

        for (final ProjectListener listener : this.projectListeners) {
            listener.projectDidReparse(this);
        }

    }

    private static Set<ICompilationUnit> getSourceFilesForProject(final IJavaProject iJavaProject) {
        final Set<ICompilationUnit> result = new HashSet<ICompilationUnit>();
        try {
            for (final IPackageFragment packageFragment : iJavaProject.getPackageFragments()) {
                if (packageFragment.getKind() == IPackageFragmentRoot.K_SOURCE
                        && packageFragment.containsJavaResources()) {
                    for (final ICompilationUnit iCompilationUnit : packageFragment.getCompilationUnits()) {

                        result.add(iCompilationUnit);
                    }

                }
            }
        } catch (final JavaModelException e) {
            e.printStackTrace();
        }
        return result;
    }

}
