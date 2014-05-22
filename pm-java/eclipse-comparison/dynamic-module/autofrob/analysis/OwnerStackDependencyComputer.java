/*******************************************************************************
  Copyright (C) 2008 Christoph Reichenbach

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

*******************************************************************************/

package autofrob.analysis;

import org.eclipse.jdt.core.*;
import org.eclipse.jdt.core.dom.*;

import autofrob.model.Pair;

import java.util.*;

/**
 * Find all methods and fields that use the specified methods and fields
 */
public class OwnerStackDependencyComputer extends DependencyComputer
{
	protected Stack<IJavaElement> owners = new Stack<IJavaElement>();

	protected IMember
	getOwner() // may return null
	{
		if (owners.empty())
			return null;
		if (owners.peek() instanceof IMember)
			return (IMember) owners.peek();
		else
			return null;
	}

	private void
	push(IJavaElement e)
	{
		owners.push(e);
	}


	public void endVisit(MethodDeclaration _)		{ this.owners.pop(); }
	public void endVisit(SingleVariableDeclaration _)	{ this.owners.pop(); }
	public void endVisit(VariableDeclarationFragment _)	{ this.owners.pop(); }

	public boolean visit(MethodDeclaration md)		{ this.push(md.resolveBinding().getJavaElement()); return true; }
	public boolean visit(SingleVariableDeclaration svd)	{ this.push(svd.getName().resolveBinding().getJavaElement()); return true; }
	public boolean visit(VariableDeclarationFragment vdf)	{ this.push(vdf.getName().resolveBinding().getJavaElement()); return true; }
}