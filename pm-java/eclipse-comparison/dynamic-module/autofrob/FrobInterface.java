/*******************************************************************************
  Copyright (C) 2008 Christoph Reichenbach

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

*******************************************************************************/

package autofrob;

import org.eclipse.jdt.apt.core.build.JdtApt;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.equinox.app.IApplication;

public interface FrobInterface
{
	public void
	run(IApplication master, IApplicationContext ctx, String [] args);
}
