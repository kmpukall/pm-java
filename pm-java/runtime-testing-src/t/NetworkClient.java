/*******************************************************************************
  Copyright (C) 2008 Christoph Reichenbach

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

*******************************************************************************/

package t;



public abstract class NetworkClient
{
	ProtocolStrategy s;
	public abstract void run();
	public abstract void connect(String connection);
	public abstract int send(String message);
	public abstract String receive();
}
