/*******************************************************************************
  Copyright (C) 2008 Christoph Reichenbach

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

*******************************************************************************/

package autofrob.model;

import java.util.LinkedList;
import java.util.*;

public abstract class AEnumerator<T>
{
	public abstract LinkedList<T>
	enumerate(AType t);

	public static <T> LinkedList<T>
	filter(Set<T> filter, LinkedList<T> source)
	{
		LinkedList<T> retval = new LinkedList<T>();

		for (T l : source)
			if (!filter.contains(l))
				retval.add(l);

		return retval;
	}

	public LinkedList<T>
	enumerateAndFilter(Set<T> filter, AType t)
	{
		return filter(filter, this.enumerate(t));
	}

}