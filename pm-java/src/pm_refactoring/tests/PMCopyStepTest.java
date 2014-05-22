/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

*******************************************************************************/

package pm_refactoring.tests;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SimpleName;

import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.junit.Test;

import pm_refactoring.PMASTQuery;
import pm_refactoring.PMNodeReference;
import pm_refactoring.PMProject;
import pm_refactoring.PMWorkspace;
import pm_refactoring.models.PMNameModel;
import pm_refactoring.models.PMUDModel;
import pm_refactoring.steps.PMCopyStep;

import static org.junit.Assert.*;

public class PMCopyStepTest extends PMTest {

	
	@Test public void testCopyFieldCreatesNewNameIdentifier() throws JavaModelException {
		
		String source = "public class S {int x; void m(){x = 1;}}";
			
		ICompilationUnit iCompilationUnit = createNewCompilationUnit("", "S.java", source);
		
		PMProject pmProject = PMWorkspace.sharedWorkspace().projectForIJavaProject(_iJavaProject);
		
		VariableDeclarationFragment fieldDeclarationFragment = PMASTQuery.fieldWithNameInClassInCompilationUnit("x", 0, "S", 0, (CompilationUnit)pmProject.findASTRootForICompilationUnit(iCompilationUnit));
		
		FieldDeclaration fieldDeclaration = (FieldDeclaration)fieldDeclarationFragment.getParent();
		
		SimpleName fieldDeclarationOriginalName = ((VariableDeclarationFragment)fieldDeclaration.fragments().get(0)).getName();

		String fieldDeclarationOriginalNameIdentifier = pmProject.getNameModel().identifierForName(fieldDeclarationOriginalName);
		
		PMCopyStep copyStep = new PMCopyStep(pmProject, fieldDeclaration);
		
		copyStep.applyAllAtOnce();
		
		//Source shouldn't have changed
		
		assertTrue(compilationUnitSourceMatchesSource("public class S {int x; void m(){x = 1;}}", iCompilationUnit.getSource()));		
	
		FieldDeclaration fieldDeclarationCopy = (FieldDeclaration)pmProject.getPasteboard().getPasteboardRoots().get(0);
	
		assertNotNull(fieldDeclarationCopy);
		
		SimpleName fieldDeclarationCopyName = ((VariableDeclarationFragment)fieldDeclarationCopy.fragments().get(0)).getName();
	
		String fieldDeclarationCopyNameIdentifier = pmProject.getNameModel().identifierForName(fieldDeclarationCopyName);
		
		assertNotNull(fieldDeclarationCopyNameIdentifier);
		
		assertFalse(fieldDeclarationCopyNameIdentifier.equals(fieldDeclarationOriginalNameIdentifier));
	}

