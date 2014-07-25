/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm.inconsistencies;

import net.creichen.pm.Project;
import net.creichen.pm.api.PMCompilationUnit;
import net.creichen.pm.models.NameModel;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.SimpleName;

public class NameCapture extends Inconsistency {

	private final ASTNode expectedDeclaration;
	private final ASTNode actualDeclaration;
	private Project project;

	public NameCapture(final Project project, final PMCompilationUnit iCompilationUnit, final ASTNode capturedNode,
			final ASTNode expectedDeclaration, final ASTNode actualDeclaration) {
		super(iCompilationUnit, capturedNode);
		this.project = project;
		this.expectedDeclaration = expectedDeclaration;
		this.actualDeclaration = actualDeclaration;
	}

	@Override
	public void acceptBehavioralChange() {
		final Name capturedName = (Name) getCapturedNode();
		final NameModel nameModel = this.project.getNameModel();
		final Name capturingName = this.project.simpleNameForDeclaringNode(this.actualDeclaration);
		final String capturingIdentifier = nameModel.identifierForName(capturingName);

		nameModel.setIdentifierForName(capturingIdentifier, capturedName);

		this.project.rescanForInconsistencies();
	}

	@Override
	public boolean allowsAcceptBehavioralChange() {
		return true;
	}

	public ASTNode getActualDeclaration() {
		return this.actualDeclaration;
	}

	public ASTNode getCapturedNode() {
		return getNode();
	}

	public String getCapturedNodeDescription() {

		if (getNode() instanceof SimpleName) {
			return ((SimpleName) getNode()).getIdentifier();
		} else {
			return "Unknown node";
		}
	}

	public ASTNode getExpectedDeclaration() {
		return this.expectedDeclaration;
	}

	@Override
	public String getHumanReadableDescription() {
		return getCapturedNodeDescription() + " was captured.";
	}

}
