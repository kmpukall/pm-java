/*******************************************************************************
  Copyright (C) 2008 Christoph Reichenbach

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

*******************************************************************************/

package t;


public abstract class UDPClient extends NetworkClient {
	public void connect(String connection) { System.out.println("UDP connect to " + connection); }
	public int send(String message) {System.out.println("UDP send `" + message + "'"); return message.length(); }
	public String receive() { System.out.println("UDP receive"); return null; };

}
