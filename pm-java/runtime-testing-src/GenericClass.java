/*******************************************************************************
  Copyright (C) 2008 Christoph Reichenbach

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

*******************************************************************************/


public class GenericClass<T> {
	T _ivar;
	

	
	T getIvar() {
		return _ivar;
	}
	
	static public class User {
		
		GenericClass<String> f = new GenericClass<String>();
		
		
		void method() {
			
			String result = f.getIvar();
		}
	}
	
}
