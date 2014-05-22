/*******************************************************************************
  Copyright (C) 2008 Christoph Reichenbach

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

*******************************************************************************/


public class TestExtractLocalVariable {

	public int someValue() {
		return 6;
	}
	
	
	public int anotherValue() {
		System.out.println("foo");
		
		return 7;
	}
	
	public void method() {
		
		int i = someValue() + anotherValue() + 7;
		
	}
}
