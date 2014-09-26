/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm.steps;

import static net.creichen.pm.utils.ASTQuery.findClassByName;
import static net.creichen.pm.utils.ASTQuery.findFieldByName;
import static net.creichen.pm.utils.ASTQuery.findMethodByName;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import net.creichen.pm.api.NodeReference;
import net.creichen.pm.data.NodeReferenceStore;
import net.creichen.pm.data.Pasteboard;
import net.creichen.pm.models.DefUseModel;
import net.creichen.pm.models.NameModel;
import net.creichen.pm.tests.PMTest;
import net.creichen.pm.utils.ASTQuery;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.junit.Test;

public class CopyStepTest extends PMTest {

    @Test
    public void testCopyDeclarationAndReferencesMaintainsInternalReferences() throws JavaModelException {

        final String source = "public class S {int x; void m(){x = 1; y++;} int y;}";

        final ICompilationUnit iCompilationUnit = createCompilationUnit("", "S.java", source);

        final NameModel nameModel = getProject().getNameModel();

        final CompilationUnit compilationUnitS = getProject().getCompilationUnit(iCompilationUnit);

        final TypeDeclaration type = findClassByName("S", compilationUnitS);
        final FieldDeclaration fieldDeclaration = findFieldByName("x", type);
        final SimpleName fieldDeclarationOriginalName = ((VariableDeclarationFragment) fieldDeclaration.fragments()
                .get(0)).getName();
        final String fieldDeclarationOriginalNameIdentifier = nameModel
                .getIdentifierForName(fieldDeclarationOriginalName);

        final MethodDeclaration methodDeclaration = findMethodByName("m", type);
        final SimpleName methodDeclarationOriginalName = methodDeclaration.getName();
        final String methodDeclarationOriginalNameIdentifier = nameModel
                .getIdentifierForName(methodDeclarationOriginalName);

        final SimpleName originalUseOfYInM = ASTQuery.findSimpleNameByIdentifier("y", 0, methodDeclaration.getBody());
        final String originalUseOfYInMIdentifier = nameModel.getIdentifierForName(originalUseOfYInM);

        final List<ASTNode> nodesToCopy = new ArrayList<ASTNode>();
        nodesToCopy.add(methodDeclaration); // Method declaration goes first to
        // make sure a reference can come
        // before a decl
        nodesToCopy.add(fieldDeclaration);

        final CopyStep copyStep = new CopyStep(getProject(), nodesToCopy);

        copyStep.applyAllAtOnce();

        // Source shouldn't have changed

        assertTrue(matchesSource("public class S {int x; void m(){x = 1; y++;} int y;}", iCompilationUnit.getSource()));

        final MethodDeclaration methodDeclarationCopy = (MethodDeclaration) Pasteboard.getInstance()
                .getPasteboardRoots().get(0);
        final FieldDeclaration fieldDeclarationCopy = (FieldDeclaration) Pasteboard.getInstance().getPasteboardRoots()
                .get(1);

        // pasteboard exists
        assertNotNull(fieldDeclarationCopy);

        final SimpleName fieldDeclarationCopyName = ((VariableDeclarationFragment) fieldDeclarationCopy.fragments()
                .get(0)).getName();

        // field declaration gets fresh identifier copy

        final String fieldDeclarationCopyNameIdentifier = nameModel.getIdentifierForName(fieldDeclarationCopyName);
        assertNotNull(fieldDeclarationCopyNameIdentifier);
        assertFalse(fieldDeclarationCopyNameIdentifier.equals(fieldDeclarationOriginalNameIdentifier));

        // method declaration gets fresh identifier in copy

        final String methodDeclarationCopyNameIdentifier = nameModel.getIdentifierForName(methodDeclarationCopy
                .getName());
        assertNotNull(methodDeclarationCopyNameIdentifier);
        assertFalse(methodDeclarationCopyNameIdentifier.equals(methodDeclarationOriginalNameIdentifier));

        // use of internal field declaration points to the fresh declaration
        // identifier

        final SimpleName copyUseOfXInM = ASTQuery.findSimpleNameByIdentifier("x", 0, methodDeclarationCopy.getBody());
        final String copyUseOfXInMIdentifier = nameModel.getIdentifierForName(copyUseOfXInM);

        assertEquals(copyUseOfXInMIdentifier, fieldDeclarationCopyNameIdentifier);

        // use of external field declaration (i.e. one not copied) still points
        // to its original declaration

        final SimpleName copyUseOfYInM = ASTQuery.findSimpleNameByIdentifier("y", 0, methodDeclarationCopy.getBody());
        final String copyUseOfYInMIdentifier = nameModel.getIdentifierForName(copyUseOfYInM);

        assertEquals(copyUseOfYInMIdentifier, originalUseOfYInMIdentifier);
    }

