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
public final class UsesDependencyComputer extends OwnerStackDependencyComputer
{
	protected void
	recordUse(IJavaElement member)
	{
		final IMember owner = this.getOwner();
		//		System.err.println("Owner " + owner + " (true owner: " + this.have(owner) + "): " + member);
		if (owner == null)
			return;
		else
			if (this.have(owner))
				this.add(member);
					
	}

	public boolean
	visit(MethodInvocation invocation)
	{
		this.recordUse(invocation.getName().resolveBinding().getJavaElement());
		return true;
	}

	public boolean
	visit(SimpleName name)
	{
		this.recordUse(name.resolveBinding().getJavaElement());
		return true;
	}
}