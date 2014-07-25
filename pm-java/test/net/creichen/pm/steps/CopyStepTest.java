/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm.steps;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import net.creichen.pm.PMTest;
import net.creichen.pm.Project;
import net.creichen.pm.Workspace;
import net.creichen.pm.analysis.ASTQuery;
import net.creichen.pm.api.NodeReference;
import net.creichen.pm.models.NameModel;
import net.creichen.pm.models.DefUseModel;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.*;
import org.junit.Test;

public class CopyStepTest extends PMTest {

    @Test
    public void testCopyDeclarationAndReferencesMaintainsInternalReferences()
            throws JavaModelException {

        final String source = "public class S {int x; void m(){x = 1; y++;} int y;}";

        final ICompilationUnit iCompilationUnit = createNewCompilationUnit("", "S.java", source);

        final Project pmProject = Workspace.sharedWorkspace().projectForIJavaProject(
                this.getIJavaProject());

        final NameModel nameModel = pmProject.getNameModel();

        final CompilationUnit compilationUnitS = (CompilationUnit) pmProject
                .findASTRootForICompilationUnit(iCompilationUnit);

        final FieldDeclaration fieldDeclaration = (FieldDeclaration) ASTQuery
                .fieldWithNameInClassInCompilationUnit("x", 0, "S", 0, compilationUnitS)
                .getParent();
        final SimpleName fieldDeclarationOriginalName = ((VariableDeclarationFragment) fieldDeclaration
                .fragments().get(0)).getName();
        final String fieldDeclarationOriginalNameIdentifier = nameModel
                .identifierForName(fieldDeclarationOriginalName);

        final MethodDeclaration methodDeclaration = ASTQuery
                .methodWithNameInClassInCompilationUnit("m", 0, "S", 0, compilationUnitS);
        final SimpleName methodDeclarationOriginalName = methodDeclaration.getName();
        final String methodDeclarationOriginalNameIdentifier = nameModel
                .identifierForName(methodDeclarationOriginalName);

        final SimpleName originalUseOfYInM = ASTQuery.simpleNameWithIdentifierInNode("y", 0,
                methodDeclaration.getBody());
        final String originalUseOfYInMIdentifier = nameModel.identifierForName(originalUseOfYInM);

        final List<ASTNode> nodesToCopy = new ArrayList<ASTNode>();
        nodesToCopy.add(methodDeclaration); // Method declaration goes first to
                                            // make sure a reference can come
                                            // before a decl
        nodesToCopy.add(fieldDeclaration);

        final CopyStep copyStep = new CopyStep(pmProject, nodesToCopy);

        copyStep.applyAllAtOnce();

        // Source shouldn't have changed

        assertTrue(compilationUnitSourceMatchesSource(
                "public class S {int x; void m(){x = 1; y++;} int y;}",
                iCompilationUnit.getSource()));

        final MethodDeclaration methodDeclarationCopy = (MethodDeclaration) pmProject
                .getPasteboard().getPasteboardRoots().get(0);
        final FieldDeclaration fieldDeclarationCopy = (FieldDeclaration) pmProject.getPasteboard()
                .getPasteboardRoots().get(1);

        // pasteboard exists
        assertNotNull(fieldDeclarationCopy);

        final SimpleName fieldDeclarationCopyName = ((VariableDeclarationFragment) fieldDeclarationCopy
                .fragments().get(0)).getName();

        // field declaration gets fresh identifier copy

        final String fieldDeclarationCopyNameIdentifier = nameModel
                .identifierForName(fieldDeclarationCopyName);
        assertNotNull(fieldDeclarationCopyNameIdentifier);
        assertFalse(fieldDeclarationCopyNameIdentifier
                .equals(fieldDeclarationOriginalNameIdentifier));

        // method declaration gets fresh identifier in copy

        final String methodDeclarationCopyNameIdentifier = nameModel
                .identifierForName(methodDeclarationCopy.getName());
        assertNotNull(methodDeclarationCopyNameIdentifier);
        assertFalse(methodDeclarationCopyNameIdentifier
                .equals(methodDeclarationOriginalNameIdentifier));

        // use of internal field declaration points to the fresh declaration
        // identifier

        final SimpleName copyUseOfXInM = ASTQuery.simpleNameWithIdentifierInNode("x", 0,
                methodDeclarationCopy.getBody());
        final String copyUseOfXInMIdentifier = nameModel.identifierForName(copyUseOfXInM);

        assertEquals(copyUseOfXInMIdentifier, fieldDeclarationCopyNameIdentifier);

        // use of external field declaration (i.e. one not copied) still points
        // to its original declaration

        final SimpleName copyUseOfYInM = ASTQuery.simpleNameWithIdentifierInNode("y", 0,
                methodDeclarationCopy.getBody());
        final String copyUseOfYInMIdentifier = nameModel.identifierForName(copyUseOfYInM);

        assertEquals(copyUseOfYInMIdentifier, originalUseOfYInMIdentifier);
    }

