package net.creichen.pm.core;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import net.creichen.pm.api.PMCompilationUnit;
import net.creichen.pm.consistency.inconsistencies.Inconsistency;
import net.creichen.pm.ui.MarkerResolutionGenerator;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;

public class PMCompilationUnitImpl implements PMCompilationUnit {

    private byte[] sourceDigest;

    private CompilationUnit compilationUnit;
    private ICompilationUnit iCompilationUnit;

    public PMCompilationUnitImpl(final ICompilationUnit iCompilationUnit, final CompilationUnit compilationUnit) {
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
            this.iCompilationUnit.rename(newName + ".java", false, null);
            this.iCompilationUnit = parentPackageFragment.getCompilationUnit(newName + ".java");
        } catch (final JavaModelException e) {
            e.printStackTrace();
            throw new PMException(e);
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

    @Override
    public void updatePair(final ICompilationUnit iCompilationUnit, final CompilationUnit compilationUnit) {
        this.compilationUnit = compilationUnit;
        this.iCompilationUnit = iCompilationUnit;

        updateHash(getSource());
    }

    @Override
    public void createMarker(final Inconsistency inconsistency, final IJavaProject iJavaProject) throws CoreException {
        IResource resource = this.iCompilationUnit.getResource();
        final IMarker marker = resource.createMarker("org.eclipse.core.resources.problemmarker");

        marker.setAttribute(MarkerResolutionGenerator.INCONSISTENCY_ID, inconsistency.getID());
        marker.setAttribute(MarkerResolutionGenerator.PROJECT_ID, iJavaProject.getHandleIdentifier());
        marker.setAttribute(MarkerResolutionGenerator.ACCEPTS_BEHAVIORAL_CHANGE,
                inconsistency.allowsAcceptBehavioralChange());
        marker.setAttribute(IMarker.MESSAGE, inconsistency.getHumanReadableDescription());
        marker.setAttribute(IMarker.TRANSIENT, true);

        final ASTNode node = inconsistency.getNode();
        marker.setAttribute(IMarker.CHAR_START, node.getStartPosition());
        marker.setAttribute(IMarker.CHAR_END, node.getStartPosition() + node.getLength());
    }

    @Override
    public String getHandleIdentifier() {
        return this.iCompilationUnit.getHandleIdentifier();
    }

    @Override
    public void accept(ASTVisitor visitor) {
        this.compilationUnit.accept(visitor);
    }

    @Override
    public IProblem[] getProblems() {
        return this.compilationUnit.getProblems();
    }
}