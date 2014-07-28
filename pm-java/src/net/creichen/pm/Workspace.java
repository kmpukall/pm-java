/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm;

import java.util.HashMap;
import java.util.Map;

import net.creichen.pm.checkers.ConsistencyValidator;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.text.edits.TextEdit;

public final class Workspace {
    private static Workspace sharedWorkspace = null;

    public static void applyRewrite(final ASTRewrite rewrite, final ICompilationUnit iCompilationUnit) {
        try {
            final TextEdit astEdit = rewrite.rewriteAST();

            final ITextFileBufferManager bufferManager = FileBuffers.getTextFileBufferManager();
            final IPath path = iCompilationUnit.getPath();
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

    public static synchronized Workspace sharedWorkspace() {

        if (sharedWorkspace == null) {
            sharedWorkspace = new Workspace();
        }

        return sharedWorkspace;
    }

    private final Map<IJavaProject, Project> projectMapping;

    private Workspace() {
        this.projectMapping = new HashMap<IJavaProject, Project>();

    }

    public synchronized Project projectForIJavaProject(final IJavaProject iJavaProject) {

        Project result = this.projectMapping.get(iJavaProject);

        if (result == null) {
            result = new Project(iJavaProject);
            this.projectMapping.put(iJavaProject, result);
            ConsistencyValidator.getInstance().reset();
        }

        return result;
    }

    public synchronized void removeProjectForIJavaProject(final IJavaProject iJavaProject) {
        this.projectMapping.remove(iJavaProject);
    }

}