    @Test
    public void testCopyDefinitionAndReferencesMaintainsInternalReferences()
            throws JavaModelException {

        final String source = "public class S {void m(){int x = 1; int y = 2; int z = 3; x = x + 1; z = y + x;}}";

        final ICompilationUnit iCompilationUnit = createNewCompilationUnit("", "S.java", source);

        final Project pmProject = Workspace.sharedWorkspace().projectForIJavaProject(
                this.getIJavaProject());

        final DefUseModel udModel = pmProject.getUDModel();

        final CompilationUnit compilationUnitS = (CompilationUnit) pmProject
                .findASTRootForICompilationUnit(iCompilationUnit);

        final MethodDeclaration methodDeclaration = ASTQuery
                .methodWithNameInClassInCompilationUnit("m", 0, "S", 0, compilationUnitS);

        final ExpressionStatement fourthStatementOriginal = (ExpressionStatement) methodDeclaration
                .getBody().statements().get(3);
        final Assignment xGetsXPlusOneAssignmentOriginal = (Assignment) fourthStatementOriginal
                .getExpression();

        final SimpleName x3RHSOriginal = ASTQuery.simpleNameWithIdentifierInNode("x", 1,
                xGetsXPlusOneAssignmentOriginal);

        final NodeReference x3RHSOriginalNodeReference = pmProject
                .getReferenceForNode(x3RHSOriginal);

        final ExpressionStatement fifthStatementOriginal = (ExpressionStatement) methodDeclaration
                .getBody().statements().get(4);

        final List<ASTNode> nodesToCopy = new ArrayList<ASTNode>();
        nodesToCopy.add(fifthStatementOriginal);
        nodesToCopy.add(fourthStatementOriginal);

        final CopyStep copyStep = new CopyStep(pmProject, nodesToCopy);

        copyStep.applyAllAtOnce();

        // Source shouldn't have changed

        assertTrue(compilationUnitSourceMatchesSource(source, iCompilationUnit.getSource()));

        final ExpressionStatement fourthStatementCopy = (ExpressionStatement) pmProject
                .getPasteboard().getPasteboardRoots().get(1);
        assertNotNull(fourthStatementCopy);
        final ExpressionStatement fifthStatementCopy = (ExpressionStatement) pmProject
                .getPasteboard().getPasteboardRoots().get(0);
        assertNotNull(fifthStatementCopy);

        final Assignment xGetsXPlusOneAssignmentCopy = (Assignment) fourthStatementCopy
                .getExpression();

        final NodeReference xGetsXPlusOneAssignmentCopyReference = pmProject
                .getReferenceForNode(xGetsXPlusOneAssignmentCopy);

        final SimpleName x3RHSCopy = ASTQuery.simpleNameWithIdentifierInNode("x", 1,
                xGetsXPlusOneAssignmentCopy);
        final NodeReference x3RHSCopyNodeReference = pmProject.getReferenceForNode(x3RHSCopy);

        final Set<NodeReference> definitionsForX3RHSOriginal = udModel
                .definitionIdentifiersForName(x3RHSOriginalNodeReference);
        final Set<NodeReference> definitionsForX3RHSCopy = udModel
                .definitionIdentifiersForName(x3RHSCopyNodeReference);

        // Since this x gets its single definition from outside of the copied
        // nodes, the copy should have the
        // same reaching definitions as the original
        assertEquals(definitionsForX3RHSOriginal, definitionsForX3RHSCopy);

        final SimpleName x4RHSCopy = ASTQuery.simpleNameWithIdentifierInNode("x", 0,
                fifthStatementCopy);
        final NodeReference x4RHSCopyNodeReference = pmProject.getReferenceForNode(x4RHSCopy);

        final Set<NodeReference> definitionsForX4RHSCopy = udModel
                .definitionIdentifiersForName(x4RHSCopyNodeReference);

        assertEquals(1, definitionsForX4RHSCopy.size());
        assertEquals(xGetsXPlusOneAssignmentCopyReference, definitionsForX4RHSCopy.toArray()[0]);
    }

    @Test
    public void testCopyFieldCreatesNewNameIdentifier() throws JavaModelException {

        final String source = "public class S {int x; void m(){x = 1;}}";

        final ICompilationUnit iCompilationUnit = createNewCompilationUnit("", "S.java", source);

        final Project pmProject = Workspace.sharedWorkspace().projectForIJavaProject(
                this.getIJavaProject());

        final VariableDeclarationFragment fieldDeclarationFragment = ASTQuery
                .fieldWithNameInClassInCompilationUnit("x", 0, "S", 0, (CompilationUnit) pmProject
                        .findASTRootForICompilationUnit(iCompilationUnit));

        final FieldDeclaration fieldDeclaration = (FieldDeclaration) fieldDeclarationFragment
                .getParent();

        final SimpleName fieldDeclarationOriginalName = ((VariableDeclarationFragment) fieldDeclaration
                .fragments().get(0)).getName();

        final String fieldDeclarationOriginalNameIdentifier = pmProject.getNameModel()
                .identifierForName(fieldDeclarationOriginalName);

        final CopyStep copyStep = new CopyStep(pmProject, fieldDeclaration);

        copyStep.applyAllAtOnce();

        // Source shouldn't have changed

        assertTrue(compilationUnitSourceMatchesSource("public class S {int x; void m(){x = 1;}}",
                iCompilationUnit.getSource()));

        final FieldDeclaration fieldDeclarationCopy = (FieldDeclaration) pmProject.getPasteboard()
                .getPasteboardRoots().get(0);

        assertNotNull(fieldDeclarationCopy);

        final SimpleName fieldDeclarationCopyName = ((VariableDeclarationFragment) fieldDeclarationCopy
                .fragments().get(0)).getName();

        final String fieldDeclarationCopyNameIdentifier = pmProject.getNameModel()
                .identifierForName(fieldDeclarationCopyName);

        assertNotNull(fieldDeclarationCopyNameIdentifier);

        assertFalse(fieldDeclarationCopyNameIdentifier
                .equals(fieldDeclarationOriginalNameIdentifier));
    }

}
