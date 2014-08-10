package net.creichen.pm.commands;

import net.creichen.pm.data.Pasteboard;
import net.creichen.pm.refactorings.PasteRefactoring;
import net.creichen.pm.selection.InsertionPoint;
import net.creichen.pm.selection.InsertionPointFactory;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.TypeDeclaration;

public class PasteHandler extends AbstractCommandHandler {

    @Override
    public final void handleEvent(final ExecutionEvent event) {
        final InsertionPoint insertionPoint = InsertionPointFactory.createInsertionPoint(getProject()
                .getCompilationUnitForICompilationUnit(getCompilationUnit()), getSelection().getOffset());
        final ASTNode selectedNode = insertionPoint.getParent();
        final Pasteboard pasteboard = Pasteboard.getInstance();

        if (!insertionPoint.isValid()
                || ((!(selectedNode instanceof Block) || !pasteboard.containsOnlyNodesOfClass(Statement.class)) && (!(selectedNode instanceof TypeDeclaration) || !pasteboard
                        .containsOnlyNodesOfClass(BodyDeclaration.class)))) {
            showErrorDialog("PM Paste Error", "Paste must be run a block or a class definition");
            return;
        }

        new PasteRefactoring(getProject(), selectedNode, insertionPoint.getProperty(), insertionPoint.getIndex())
                .apply();
    }

}
