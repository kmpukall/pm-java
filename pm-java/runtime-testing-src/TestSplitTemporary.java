/*******************************************************************************
  Copyright (C) 2008 Christoph Reichenbach

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

*******************************************************************************/


public class TestSplitTemporary {
	
	int y;
	
	public void method() {
		int x;
		int z;
		
		x = 4;
		
		z = x + 1;
		
		int y = z; 
		
		z = x*2;
		
		x = 6 + x;
		
		z = x;
		
		y++;
		
		y = 4;
	}
}
