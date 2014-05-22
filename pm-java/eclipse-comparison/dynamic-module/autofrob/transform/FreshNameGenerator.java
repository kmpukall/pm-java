/*******************************************************************************
  Copyright (C) 2008 Christoph Reichenbach

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

*******************************************************************************/

package autofrob.transform;

public class FreshNameGenerator extends NameGenerator
{
	int count = 0;
	String prefix;

	public
	FreshNameGenerator(String prefix)
	{
		this.prefix = prefix;
	}

	public String
	getRandomName(Location l)
	{
		return this.prefix + "__" + count++;
	}
}