/*******************************************************************************
  Copyright (C) 2008 Christoph Reichenbach

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

*******************************************************************************/


public class TestDelegation extends ClassC {
	TestDelegation _t;

	public void cMethod(String arg1) {
		
	}
	
	void aTMethod() {
		
		_t.getClass();
		
		_t.aTMethod();
	}
	
	
	void method() {
		TestDelegation t = new TestDelegation();
		t.getClass();
		
		ClassC c = new ClassC();
		
		c.getClass();
		
		cMethod("foo");
	}
	
	
}
