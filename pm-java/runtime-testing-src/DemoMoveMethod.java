/*******************************************************************************
  Copyright (C) 2008 Christoph Reichenbach

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

*******************************************************************************/


public class DemoMoveMethod {
	static public class S {
		void methodA() {
			System.out.println("SuperClass's methodA()");
		}
		
		void callsA() {
			methodA();
		}
		
	}
	
	static public class T extends S{
		
		void methodA() {
			System.err.println("Another methodA()");
		}

		

		
	}
}
