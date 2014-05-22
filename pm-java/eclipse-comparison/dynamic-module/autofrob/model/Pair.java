/*******************************************************************************
  Copyright (C) 2008 Christoph Reichenbach

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

*******************************************************************************/

package autofrob.model;

public class Pair<L, R>
{
	private L left;
	private R right;

	public
	Pair(L l, R r)
	{
		this.left = l;
		this.right = r;
	}

	public L
	getLeft()
	{
		return left;
	}

	public R
	getRight()
	{
		return right;
	}

	@Override
	public String
	toString()
	{
		return "<" + this.left + ", " + this.right + ">";
	}

	@Override
	public boolean
	equals(Object o)
	{
		if (o == null)
			return false;

		if (o instanceof Pair<?, ?>) {
			Pair<?, ?> other = (Pair<?, ?>) o;

			return other.left.equals(this.left)
				&& other.right.equals(this.right);
		}
		return false;
	}

	@Override
	public int
	hashCode()
	{
		return this.left.hashCode() ^ this.right.hashCode();
	}
}