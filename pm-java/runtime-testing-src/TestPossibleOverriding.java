/*******************************************************************************
  Copyright (C) 2008 Christoph Reichenbach

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

*******************************************************************************/


public class TestPossibleOverriding {
	public static class S {
		public void methodToOverride() {
			System.out.println("Foo!");
		}
	}
	
	public static class T extends S {
		//We should catch that there is a problem when we rename this to 'methodToOverride'
		public void methodToRenameAndThusOverrideMethodInS() {
			System.out.println("bar");
		}
	}
	
	
	public void someMethod() {
		S s = new T(); //PM detects the problem when s is of declared type T but not declared type S
		
		s.methodToOverride();
		
	}
}
