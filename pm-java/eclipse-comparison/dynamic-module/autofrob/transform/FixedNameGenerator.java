/*******************************************************************************
  Copyright (C) 2008 Christoph Reichenbach

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

*******************************************************************************/

package autofrob.transform;

public class FixedNameGenerator extends NameGenerator
{
	String fixed_name;

	public
	FixedNameGenerator(String fixed_name)
	{
		this.fixed_name = fixed_name;
	}

	public String
	getRandomName(Location l)
	{
		return this.fixed_name;
	}
}