    @Test
    public void testCopyDefinitionAndReferencesMaintainsInternalReferences() throws JavaModelException {

        final String source = "public class S {void m(){int x = 1; int y = 2; int z = 3; x = x + 1; z = y + x;}}";

        final ICompilationUnit iCompilationUnit = createCompilationUnit("", "S.java", source);

        final DefUseModel udModel = getProject().getUDModel();

        final CompilationUnit compilationUnitS = getProject().getCompilationUnit(iCompilationUnit);

        final TypeDeclaration type = findClassByName("S", compilationUnitS);
        final MethodDeclaration methodDeclaration = findMethodByName("m", type);

        final ExpressionStatement fourthStatementOriginal = (ExpressionStatement) methodDeclaration.getBody()
                .statements().get(3);
        final Assignment xGetsXPlusOneAssignmentOriginal = (Assignment) fourthStatementOriginal.getExpression();

        final SimpleName x3RHSOriginal = ASTQuery.findSimpleNameByIdentifier("x", 1, xGetsXPlusOneAssignmentOriginal);

        final NodeReference x3RHSOriginalNodeReference = NodeReferenceStore.getInstance().getReference(x3RHSOriginal);

        final ExpressionStatement fifthStatementOriginal = (ExpressionStatement) methodDeclaration.getBody()
                .statements().get(4);

        final List<ASTNode> nodesToCopy = new ArrayList<ASTNode>();
        nodesToCopy.add(fifthStatementOriginal);
        nodesToCopy.add(fourthStatementOriginal);

        final CopyStep copyStep = new CopyStep(getProject(), nodesToCopy);

        copyStep.applyAllAtOnce();

        // Source shouldn't have changed

        assertTrue(matchesSource(source, iCompilationUnit.getSource()));

        final ExpressionStatement fourthStatementCopy = (ExpressionStatement) Pasteboard.getInstance()
                .getPasteboardRoots().get(1);
        assertNotNull(fourthStatementCopy);
        final ExpressionStatement fifthStatementCopy = (ExpressionStatement) Pasteboard.getInstance()
                .getPasteboardRoots().get(0);
        assertNotNull(fifthStatementCopy);

        final Assignment xGetsXPlusOneAssignmentCopy = (Assignment) fourthStatementCopy.getExpression();

        final NodeReference xGetsXPlusOneAssignmentCopyReference = NodeReferenceStore.getInstance().getReference(
                xGetsXPlusOneAssignmentCopy);

        final SimpleName x3RHSCopy = ASTQuery.findSimpleNameByIdentifier("x", 1, xGetsXPlusOneAssignmentCopy);
        final NodeReference x3RHSCopyNodeReference = NodeReferenceStore.getInstance().getReference(x3RHSCopy);

        final Set<NodeReference> definitionsForX3RHSOriginal = udModel
                .definitionIdentifiersForName(x3RHSOriginalNodeReference);
        final Set<NodeReference> definitionsForX3RHSCopy = udModel.definitionIdentifiersForName(x3RHSCopyNodeReference);

        // Since this x gets its single definition from outside of the copied
        // nodes, the copy should have the
        // same reaching definitions as the original
        assertEquals(definitionsForX3RHSOriginal, definitionsForX3RHSCopy);

        final SimpleName x4RHSCopy = ASTQuery.findSimpleNameByIdentifier("x", 0, fifthStatementCopy);
        final NodeReference x4RHSCopyNodeReference = NodeReferenceStore.getInstance().getReference(x4RHSCopy);

        final Set<NodeReference> definitionsForX4RHSCopy = udModel.definitionIdentifiersForName(x4RHSCopyNodeReference);

        assertEquals(1, definitionsForX4RHSCopy.size());
        assertEquals(xGetsXPlusOneAssignmentCopyReference, definitionsForX4RHSCopy.toArray()[0]);
    }

    @Test
    public void testCopyFieldCreatesNewNameIdentifier() throws JavaModelException {

        final String source = "public class S {int x; void m(){x = 1;}}";

        final ICompilationUnit iCompilationUnit = createCompilationUnit("", "S.java", source);

        final TypeDeclaration type = findClassByName("S", getProject().getCompilationUnit(iCompilationUnit));
        final FieldDeclaration fieldDeclaration = findFieldByName("x", type);

        final SimpleName fieldDeclarationOriginalName = ((VariableDeclarationFragment) fieldDeclaration.fragments()
                .get(0)).getName();

        final String fieldDeclarationOriginalNameIdentifier = getProject().getNameModel().getIdentifierForName(
                fieldDeclarationOriginalName);

        final CopyStep copyStep = new CopyStep(getProject(), fieldDeclaration);

        copyStep.applyAllAtOnce();

        // Source shouldn't have changed

        assertTrue(matchesSource("public class S {int x; void m(){x = 1;}}", iCompilationUnit.getSource()));

        final FieldDeclaration fieldDeclarationCopy = (FieldDeclaration) Pasteboard.getInstance().getPasteboardRoots()
                .get(0);

        assertNotNull(fieldDeclarationCopy);

        final SimpleName fieldDeclarationCopyName = ((VariableDeclarationFragment) fieldDeclarationCopy.fragments()
                .get(0)).getName();

        final String fieldDeclarationCopyNameIdentifier = getProject().getNameModel().getIdentifierForName(
                fieldDeclarationCopyName);

        assertNotNull(fieldDeclarationCopyNameIdentifier);

        assertFalse(fieldDeclarationCopyNameIdentifier.equals(fieldDeclarationOriginalNameIdentifier));
    }

}
