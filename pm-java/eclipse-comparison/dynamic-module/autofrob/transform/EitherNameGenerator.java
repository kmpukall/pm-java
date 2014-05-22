/*******************************************************************************
  Copyright (C) 2008 Christoph Reichenbach

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

*******************************************************************************/

package autofrob.transform;

import java.util.Random;

public class EitherNameGenerator extends NameGenerator
{
	private float left_probability;
	private NameGenerator left, right;
	private Random rand = new Random();

	public
	EitherNameGenerator(float left_probability, NameGenerator l, NameGenerator r)
	{
		this.left_probability = left_probability;
		this.left = l;
		this.right = r;
	}

	public String
	getRandomName(Location l)
	{
		if (rand.nextFloat() < left_probability)
			return this.left.getRandomName(l);
		else
			return this.right.getRandomName(l);
	}

	public void
	setProject(TProject tp)
	{
		this.left.setProject(tp);
		this.right.setProject(tp);
	}
}
