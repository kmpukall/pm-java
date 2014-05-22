/*******************************************************************************
  Copyright (C) 2008 Christoph Reichenbach

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

*******************************************************************************/

package autofrob.scoring;

public enum ResultKind {
	TRANSFORM_DISALLOWED('-'),
	TRANSFORM_OK('+'),
	TRANSFORM_ERRONEOUS('!');

	private char short_name;

	ResultKind(char c)
	{
		short_name = c;
	}

	public char
	getChar()
	{
		return short_name;
	}
};

