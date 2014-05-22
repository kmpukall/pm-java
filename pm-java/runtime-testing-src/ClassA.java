/*******************************************************************************
  Copyright (C) 2008 Christoph Reichenbach

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

*******************************************************************************/


public class ClassA {
	ClassB _classB;
	ClassA _classA;
	
	private String _testIvar;
	
	
	public void f(ClassB a) {
		g(a);
		
		System.out.println(_testIvar);
	}
	
	public void g(ClassB b) {
		f(b);
	}
	
	
	public void method() {
		System.out.println(_testIvar);
		
	}
}