	@Test public void testCopyDeclarationAndReferencesMaintainsInternalReferences() throws JavaModelException {
		
		String source = "public class S {int x; void m(){x = 1; y++;} int y;}";
			
		ICompilationUnit iCompilationUnit = createNewCompilationUnit("", "S.java", source);
		
		PMProject pmProject = PMWorkspace.sharedWorkspace().projectForIJavaProject(_iJavaProject);
		
		PMNameModel nameModel = pmProject.getNameModel();
		
		CompilationUnit compilationUnitS = (CompilationUnit)pmProject.findASTRootForICompilationUnit(iCompilationUnit);
		
		FieldDeclaration fieldDeclaration = (FieldDeclaration)PMASTQuery.fieldWithNameInClassInCompilationUnit("x", 0, "S", 0, compilationUnitS).getParent();
		SimpleName fieldDeclarationOriginalName = ((VariableDeclarationFragment)fieldDeclaration.fragments().get(0)).getName();
		String fieldDeclarationOriginalNameIdentifier = nameModel.identifierForName(fieldDeclarationOriginalName);

		MethodDeclaration  methodDeclaration = PMASTQuery.methodWithNameInClassInCompilationUnit("m", 0, "S", 0, compilationUnitS);
		SimpleName methodDeclarationOriginalName = methodDeclaration.getName();
		String methodDeclarationOriginalNameIdentifier = nameModel.identifierForName(methodDeclarationOriginalName);

		SimpleName originalUseOfYInM = PMASTQuery.simpleNameWithIdentifierInNode("y", 0, methodDeclaration.getBody());
		String originalUseOfYInMIdentifier = nameModel.identifierForName(originalUseOfYInM);
		
		
		List<ASTNode> nodesToCopy = new ArrayList<ASTNode>();
		nodesToCopy.add(methodDeclaration); //Method declaration goes first to make sure a reference can come before a decl
		nodesToCopy.add(fieldDeclaration);
		
		
		PMCopyStep copyStep = new PMCopyStep(pmProject, nodesToCopy);
		
		copyStep.applyAllAtOnce();
		
		//Source shouldn't have changed
		
		assertTrue(compilationUnitSourceMatchesSource("public class S {int x; void m(){x = 1; y++;} int y;}", iCompilationUnit.getSource()));		
	
		MethodDeclaration methodDeclarationCopy = (MethodDeclaration)pmProject.getPasteboard().getPasteboardRoots().get(0);
		FieldDeclaration fieldDeclarationCopy = (FieldDeclaration)pmProject.getPasteboard().getPasteboardRoots().get(1);
	
		//pasteboard exists
		assertNotNull(fieldDeclarationCopy);
		
		SimpleName fieldDeclarationCopyName = ((VariableDeclarationFragment)fieldDeclarationCopy.fragments().get(0)).getName();
	
		
		//field declaration gets fresh identifier copy
		
		String fieldDeclarationCopyNameIdentifier = nameModel.identifierForName(fieldDeclarationCopyName);
		assertNotNull(fieldDeclarationCopyNameIdentifier);
		assertFalse(fieldDeclarationCopyNameIdentifier.equals(fieldDeclarationOriginalNameIdentifier));
		
		//method declaration gets fresh identifier in copy
		
		String methodDeclarationCopyNameIdentifier = nameModel.identifierForName(methodDeclarationCopy.getName());
		assertNotNull(methodDeclarationCopyNameIdentifier);
		assertFalse(methodDeclarationCopyNameIdentifier.equals(methodDeclarationOriginalNameIdentifier));
		
	
		//use of internal field declaration points to the fresh declaration identifier
	
		SimpleName copyUseOfXInM = PMASTQuery.simpleNameWithIdentifierInNode("x", 0, methodDeclarationCopy.getBody());
		String copyUseOfXInMIdentifier = nameModel.identifierForName(copyUseOfXInM);
		
		assertEquals(copyUseOfXInMIdentifier, fieldDeclarationCopyNameIdentifier);
		
		//use of external field declaration (i.e. one not copied) still points to its original declaration
		
		SimpleName copyUseOfYInM = PMASTQuery.simpleNameWithIdentifierInNode("y", 0, methodDeclarationCopy.getBody());
		String copyUseOfYInMIdentifier = nameModel.identifierForName(copyUseOfYInM);
		
		assertEquals(copyUseOfYInMIdentifier, originalUseOfYInMIdentifier);
	}

	
@Test public void testCopyDefinitionAndReferencesMaintainsInternalReferences() throws JavaModelException {
		
		String source = "public class S {void m(){int x = 1; int y = 2; int z = 3; x = x + 1; z = y + x;}}";
			
		ICompilationUnit iCompilationUnit = createNewCompilationUnit("", "S.java", source);
		
		PMProject pmProject = PMWorkspace.sharedWorkspace().projectForIJavaProject(_iJavaProject);
		
		PMUDModel udModel = pmProject.getUDModel();
		
		CompilationUnit compilationUnitS = (CompilationUnit)pmProject.findASTRootForICompilationUnit(iCompilationUnit);
		
		MethodDeclaration methodDeclaration = PMASTQuery.methodWithNameInClassInCompilationUnit("m", 0, "S", 0, compilationUnitS);
		
		ExpressionStatement fourthStatementOriginal = (ExpressionStatement)methodDeclaration.getBody().statements().get(3);
		Assignment xGetsXPlusOneAssignmentOriginal = (Assignment)fourthStatementOriginal.getExpression();
		
				
		SimpleName x3RHSOriginal = PMASTQuery.simpleNameWithIdentifierInNode("x", 1, xGetsXPlusOneAssignmentOriginal);
		
		
		PMNodeReference x3RHSOriginalNodeReference = pmProject.getReferenceForNode(x3RHSOriginal);
				
		ExpressionStatement fifthStatementOriginal = (ExpressionStatement)methodDeclaration.getBody().statements().get(4);
					
	
		List<ASTNode> nodesToCopy = new ArrayList<ASTNode>();
		nodesToCopy.add(fifthStatementOriginal); 
		nodesToCopy.add(fourthStatementOriginal);
		
		PMCopyStep copyStep = new PMCopyStep(pmProject, nodesToCopy);
		
		copyStep.applyAllAtOnce();
		
		//Source shouldn't have changed
		
		assertTrue(compilationUnitSourceMatchesSource(source, iCompilationUnit.getSource()));		
	
		ExpressionStatement fourthStatementCopy = (ExpressionStatement)pmProject.getPasteboard().getPasteboardRoots().get(1);
		assertNotNull(fourthStatementCopy);
		ExpressionStatement fifthStatementCopy = (ExpressionStatement)pmProject.getPasteboard().getPasteboardRoots().get(0);
		assertNotNull(fifthStatementCopy);
	
		Assignment xGetsXPlusOneAssignmentCopy = (Assignment)fourthStatementCopy.getExpression();

		PMNodeReference xGetsXPlusOneAssignmentCopyReference = pmProject.getReferenceForNode(xGetsXPlusOneAssignmentCopy);
		
		SimpleName x3RHSCopy = PMASTQuery.simpleNameWithIdentifierInNode("x", 1, xGetsXPlusOneAssignmentCopy);
		PMNodeReference x3RHSCopyNodeReference = pmProject.getReferenceForNode(x3RHSCopy);
		
		Set<PMNodeReference> definitionsForX3RHSOriginal = udModel.definitionIdentifiersForName(x3RHSOriginalNodeReference);
		Set<PMNodeReference> definitionsForX3RHSCopy = udModel.definitionIdentifiersForName(x3RHSCopyNodeReference);
		
		//Since this x gets its single definition from outside of the copied nodes, the copy should have the
		//same reaching definitions as the original
		assertEquals(definitionsForX3RHSOriginal, definitionsForX3RHSCopy);
		
		
		SimpleName x4RHSCopy = PMASTQuery.simpleNameWithIdentifierInNode("x", 0, fifthStatementCopy);
		PMNodeReference x4RHSCopyNodeReference = pmProject.getReferenceForNode(x4RHSCopy);
		
		Set<PMNodeReference> definitionsForX4RHSCopy = udModel.definitionIdentifiersForName(x4RHSCopyNodeReference);
		
		assertEquals(1, definitionsForX4RHSCopy.size()); 
		assertEquals(xGetsXPlusOneAssignmentCopyReference, definitionsForX4RHSCopy.toArray()[0]);
	}

}
