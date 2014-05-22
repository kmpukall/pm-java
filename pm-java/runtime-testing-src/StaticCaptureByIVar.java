/*******************************************************************************
  Copyright (C) 2008 Christoph Reichenbach

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

*******************************************************************************/


public class StaticCaptureByIVar {

	/**
	 * @param args
	 */
	
	static StaticCaptureByIVar Bar = new StaticCaptureByIVar();
	
	
	//rename this class to Bar to get a name capture allowed by Refactorings but caught by PM
	static class foo {
		public static void method() {
			System.out.println("foo method");
		}
		
		
	}
	
	void method() {
		
		System.out.println("StaticCaptureByIVar  method");
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub

		
		System.out.println("Hello World!");
	
	
		StaticCaptureByIVar.foo.method();
	}

}
