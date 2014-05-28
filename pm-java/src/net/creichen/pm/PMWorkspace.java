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




public class PMWorkspace  {
	private static PMWorkspace _sharedWorkspace = null;
	
	public static synchronized PMWorkspace sharedWorkspace() {
		
		
		if (_sharedWorkspace == null)
			_sharedWorkspace = new PMWorkspace();
		
		return _sharedWorkspace;
	}
	
	
	
	
	private PMWorkspace() {
		_projectMapping = new HashMap<IJavaProject, PMProject>();
		
		
	}
	
	private  HashMap<IJavaProject, PMProject> _projectMapping;
	
	public synchronized PMProject projectForIJavaProject(IJavaProject iJavaProject) {
		
			
		
		PMProject result = _projectMapping.get(iJavaProject);
		
		if (result == null) {
			result = new PMProject(iJavaProject);
			_projectMapping.put(iJavaProject, result);
			

		}
		
		return result;
	}
	
	public synchronized void removeProjectForIJavaProject(IJavaProject iJavaProject) {
		_projectMapping.remove(iJavaProject);
	}
	

	
	

	static public void applyRewrite(ASTRewrite rewrite, ICompilationUnit iCompilationUnit) {
		try {
			TextEdit astEdit = rewrite.rewriteAST();

			

			ITextFileBufferManager bufferManager = FileBuffers.getTextFileBufferManager();
			IPath path = iCompilationUnit.getPath(); 
			try {
				bufferManager.connect(path, LocationKind.IFILE, null); 
				ITextFileBuffer textFileBuffer = bufferManager.getTextFileBuffer(path, LocationKind.IFILE);

				IDocument document = textFileBuffer.getDocument(); 

				

				astEdit.apply(document);

				textFileBuffer.commit(null /* ProgressMonitor */, false /* Overwrite */); 
			} finally {
				bufferManager.disconnect(path, LocationKind.IFILE, null); 
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}
