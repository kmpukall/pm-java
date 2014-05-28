/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm;

import java.util.HashMap;

public class PMTimer {

	protected HashMap<String, Long> _accumulatedMilliseconds;

	protected HashMap<String, Long> _startTimes;

	public PMTimer() {
		_accumulatedMilliseconds = new HashMap<String, Long>();
		_startTimes = new HashMap<String, Long>();

	}

	protected static PMTimer _sharedTimer;

	static public PMTimer sharedTimer() {
		if (_sharedTimer == null) {
			_sharedTimer = new PMTimer();
		}

		return _sharedTimer;
	}

	public void start(String key) {
		_startTimes.put(key, System.currentTimeMillis());

		if (!_accumulatedMilliseconds.containsKey(key))
			_accumulatedMilliseconds.put(key, 0L);
	}

	public void stop(String key) {
		long stopTime = System.currentTimeMillis();

		long startTime = _startTimes.get(key);

		long accumulatedTime = _accumulatedMilliseconds.get(key);

		accumulatedTime += (stopTime - startTime);

		_accumulatedMilliseconds.put(key, accumulatedTime);
	}

	public void clear(String key) {
		_accumulatedMilliseconds.remove(key);
		_startTimes.remove(key);
	}

	public double accumulatedSecondsForKey(String key) {
		if (_accumulatedMilliseconds.containsKey(key)) {
			return ((double) _accumulatedMilliseconds.get(key)) / 1000.0;
		} else {
			System.err.println("ERROR: no accumulated time for key: " + key);
			return 0.0;
		}

	}
}
