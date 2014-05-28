/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

*******************************************************************************/

package net.creichen.pm;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.creichen.pm.inconsistencies.PMInconsistency;
import net.creichen.pm.inconsistencies.PMMarkerResolutionGenerator;
import net.creichen.pm.models.PMNameModel;
import net.creichen.pm.models.PMUDModel;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTRequestor;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.TypeParameter;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jface.text.ITextSelection;

public class PMProject {

	IJavaProject _iJavaProject;

	// Wow, there are two of these
	// Need to clean this up.

	Map<String, PMInconsistency> _currentInconsistencies;

	HashMap<String, PMCompilationUnitImplementation> _pmCompilationUnits; // keyed
	// off
	// ICompilationUnit.getHandleIdentifier

	ArrayList<PMProjectListener> _projectListeners;

	PMUDModel _udModel;

	PMNameModel _nameModel;

	PMPasteboard _pasteboard;

	PMNodeReferenceStore _nodeReferenceStore;

	protected PMProject(IJavaProject iJavaProject) {

		_nodeReferenceStore = new PMNodeReferenceStore();

		_iJavaProject = iJavaProject;

		_currentInconsistencies = new HashMap<String, PMInconsistency>();

		_pmCompilationUnits = new HashMap<String, PMCompilationUnitImplementation>();

		_projectListeners = new ArrayList<PMProjectListener>();

		_pasteboard = new PMPasteboard(this);

		updateToNewVersionsOfICompilationUnits(true);

	}

	public IJavaProject getIJavaProject() {
		return _iJavaProject;
	}

	public PMPasteboard getPasteboard() {
		return _pasteboard;
	}

	public void addProjectListener(PMProjectListener listener) {
		_projectListeners.add(listener);
	}

	public void removeProjectListener(PMProjectListener listener) {
		_projectListeners.remove(listener);
	}

	public ArrayList<PMProjectListener> projectListeners() {
		return new ArrayList<PMProjectListener>(_projectListeners);
	}

	public void syncSources() {
		if (sourcesAreOutOfSync()) {
			updateToNewVersionsOfICompilationUnits(true);
		}
	}

	public boolean sourcesAreOutOfSync() {
		for (ICompilationUnit iCompilationUnit : getSourceFilesForProject(_iJavaProject)) {
			if (!sourceIsUpToDateForICompilationUnit(iCompilationUnit))
				return true;
		}

		return false;
	}

	public boolean sourceIsUpToDateForICompilationUnit(
			ICompilationUnit iCompilationUnit) {
		PMCompilationUnitImplementation pmCompilationUnit = (PMCompilationUnitImplementation) getPMCompilationUnitForICompilationUnit(iCompilationUnit);

		return pmCompilationUnit.textHasChanged();
	}

	// For measurement purposes only
	public void justParseMeasurement(boolean resolveBindings) {
		Set<ICompilationUnit> iCompilationUnits = getSourceFilesForProject(_iJavaProject);

		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setProject(_iJavaProject);

		parser.setResolveBindings(resolveBindings);

		parser.createASTs(iCompilationUnits
				.toArray(new ICompilationUnit[iCompilationUnits.size()]),
				new String[0], new ASTRequestor() {
					public void acceptAST(ICompilationUnit source,
							CompilationUnit ast) {

					}
				}, null);

	}

	public void updateToNewVersionsOfICompilationUnits() {
		updateToNewVersionsOfICompilationUnits(false);
	}

	

