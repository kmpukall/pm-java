/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm.core;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.creichen.pm.analysis.ASTMatcher;
import net.creichen.pm.analysis.DefUseAnalysis;
import net.creichen.pm.api.PMCompilationUnit;
import net.creichen.pm.data.NodeStore;
import net.creichen.pm.models.defuse.DefUseModel;
import net.creichen.pm.models.name.NameModel;
import net.creichen.pm.utils.ASTQuery;
import net.creichen.pm.utils.ASTUtil;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
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

    private final IJavaProject iJavaProject;

    private final Map<String, PMCompilationUnit> pmCompilationUnits; // keyed
    // off
    // ICompilationUnit.getHandleIdentifier

    private DefUseModel udModel;
    private NameModel nameModel;

    public Project(final IJavaProject iJavaProject) {
        this.iJavaProject = iJavaProject;
        this.pmCompilationUnits = new HashMap<String, PMCompilationUnit>();
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
                    final CompilationUnit declaringCompilationUnit = getPMCompilationUnit(declaringICompilationUnit)
                            .getCompilationUnit();
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

    public Set<ICompilationUnit> getICompilationUnits() {
        return ASTUtil.getSourceFilesForProject(this.iJavaProject);
    }

    public IJavaProject getIJavaProject() {
        return this.iJavaProject;
    }

    public NameModel getNameModel() {
        return this.nameModel;
    }

    public PMCompilationUnit getPMCompilationUnit(final ICompilationUnit iCompilationUnit) {
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

    public ASTNode nodeForSelection(final ITextSelection selection, final ICompilationUnit iCompilationUnit) {
        final CompilationUnit compilationUnit = getPMCompilationUnit(iCompilationUnit).getCompilationUnit();

        final ASTNode selectedNode = ASTQuery.findNodeForSelection(selection.getOffset(), selection.getLength(),
                compilationUnit);

        return selectedNode;
    }

    public boolean recursivelyReplaceNodeWithCopy(final ASTNode node, final ASTNode copy) {
        // It's kind of silly that we have to match twice
        final ASTMatcher astMatcher = new ASTMatcher(node, copy);
        final boolean matches = astMatcher.matches();
        if (matches) {
            final Map<ASTNode, ASTNode> isomorphicNodes = astMatcher.isomorphicNodes();
            for (final ASTNode oldNode : isomorphicNodes.keySet()) {
                final ASTNode newNode = isomorphicNodes.get(oldNode);
                if (oldNode instanceof SimpleName) {
                    this.nameModel.rename((SimpleName) oldNode, (SimpleName) newNode);
                }
                NodeStore.getInstance().replaceNode(oldNode, newNode);
            }
        } else {
            System.err.println("Copy [" + copy + "] does not structurally match original [" + node + "]");
            throw new PMException("Copy not does structurally match original");
        }

        return matches;
    }

    public boolean sourcesAreOutOfSync() {
        for (final ICompilationUnit iCompilationUnit : ASTUtil.getSourceFilesForProject(this.iJavaProject)) {
            if (!((PMCompilationUnitImpl) getPMCompilationUnit(iCompilationUnit)).isSourceUnchanged()) {
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

    private void resetModels() {
        this.udModel = new DefUseAnalysis(getPMCompilationUnits()).getModel();
        this.nameModel = new NameModel(getPMCompilationUnits());
    }

    private void updateModelData(final boolean reset) {
        final Set<ICompilationUnit> iCompilationUnits = ASTUtil.getSourceFilesForProject(this.iJavaProject);
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
                PMCompilationUnit pmCompilationUnit = Project.this.pmCompilationUnits.get(source.getHandleIdentifier());

                // We don't handle deletions yet
                if (pmCompilationUnit == null) {
                    pmCompilationUnit = new PMCompilationUnitImpl(source, newCompilationUnit);
                    Project.this.pmCompilationUnits.put(source.getHandleIdentifier(), pmCompilationUnit);
                }

                if (!resetAll) {
                    final CompilationUnit oldCompilationUnit = getPMCompilationUnit(source).getCompilationUnit();

                    if (recursivelyReplaceNodeWithCopy(oldCompilationUnit, newCompilationUnit)) {
                        pmCompilationUnit.updatePair(source, newCompilationUnit);

                    } else {
                        System.err.println("Couldn't update to new version of compilation unit!");
                        System.err.println("Old compilation unit: " + oldCompilationUnit);
                        System.err.println("New compilation unit: " + newCompilationUnit);

                        resetModels();
                    }
                }
            }
        };

        parser.createASTs(iCompilationUnits.toArray(new ICompilationUnit[iCompilationUnits.size()]), new String[0],
                requestor, null);

        if (resetAll) {
            resetModels();
        }

    }

    public void rename(PMCompilationUnit compilationUnit, String newName) {
        this.pmCompilationUnits.remove(compilationUnit.getHandleIdentifier());
        compilationUnit.rename(newName);
        this.pmCompilationUnits.put(compilationUnit.getHandleIdentifier(), compilationUnit);
    }

}
