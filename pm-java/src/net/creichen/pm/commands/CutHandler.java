package net.creichen.pm.commands;

import static net.creichen.pm.utils.APIWrapperUtil.getStructuralProperty;

import java.util.ArrayList;
import java.util.List;

import net.creichen.pm.PMProject;
import net.creichen.pm.PMWorkspace;
import net.creichen.pm.selection.Selection;
import net.creichen.pm.steps.CutStep;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ChildListPropertyDescriptor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.handlers.HandlerUtil;

public class CutHandler extends AbstractActionWrapper {

	@Override
	public final Object execute(final ExecutionEvent event)
			throws ExecutionException {
		setWindow(HandlerUtil.getActiveWorkbenchWindow(event));
		ISelection selection = HandlerUtil.getCurrentSelection(event);

		if (!(selection instanceof ITextSelection)) {
			showErrorDialog("PM Cut Error",
					"PM Cut must be run on a text selection.");
			return null;
		}

		final ICompilationUnit iCompilationUnit = getCompilationUnit(getWindow());
		final PMProject project = PMWorkspace.sharedWorkspace()
				.projectForIJavaProject(iCompilationUnit.getJavaProject());
		final ITextSelection textSelection = (ITextSelection) selection;
		project.syncSources(); // in case they have changed
		final Selection selectionDescriptor = new Selection(
				(CompilationUnit) project
						.findASTRootForICompilationUnit(iCompilationUnit),
				textSelection.getOffset(), textSelection.getLength());

		if (!selectionDescriptor.isSaneSelection()) {
			showErrorDialog("PM Cut Error",
					"PMCut cannot be applied to this selection");
			return null;
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

		return null;
	}

}
