/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm.models;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.creichen.pm.PMNodeReference;
import net.creichen.pm.PMProject;
import net.creichen.pm.PMTimer;
import net.creichen.pm.analysis.PMDef;
import net.creichen.pm.analysis.PMRDefsAnalysis;
import net.creichen.pm.analysis.PMUse;
import net.creichen.pm.inconsistencies.PMExtraDefinition;
import net.creichen.pm.inconsistencies.PMInconsistency;
import net.creichen.pm.inconsistencies.PMMissingDefinition;
import net.creichen.pm.inconsistencies.PMUnknownUse;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SimpleName;

public class PMUDModel {
	protected PMProject _project;

	// for now we only care about the defs that are used by our names

	final ASTNode _uninitializedMarkerNode;
	final PMNodeReference _uninitialized;

	Map<PMNodeReference, Set<PMNodeReference>> _definitionIdentifiersByUseIdentifier;

	Map<PMNodeReference, Set<PMNodeReference>> _useIdentifiersByDefinitionIdentifier;

	public PMUDModel(PMProject project) {

		_project = project;

		// this is such a hack; we create a random ast node and then get a
		// reference to it to
		// act as our uninitialized distinguished marker. We have to store this
		// node
		// so it isn't garbage collected out of the store (since the store uses
		// weak refs).

		AST ast = AST.newAST(AST.JLS4);

		_uninitializedMarkerNode = ast.newSimpleName("Foo");
		_uninitialized = _project.getReferenceForNode(_uninitializedMarkerNode);

		_definitionIdentifiersByUseIdentifier = new HashMap<PMNodeReference, Set<PMNodeReference>>();
		_useIdentifiersByDefinitionIdentifier = new HashMap<PMNodeReference, Set<PMNodeReference>>();

		initializeModel();
	}

	public boolean nameIsUse(SimpleName name) {
		PMNodeReference nameReference = _project.getReferenceForNode(name);

		return _definitionIdentifiersByUseIdentifier.containsKey(nameReference);
	}

	public Set<PMNodeReference> definitionIdentifiersForName(
			PMNodeReference nameIdentifier) {
		Set<PMNodeReference> definitionIdentifiers = _definitionIdentifiersByUseIdentifier
				.get(nameIdentifier);

		if (definitionIdentifiers == null) {
			definitionIdentifiers = new HashSet<PMNodeReference>();
			_definitionIdentifiersByUseIdentifier.put(nameIdentifier,
					definitionIdentifiers);
		}

		return definitionIdentifiers;
	}

	public void addDefinitionIdentifierForName(
			PMNodeReference definitionIdentifier, PMNodeReference nameIdentifier) {
		definitionIdentifiersForName(nameIdentifier).add(definitionIdentifier);
		addUseForDefinition(nameIdentifier, definitionIdentifier);

	}

	public void removeDefinitionIdentifierForName(
			PMNodeReference definitionIdentifier, PMNodeReference nameIdentifier) {
		definitionIdentifiersForName(nameIdentifier).remove(
				definitionIdentifier);
		removeUseForDefinition(nameIdentifier, definitionIdentifier);
	}

	public Set<PMNodeReference> usesForDefinition(
			PMNodeReference definitionIdentifier) {
		Set<PMNodeReference> useIdentifiers = _useIdentifiersByDefinitionIdentifier
				.get(definitionIdentifier);

		if (useIdentifiers == null) {
			useIdentifiers = new HashSet<PMNodeReference>();
			_useIdentifiersByDefinitionIdentifier.put(definitionIdentifier,
					useIdentifiers);
		}

		return useIdentifiers;
	}

	public void addUseForDefinition(PMNodeReference useIdentifier,
			PMNodeReference definitionIdentifier) {
		usesForDefinition(definitionIdentifier).add(useIdentifier);
	}

	public void removeUseForDefinition(PMNodeReference useIdentifier,
			PMNodeReference definitionIdentifier) {
		usesForDefinition(definitionIdentifier).remove(useIdentifier);
	}

	public void deleteDefinition(PMNodeReference definition) {
		// delete all uses of the definition

		for (PMNodeReference use : usesForDefinition(definition)) {
			removeDefinitionIdentifierForName(definition, use);
		}

		// delete list of uses for definition

		_useIdentifiersByDefinitionIdentifier.remove(definition);
	}

	public void addUseToModel(PMUse use) {
		SimpleName name = use.getSimpleName();

		PMNodeReference nameIdentifier = _project.getReferenceForNode(name);

		definitionIdentifiersForName(nameIdentifier); // To add an empty entry
														// to our store; gross.

		for (PMDef def : use.getReachingDefinitions()) {

			PMNodeReference definitionIdentifier;

			if (def != null) {
				ASTNode definingNode = def.getDefiningNode();
				definitionIdentifier = _project
						.getReferenceForNode(definingNode);

				if (definitionIdentifier == null)
					throw new RuntimeException(
							"Couldn't find identifier for defining node "
									+ definingNode);
			} else {
				definitionIdentifier = _uninitialized;
			}

			addDefinitionIdentifierForName(definitionIdentifier, nameIdentifier);
		}
	}