	public void updateToNewVersionsOfICompilationUnits(boolean firstTime) {


		Set<ICompilationUnit> iCompilationUnits = getSourceFilesForProject(_iJavaProject);

		Set<ICompilationUnit> previouslyKnownCompilationUnits = allKnownICompilationUnits();

		// In future we will be smarter about detecting add/remove of
		// compilation units
		// and updating the models accordingly
		// for now we punt and have this reset the model
		if (!firstTime
				&& !iCompilationUnits.equals(previouslyKnownCompilationUnits)) {
			System.err
					.println("Previously known ICompilationUnits does not match current ICompilationUnits so resetting!!!");

			_pmCompilationUnits.clear();
			firstTime = true;
		}

		final boolean finalFirstTime = firstTime;

		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setProject(_iJavaProject);

		parser.setResolveBindings(true);

		ASTRequestor requestor = new ASTRequestor() {
			public void acceptAST(final ICompilationUnit source,
					CompilationUnit newCompilationUnit) {

				PMTimer.sharedTimer().start("PARSE_INTERNAL");

				PMCompilationUnitImplementation pmCompilationUnit = _pmCompilationUnits
						.get(source.getHandleIdentifier());

				// We don't handle deletions yet
				if (pmCompilationUnit == null) {
					// System.out.println("We have an ICompilationUnit we've
					// never seen before!");

					pmCompilationUnit = new PMCompilationUnitImplementation(
							source, newCompilationUnit);

					_pmCompilationUnits.put(source.getHandleIdentifier(),
							pmCompilationUnit);
				} else {

				}

				if (!finalFirstTime) {
					CompilationUnit oldCompilationUnit = parsedCompilationUnitForICompilationUnit(source);

					//debug
					try {
						//System.err.println("New source is " + source.getSource());
						//System.err.println("New ast compilation unit is " + newCompilationUnit);
						
						//System.err.println("Old ast compilation unit is " + oldCompilationUnit);
					} catch (Exception e) {
						throw new RuntimeException(e);			
					}
					
					
					if (recursivelyReplaceNodeWithCopy(oldCompilationUnit,
							newCompilationUnit)) {
						pmCompilationUnit
								.updatePair(source, newCompilationUnit);

					} else {
						System.err
								.println("Couldn't update to new version of compilation unit!");
						System.err.println("Old compilation unit: "
								+ oldCompilationUnit);
						System.err.println("New compilation unit: "
								+ newCompilationUnit);

						resetModel();
					}

				}

				PMTimer.sharedTimer().stop("PARSE_INTERNAL");
			}
		};

		parser.createASTs(iCompilationUnits
				.toArray(new ICompilationUnit[iCompilationUnits.size()]),
				new String[0], requestor, null);

		if (finalFirstTime) {
			resetModel();
		}

		for (PMProjectListener listener : projectListeners()) {
			listener.projectDidReparse(this);
		}

		_currentInconsistencies.clear();

	}

	private void resetModel() {
		_udModel = new PMUDModel(this);
		_nameModel = new PMNameModel(this);
	}

