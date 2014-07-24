package net.creichen.pm.commands;

import net.creichen.pm.PMProject;
import net.creichen.pm.PMWorkspace;
import net.creichen.pm.Pasteboard;
import net.creichen.pm.selection.InsertionPoint;
import net.creichen.pm.selection.InsertionPointFactory;
import net.creichen.pm.steps.PasteStep;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.ChildListPropertyDescriptor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.TypeDeclaration;

public class PasteHandler extends AbstractCommandHandler {

	@Override
	public final void handleEvent(final ExecutionEvent event) {
		final ICompilationUnit iCompilationUnit = getCompilationUnit();
		final PMProject project = PMWorkspace.sharedWorkspace()
				.projectForIJavaProject(iCompilationUnit.getJavaProject());
		final InsertionPoint insertionPoint = InsertionPointFactory
				.createInsertionPoint((CompilationUnit) project
						.findASTRootForICompilationUnit(iCompilationUnit),
						getSelection().getOffset());
		final ASTNode selectedNode = insertionPoint.getParent();
		final Pasteboard pasteboard = project.getPasteboard();

		if (insertionPoint.isValid()
				&& (selectedNode instanceof Block
						&& pasteboard.containsOnlyNodesOfClass(Statement.class) || selectedNode instanceof TypeDeclaration
						&& pasteboard
								.containsOnlyNodesOfClass(BodyDeclaration.class))) {

			final ChildListPropertyDescriptor childProperty = insertionPoint
					.getProperty();
			final int insertIndex = insertionPoint.getIndex();
			final PasteStep pasteStep = new PasteStep(project, selectedNode,
					childProperty, insertIndex);
			pasteStep.applyAllAtOnce();

		} else {
			showErrorDialog("PM Paste Error",
					"Paste must be run a block or a class definition");
		}
	}

}