	protected void addUsesToModel(Collection<PMUse> uses) {
		for (PMUse use : uses) {
			addUseToModel(use);
		}
	}

	protected Collection<PMUse> getCurrentUses() {

		PMTimer.sharedTimer().start("DUUD_CHAINS");

		final Collection<PMUse> uses = new HashSet<PMUse>();

		for (ASTNode root : _project.getASTRoots()) {

			root.accept(new ASTVisitor() {
				public boolean visit(MethodDeclaration methodDeclaration) {

					// There is nothing to analyze if we have an interface or
					// abstract method
					if (methodDeclaration.getBody() != null) {
						PMRDefsAnalysis analysis = new PMRDefsAnalysis(
								methodDeclaration);

						uses.addAll(analysis.getUses());
					}

					return false; // don't visit children
				}
			});

		}

		PMTimer.sharedTimer().stop("DUUD_CHAINS");
		return uses;
	}

	protected void initializeModel() {
		addUsesToModel(getCurrentUses());

	}

	protected Collection<ASTNode> definingNodesForUse(PMUse use) {
		HashSet<ASTNode> definingNodes = new HashSet<ASTNode>();

		for (PMDef definition : use.getReachingDefinitions()) {
			if (definition != null) {
				definingNodes.add(definition.getDefiningNode());
			} else
				definingNodes.add(null);

		}

		return definingNodes;
	}

	public Collection<PMInconsistency> calculateInconsistencies() {

		final Collection<PMInconsistency> inconsistencies = new HashSet<PMInconsistency>();

		Collection<PMUse> uses = getCurrentUses();

		PMTimer.sharedTimer().start("INCONSISTENCIES");

		for (PMUse use : uses) {

			ASTNode usingNode = use.getSimpleName();

			Collection<ASTNode> currentDefiningNodes = definingNodesForUse(use);

			PMNodeReference useNameIdentifier = _project
					.getReferenceForNode(usingNode);

			if (useNameIdentifier != null) {
				Set<PMNodeReference> desiredDefinitionIdentifiers = _definitionIdentifiersByUseIdentifier
						.get(useNameIdentifier);

				if (desiredDefinitionIdentifiers != null) {
					// find definitions that should reach and missing
					// definitions

					for (PMNodeReference desiredDefinitionIdentifier : desiredDefinitionIdentifiers) {
						ASTNode desiredDefiningNode;

						if (!desiredDefinitionIdentifier.equals(_uninitialized)) {
							desiredDefiningNode = desiredDefinitionIdentifier
									.getNode();

							if (desiredDefiningNode == null)
								throw new RuntimeException(
										"Couldn't find defining node for identifier:"
												+ desiredDefinitionIdentifier
												+ "for use " + usingNode);
						} else {
							desiredDefiningNode = null;
						}

						if (!currentDefiningNodes.contains(desiredDefiningNode)) {

							inconsistencies
									.add(new PMMissingDefinition(
											_project,
											_project.findPMCompilationUnitForNode(usingNode),
											usingNode, desiredDefiningNode));

						}
					}

				} else {
					inconsistencies.add(new PMUnknownUse(_project, _project
							.findPMCompilationUnitForNode(usingNode), use
							.getSimpleName()));

					continue;
					// throw new
					// RuntimeException("Couldn't find stored mappings for use:"
					// + use.getSimpleName());
				}

				// Now check to make sure there aren't any extra defs
				// i.e. is every current defining node in the list of desired
				// efining nodes

				for (ASTNode currentDefiningNode : currentDefiningNodes) {
					PMNodeReference currentDefiningIdentifier = null;

					if (currentDefiningNode != null) {
						currentDefiningIdentifier = _project
								.getReferenceForNode(currentDefiningNode);

						if (currentDefiningIdentifier == null)
							new RuntimeException(
									"Couldn't find  identifier for current defining node "
											+ currentDefiningNode);
					} else
						currentDefiningIdentifier = _uninitialized;

					if (!desiredDefinitionIdentifiers
							.contains(currentDefiningIdentifier)) {
						inconsistencies
								.add(new PMExtraDefinition(
										_project,
										_project.findPMCompilationUnitForNode(usingNode),
										usingNode, currentDefiningNode));

					}
				}

			} else {
				throw new RuntimeException(
						"Couldn't find use identifier for use:"
								+ use.getSimpleName());
			}
		}

		PMTimer.sharedTimer().stop("INCONSISTENCIES");

		return inconsistencies;
	}
}
