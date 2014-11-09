/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm.utils;

import static net.creichen.pm.utils.APIWrapperUtil.getStructuralProperty;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.creichen.pm.api.PMCompilationUnit;
import net.creichen.pm.core.PMException;
import net.creichen.pm.models.defuse.Def;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.ChildListPropertyDescriptor;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.text.edits.TextEdit;

public final class ASTUtil {

    private ASTUtil() {
        // private utility class constructor
    }

    public static void applyRewrite(final ASTRewrite rewrite, final PMCompilationUnit compilationUnitToRewrite) {
        try {
            final TextEdit astEdit = rewrite.rewriteAST();

            final ITextFileBufferManager bufferManager = FileBuffers.getTextFileBufferManager();
            final IPath path = compilationUnitToRewrite.getPath();
            try {
                bufferManager.connect(path, LocationKind.IFILE, null);
                final ITextFileBuffer textFileBuffer = bufferManager.getTextFileBuffer(path, LocationKind.IFILE);

                final IDocument document = textFileBuffer.getDocument();

                astEdit.apply(document);

                textFileBuffer.commit(null /* ProgressMonitor */, false /* Overwrite */);
            } finally {
                bufferManager.disconnect(path, LocationKind.IFILE, null);
            }

        } catch (final BadLocationException e) {
            e.printStackTrace();
        } catch (final CoreException e) {
            e.printStackTrace();
        }
    }

    public static IBinding getBinding(Def def) {
        Expression expression;
        ASTNode definingNode2 = def.getDefiningNode();
        if (definingNode2 instanceof Assignment) {
            expression = ((Assignment) definingNode2).getLeftHandSide();
        } else if (definingNode2 instanceof SingleVariableDeclaration) {
            expression = ((SingleVariableDeclaration) definingNode2).getName();
        } else if (definingNode2 instanceof VariableDeclarationFragment) {
            expression = ((VariableDeclarationFragment) definingNode2).getName();
        } else if (definingNode2 instanceof PostfixExpression) {
            expression = ((PostfixExpression) definingNode2).getOperand();
        } else if (definingNode2 instanceof PrefixExpression) {
            expression = ((PrefixExpression) definingNode2).getOperand();
        } else {
            throw new PMException("Un-handled _definingNode type " + definingNode2.getClass());
        }
        return findBindingForExpression(expression);
    }

    public static Set<ICompilationUnit> getSourceFilesForProject(final IJavaProject iJavaProject) {
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

    // We also consider parameters, for statement vars, and catch vars to be
    // local
    public static boolean isVariableDeclarationLocal(final VariableDeclaration declaration) {
        final ASTNode parent = declaration.getParent();

        // not sure this is actually the best way to do this
        return parent instanceof CatchClause || parent instanceof VariableDeclarationExpression
                || parent instanceof VariableDeclarationStatement || parent instanceof ForStatement;

    }

    public static void replaceNodeInParent(final ASTNode oldNode, final ASTNode replacement) {
        final StructuralPropertyDescriptor location = oldNode.getLocationInParent();

        // replace the selected method invocation with the new invocation
        if (location.isChildProperty()) {
            oldNode.getParent().setStructuralProperty(location, replacement);
        } else {
            final List<ASTNode> parentList = getStructuralProperty((ChildListPropertyDescriptor) location,
                    oldNode.getParent());

            parentList.set(parentList.indexOf(oldNode), replacement);
        }
    }

    private static IBinding findBindingForExpression(final Expression expression) {
        if (expression instanceof Name) {
            return ((Name) expression).resolveBinding();
        } else if (expression instanceof FieldAccess) {
            return ((FieldAccess) expression).resolveFieldBinding();
        } else {
            throw new PMException("Don't know how to find binding for " + expression.getClass().getSimpleName() + " ["
                    + expression + "]");
        }
    }
}
