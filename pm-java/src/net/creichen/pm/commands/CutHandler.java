package net.creichen.pm.commands;

import static net.creichen.pm.utils.APIWrapperUtil.getStructuralProperty;

import java.util.ArrayList;
import java.util.List;

import net.creichen.pm.PMProject;
import net.creichen.pm.PMWorkspace;
import net.creichen.pm.selection.Selection;
import net.creichen.pm.steps.CutStep;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ChildListPropertyDescriptor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Statement;

public class CutHandler extends AbstractCommandHandler {

	@Override
	public final void handleEvent(final ExecutionEvent event) {
		final ICompilationUnit iCompilationUnit = getCompilationUnit();
		final PMProject project = PMWorkspace.sharedWorkspace()
				.projectForIJavaProject(iCompilationUnit.getJavaProject());
		project.syncSources(); // in case they have changed
		final Selection selectionDescriptor = new Selection(
				(CompilationUnit) project
						.findASTRootForICompilationUnit(iCompilationUnit),
				getSelection().getOffset(), getSelection().getLength());

		if (!selectionDescriptor.isSaneSelection()) {
			showErrorDialog("PM Cut Error",
					"PMCut cannot be applied to this selection");
		}
		final List<ASTNode> nodesToCut = new ArrayList<ASTNode>();

		if (!selectionDescriptor.isListSelection()) {
			final ASTNode selectedNode = selectionDescriptor
					.singleSelectedNode();
			if (selectedNode instanceof Statement
					|| selectedNode instanceof FieldDeclaration
					|| selectedNode instanceof MethodDeclaration) {
				nodesToCut.add(selectedNode);
			} else {
				showErrorDialog(
						"PM Cut Error",
						"We currently only support PM Cut on statements, methods, and fields -- you've selected a "
								+ selectedNode.getClass());
			}
		} else {
			final List<ASTNode> propertyList = getStructuralProperty(
					(ChildListPropertyDescriptor) selectionDescriptor
							.selectedNodeParentProperty(),
					selectionDescriptor.selectedNodeParent());
			for (int i = selectionDescriptor
					.selectedNodeParentPropertyListOffset(); i < selectionDescriptor
					.selectedNodeParentPropertyListOffset()
					+ selectionDescriptor
							.selectedNodeParentPropertyListLength(); i++) {
				nodesToCut.add(propertyList.get(i));
			}
		}
		if (nodesToCut.size() > 0) {
			new CutStep(project, nodesToCut).applyAllAtOnce();
		}

	}

}
