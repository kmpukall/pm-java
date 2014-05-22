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
public final class UsedByDependencyComputer extends OwnerStackDependencyComputer
{
	protected void
	recordOwner()
	{
		final IMember owner = this.getOwner();
		if (owner == null)
			return;
		else
			this.add(owner);
	}

	public boolean
	visit(MethodInvocation invocation)
	{
		if (this.have(invocation.getName().resolveBinding().getJavaElement()))
			recordOwner();
		return true;
	}

	public boolean
	visit(SimpleName name)
	{
		if (this.have(name.resolveBinding().getJavaElement()))
			recordOwner();
		return true;
	}
}