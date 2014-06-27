/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm;

import java.util.HashMap;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jface.text.IDocument;
import org.eclipse.text.edits.TextEdit;

public class PMWorkspace {
    private static PMWorkspace sharedWorkspace = null;

    static public void applyRewrite(final ASTRewrite rewrite,
            final ICompilationUnit iCompilationUnit) {
        try {
            final TextEdit astEdit = rewrite.rewriteAST();

            final ITextFileBufferManager bufferManager = FileBuffers.getTextFileBufferManager();
            final IPath path = iCompilationUnit.getPath();
            try {
                bufferManager.connect(path, LocationKind.IFILE, null);
                final ITextFileBuffer textFileBuffer = bufferManager.getTextFileBuffer(path,
                        LocationKind.IFILE);

                final IDocument document = textFileBuffer.getDocument();

                astEdit.apply(document);

                textFileBuffer.commit(null /* ProgressMonitor */, false /* Overwrite */);
            } finally {
                bufferManager.disconnect(path, LocationKind.IFILE, null);
            }

        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    public static synchronized PMWorkspace sharedWorkspace() {

        if (sharedWorkspace == null) {
            sharedWorkspace = new PMWorkspace();
        }

        return sharedWorkspace;
    }

    private final HashMap<IJavaProject, PMProject> projectMapping;

    private PMWorkspace() {
        this.projectMapping = new HashMap<IJavaProject, PMProject>();

    }

    public synchronized PMProject projectForIJavaProject(final IJavaProject iJavaProject) {

        PMProject result = this.projectMapping.get(iJavaProject);

        if (result == null) {
            result = new PMProject(iJavaProject);
            this.projectMapping.put(iJavaProject, result);

        }

        return result;
    }

    public synchronized void removeProjectForIJavaProject(final IJavaProject iJavaProject) {
        this.projectMapping.remove(iJavaProject);
    }

}