	private Set<ICompilationUnit> getSourceFilesForProject(
			IJavaProject iJavaProject) {
		Set<ICompilationUnit> result = new HashSet<ICompilationUnit>();

		try {
			for (IPackageFragment packageFragment : iJavaProject
					.getPackageFragments()) {
				if (packageFragment.getKind() == IPackageFragmentRoot.K_SOURCE
						&& packageFragment.containsJavaResources()) {
					for (ICompilationUnit iCompilationUnit : packageFragment
							.getCompilationUnits()) {

						result.add(iCompilationUnit);
					}

				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return result;
	}

	private Set<ICompilationUnit> getICompilationUnits() {
		return getSourceFilesForProject(_iJavaProject);
	}

	public ASTNode nodeForSelection(ITextSelection selection,
			ICompilationUnit iCompilationUnit) {

		CompilationUnit compilationUnit = (CompilationUnit) findASTRootForICompilationUnit(iCompilationUnit);

		ASTNode selectedNode = PMASTQuery.nodeForSelectionInCompilationUnit(
				selection.getOffset(), selection.getLength(), compilationUnit);

		return selectedNode;
	}

	public Set<SimpleName> relatedNodesForSimpleName(SimpleName simpleName) {
		Set<SimpleName> result = new HashSet<SimpleName>();

		result.addAll(_nameModel.nameNodesRelatedToNameNode(simpleName));

		return result;
	}

	public Set<PMCompilationUnit> getPMCompilationUnits() {
		Set<PMCompilationUnit> result = new HashSet<PMCompilationUnit>();

		result.addAll(_pmCompilationUnits.values());

		return result;
	}

	private Set<ICompilationUnit> allKnownICompilationUnits() {
		Set<ICompilationUnit> result = new HashSet<ICompilationUnit>();

		for (PMCompilationUnit pmCompilationUnit : getPMCompilationUnits()) {
			result.add(pmCompilationUnit.getICompilationUnit());
		}

		return result;
	}

	// Hmmm, this assumes there is only one simple name for a given declaring
	// node
	public SimpleName simpleNameForDeclaringNode(ASTNode declaringNode) {
		if (declaringNode != null) {
			if (declaringNode instanceof VariableDeclarationFragment) {
				return ((VariableDeclarationFragment) declaringNode).getName();
			} else if (declaringNode instanceof SingleVariableDeclaration) {
				return ((SingleVariableDeclaration) declaringNode).getName();
			} else if (declaringNode instanceof VariableDeclarationFragment) {
				return ((VariableDeclarationFragment) declaringNode).getName();
			} else if (declaringNode instanceof TypeDeclaration) {
				return ((TypeDeclaration) declaringNode).getName();
			} else if (declaringNode instanceof MethodDeclaration) {
				return ((MethodDeclaration) declaringNode).getName();
			} else if (declaringNode instanceof TypeParameter) {
				return ((TypeParameter) declaringNode).getName();
			} else {
				throw new RuntimeException("Unexpected declaring ASTNode type "
						+ declaringNode + " of class "
						+ declaringNode.getClass());
			}
		} else {
			throw new RuntimeException(
					"Tried to find simple name for null declaring node!");
		}

	}

	public ASTNode findDeclaringNodeForName(Name nameNode) {

		CompilationUnit usingCompilationUnit = (CompilationUnit) nameNode
				.getRoot();

		IBinding nameBinding = nameNode.resolveBinding();

		//System.out.println("Binding for " + nameNode + " in " + nameNode.getParent().getClass().getName() + " is " + binding);
		
		// It appears that name nodes like m in foo.m() have nil bindings here (but not always??)
		// we'll want to do the analysis through the method invocation's
		// resolveMethodBinding() to catch capture here
		// in the future

		if (nameBinding != null) {

			IJavaElement elementForBinding = nameBinding.getJavaElement();
			
			//Some name's bindings may not not have java elements (e.g. "length" in an array.length)
			//For now we ignore these, but in the future we need a way to make sure that array hasn't
			//been switched to have another type that also has a "length" element
			
			if (elementForBinding != null) {
				//System.out.println("java elementForBinding for " + nameNode + " in " + nameNode.getParent().getClass().getName() + " is " + elementForBinding);

				ICompilationUnit declaringICompilationUnit = (ICompilationUnit)elementForBinding.getAncestor(IJavaElement.COMPILATION_UNIT);
				
				
				//we may  not have the source to declaring compilation unit (e.g. for System.out.println())
				//in this case file-level representation would be an IClassFile, not an ICompilation unit
				//in this case we return null since we can't get an ASTNode from an IClassFile
				
									
				if (declaringICompilationUnit != null) {
					PMCompilationUnit declaringPMCompilationUnit = getPMCompilationUnitForICompilationUnit(declaringICompilationUnit);

					CompilationUnit declaringCompilationUnit = declaringPMCompilationUnit.getASTNode();
					
					ASTNode declaringNode = declaringCompilationUnit
							.findDeclaringNode(nameBinding);

					if (declaringNode == null) {
						declaringNode = usingCompilationUnit.findDeclaringNode(nameNode
								.resolveBinding().getKey());
					}

					return declaringNode;
				}
			}
			
		} 
			
		return null;

	}

	public boolean nameNodeIsDeclaring(SimpleName name) {
		return simpleNameForDeclaringNode(findDeclaringNodeForName(name)) == name;
	}
	
	public ASTNode declaringNodeForTypeName(String fullyQualifiedTypeName) {

		ASTNode declaringNode = null;

		try {
			IType type = _iJavaProject.findType(fullyQualifiedTypeName);

			if (type != null) {
				ICompilationUnit declaringIComputationUnit = (ICompilationUnit) type
						.getAncestor(IJavaElement.COMPILATION_UNIT);

				CompilationUnit declaringCompilationUnit = parsedCompilationUnitForICompilationUnit(declaringIComputationUnit);

				if (declaringCompilationUnit != null) {
					declaringNode = declaringCompilationUnit
							.findDeclaringNode(type.getKey());
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return declaringNode;
	}

	// Change to findPMCompilationUnitForNode??
	public PMCompilationUnit findPMCompilationUnitForNode(ASTNode node) {
		return _pmCompilationUnits
				.get(((ICompilationUnit) ((CompilationUnit) node.getRoot())
						.getJavaElement()).getHandleIdentifier());
	}

	public PMCompilationUnit getPMCompilationUnitForICompilationUnit(
			ICompilationUnit iCompilationUnit) {
		return _pmCompilationUnits.get(iCompilationUnit.getHandleIdentifier());
	}

	public boolean recursivelyReplaceNodeWithCopy(ASTNode node, ASTNode copy) {

		PMTimer.sharedTimer().start("NODE_REPLACEMENT");

		// It's kind of silly that we have to match twice

		PMASTMatcher astMatcher = new PMASTMatcher(node, copy);

		boolean matches = astMatcher.match();

		if (matches) {

			Map<ASTNode, ASTNode> isomorphicNodes = astMatcher
					.isomorphicNodes();

			for (ASTNode oldNode : isomorphicNodes.keySet()) {

				ASTNode newNode = isomorphicNodes.get(oldNode);

				if (oldNode instanceof SimpleName) {
					_nameModel.replaceNameWithName((SimpleName) oldNode,
							(SimpleName) newNode);
				}

				replaceNodeWithNode(oldNode, newNode);

			}

		} else {
			System.err.println("Copy [" + copy
					+ "] does not structurally match original [" + node + "]");
			throw new RuntimeException(
					"Copy not does structurally match original");
		}

		PMTimer.sharedTimer().stop("NODE_REPLACEMENT");

		return matches;
	}

	public CompilationUnit parsedCompilationUnitForICompilationUnit(
			ICompilationUnit iCompilationUnit) {
		return _pmCompilationUnits.get(iCompilationUnit.getHandleIdentifier())
				.getASTNode();
	}

	public PMNodeReference getReferenceForNode(ASTNode node) {
		return _nodeReferenceStore.getReferenceForNode(node);
	}

	public void replaceNodeWithNode(ASTNode oldNode, ASTNode newNode) {
		_nodeReferenceStore.replaceOldNodeVersionWithNewVersion(oldNode,
				newNode);
	}

	public Collection<ASTNode> getASTRoots() {
		Collection<ASTNode> roots = new HashSet<ASTNode>();

		for (PMCompilationUnit pmCompilationUnit : _pmCompilationUnits.values()) {
			roots.add(pmCompilationUnit.getASTNode());
		}

		return roots;
	}

	public ASTNode findASTRootForICompilationUnit(
			ICompilationUnit iCompilationUnit) {
		return parsedCompilationUnitForICompilationUnit(iCompilationUnit);
	}

	public PMUDModel getUDModel() {
		return _udModel;
	}

	public PMNameModel getNameModel() {
		return _nameModel;
	}

	public Set<PMInconsistency> allInconsistencies() {
		HashSet<PMInconsistency> result = new HashSet<PMInconsistency>();

		result.addAll(_currentInconsistencies.values());

		return result;
	}

	public void rescanForInconsistencies() {

		try {

			_currentInconsistencies.clear();

			PMTimer.sharedTimer().start("INCONSISTENCIES");

			Set<PMInconsistency> inconsistencySet = new HashSet<PMInconsistency>();

			inconsistencySet.addAll(_nameModel.calculateInconsistencies());
			inconsistencySet.addAll(_udModel.calculateInconsistencies());

			PMTimer.sharedTimer().stop("INCONSISTENCIES");

			// delete previous markers
			for (ICompilationUnit iCompilationUnit : getICompilationUnits()) {

				iCompilationUnit.getResource().deleteMarkers(
						"org.eclipse.core.resources.problemmarker", false,
						IResource.DEPTH_ZERO);
			}

			for (PMInconsistency inconsistency : inconsistencySet) {
				IResource resource = findPMCompilationUnitForNode(
						inconsistency.getNode()).getICompilationUnit()
						.getResource();

				IMarker marker = resource
						.createMarker("org.eclipse.core.resources.problemmarker");

				marker.setAttribute(
						PMMarkerResolutionGenerator.INCONSISTENCY_ID,
						inconsistency.getID());
				marker.setAttribute(PMMarkerResolutionGenerator.PROJECT_ID,
						_iJavaProject.getHandleIdentifier());

				marker.setAttribute(
						PMMarkerResolutionGenerator.ACCEPTS_BEHAVIORAL_CHANGE,
						inconsistency.allowsAcceptBehavioralChange());

				marker.setAttribute(IMarker.MESSAGE, inconsistency
						.getHumanReadableDescription());
				marker.setAttribute(IMarker.TRANSIENT, true);

				ASTNode node = inconsistency.getNode();

				marker
						.setAttribute(IMarker.CHAR_START, node
								.getStartPosition());
				marker.setAttribute(IMarker.CHAR_END, node.getStartPosition()
						+ node.getLength());

				_currentInconsistencies.put(inconsistency.getID(),
						inconsistency);
			}

		} catch (CoreException e) {
			e.printStackTrace();

			throw new RuntimeException(e);
		}
	}

	public PMInconsistency getInconsistencyWithKey(String key) {
		return _currentInconsistencies.get(key);
	}

	private class PMCompilationUnitImplementation implements PMCompilationUnit {
		ICompilationUnit _iCompilationUnit;

		byte[] _sourceDigest;

		CompilationUnit _compilationUnit;

		public PMCompilationUnitImplementation(
				ICompilationUnit iCompilationUnit,
				CompilationUnit compilationUnit) {
			updatePair(iCompilationUnit, compilationUnit);
		}

		public String getSource() {
			try {
				return _iCompilationUnit.getSource();
			} catch (Exception e) {
				e.printStackTrace();
			}

			return null;
		}

		public CompilationUnit getASTNode() {
			return _compilationUnit;
		}

		public ICompilationUnit getICompilationUnit() {
			return _iCompilationUnit;
		}

		// we parse more than one compilation unit at once (since this makes it
		// faster) in project and then pass
		// the newly parsed ast to to the pmcompilationunit with this method.

		protected void updatePair(ICompilationUnit iCompilationUnit,
				CompilationUnit compilationUnit) {
			_compilationUnit = compilationUnit;
			_iCompilationUnit = iCompilationUnit;

			updateSourceDigestForSource(getSource());
		}

		private byte[] calculatedHashForSource(String source) {

			try {
				MessageDigest digest = java.security.MessageDigest
						.getInstance("SHA");
				digest.update(source.getBytes()); // Encoding issues here?

				return digest.digest();
			} catch (Exception e) {
				e.printStackTrace();

				return null;
			}

		}

		private void updateSourceDigestForSource(String source) {
			_sourceDigest = calculatedHashForSource(source);
		}

		public void rename(String newName) {
			try {

				IPackageFragment parentPackageFragment = (IPackageFragment) _iCompilationUnit
						.getParent();

				PMProject.this._pmCompilationUnits.remove(_iCompilationUnit
						.getHandleIdentifier());

				_iCompilationUnit.rename(newName + ".java", false, null);

				ICompilationUnit newICompilationUnit = parentPackageFragment
						.getCompilationUnit(newName + ".java");

				_iCompilationUnit = newICompilationUnit;

				PMProject.this._pmCompilationUnits.put(newICompilationUnit
						.getHandleIdentifier(), this);

			} catch (Exception e) {

				e.printStackTrace();

				throw new RuntimeException(e);
			}
		}

		public boolean textHasChanged() {
			return Arrays.equals(calculatedHashForSource(getSource()),
					_sourceDigest);
		}
	}

}
