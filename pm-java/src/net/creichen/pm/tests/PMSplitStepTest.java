/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

*******************************************************************************/

package net.creichen.pm.tests;

import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;
import net.creichen.pm.PMASTQuery;
import net.creichen.pm.PMNodeReference;
import net.creichen.pm.PMProject;
import net.creichen.pm.PMWorkspace;
import net.creichen.pm.models.PMNameModel;
import net.creichen.pm.models.PMUDModel;
import net.creichen.pm.steps.PMSplitStep;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.junit.Test;

public class PMSplitStepTest extends PMTest {

	@Test public void testSimplestCase() throws JavaModelException {
		ICompilationUnit iCompilationUnit = createNewCompilationUnit("", "S.java", "public class S { void m() {int x; x = 7; x = 5; System.out.println(x);} }");
		
		PMProject project = PMWorkspace.sharedWorkspace().projectForIJavaProject(_iJavaProject);
		
		Assignment secondAssignment = PMASTQuery.assignmentInMethodInClassInCompilationUnit(1, "m", 0, "S", 0, (CompilationUnit)project.findASTRootForICompilationUnit(iCompilationUnit));
		
		ExpressionStatement assignmentStatement = (ExpressionStatement)secondAssignment.getParent();
		
		PMSplitStep step = new PMSplitStep(project,assignmentStatement);
		
		step.applyAllAtOnce();
		
		//We have five outputs that we care about: the source, the updated name model,
		//the DU/UD model, replacement declaration statement node, and the inconsistencies
		
		//Source test
		assertTrue(compilationUnitSourceMatchesSource("public class S { void m() {int x; x = 7; int x = 5; System.out.println(x);} }", iCompilationUnit.getSource()));
		
		//Name model test
		
		PMNameModel nameModel = project.getNameModel();
		
		//First two occurrences of x should have same identifier
		//Second two occurrences of x should have same identifier (different from first identifier)
		
		SimpleName firstX = PMASTQuery.simpleNameWithIdentifierInMethodInClassInCompilationUnit("x", 0, "m", 0, "S", 0, (CompilationUnit)project.findASTRootForICompilationUnit(iCompilationUnit));
		SimpleName secondX = PMASTQuery.simpleNameWithIdentifierInMethodInClassInCompilationUnit("x", 1, "m", 0, "S", 0, (CompilationUnit)project.findASTRootForICompilationUnit(iCompilationUnit));

		
		
		assertNotNull(nameModel.identifierForName(firstX));
		assertNotNull(nameModel.identifierForName(secondX));
		
		assertEquals(nameModel.identifierForName(firstX), nameModel.identifierForName(secondX));
		
		SimpleName thirdX = PMASTQuery.simpleNameWithIdentifierInMethodInClassInCompilationUnit("x", 2, "m", 0, "S", 0, (CompilationUnit)project.findASTRootForICompilationUnit(iCompilationUnit));
		SimpleName fourthX = PMASTQuery.simpleNameWithIdentifierInMethodInClassInCompilationUnit("x", 3, "m", 0, "S", 0, (CompilationUnit)project.findASTRootForICompilationUnit(iCompilationUnit));

		
		assertNotNull(nameModel.identifierForName(thirdX));
		assertNotNull(nameModel.identifierForName(fourthX));
	
		assertEquals(nameModel.identifierForName(thirdX), nameModel.identifierForName(fourthX));
		
		assertTrue(!nameModel.identifierForName(firstX).equals(nameModel.identifierForName(thirdX)));
		
		//now test reverse mapping interface
		
		List<SimpleName> nodesRelatedToFirstDeclaration = nameModel.nameNodesRelatedToNameNode(firstX);
		
		assertEquals(2, nodesRelatedToFirstDeclaration.size());
		assertTrue(nodesRelatedToFirstDeclaration.contains(firstX));
		assertTrue(nodesRelatedToFirstDeclaration.contains(secondX));
		
		List<SimpleName> nodesRelatedToSecondDeclaration = nameModel.nameNodesRelatedToNameNode(thirdX);
		
		assertEquals(2, nodesRelatedToSecondDeclaration.size());
		assertTrue(nodesRelatedToSecondDeclaration.contains(thirdX));
		assertTrue(nodesRelatedToSecondDeclaration.contains(fourthX));
		
		//DU/UD test
		
		PMUDModel udModel = project.getUDModel();
		
		//The use for the second declaration should be the fourthX
		
		VariableDeclarationFragment secondXDeclaration = (VariableDeclarationFragment)thirdX.getParent();
		
		Set<PMNodeReference> usesOfSecondDeclaration = udModel.usesForDefinition(project.getReferenceForNode(secondXDeclaration));
		
		assertEquals(1, usesOfSecondDeclaration.size());
		
		
		//Replacement Declaration Node test
		//Make sure the node we report back is exactly equal to the one in the AST
		
		VariableDeclarationStatement expectedReplacementDeclaration = (VariableDeclarationStatement)secondXDeclaration.getParent();
		
		assertTrue(expectedReplacementDeclaration == step.getReplacementDeclarationStatement());
	}
	
}
