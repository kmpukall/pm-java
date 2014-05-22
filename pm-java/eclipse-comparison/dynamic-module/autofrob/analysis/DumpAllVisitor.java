/*******************************************************************************
  Copyright (C) 2008 Christoph Reichenbach

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

*******************************************************************************/

package autofrob.analysis;

import org.eclipse.jdt.core.*;
import org.eclipse.jdt.core.dom.*;

public class DumpAllVisitor extends ASTVisitor
{
	public void
	preVisit(ASTNode n)
	{
		System.err.println("    " + n.getClass() + " : " + n.toString().replace("\n", " "));
	}
}