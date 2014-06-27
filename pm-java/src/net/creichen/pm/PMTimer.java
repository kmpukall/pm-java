/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm;

import java.util.HashMap;

public class PMTimer {

    private static final double MILLISECONDS_PER_SECOND = 1000.0;

    private final HashMap<String, Long> accumulatedMilliseconds;

    private final HashMap<String, Long> startTimes;

    private static PMTimer sharedTimer;

    public static PMTimer sharedTimer() {
        if (sharedTimer == null) {
            sharedTimer = new PMTimer();
        }

        return sharedTimer;
    }

    public PMTimer() {
        accumulatedMilliseconds = new HashMap<String, Long>();
        startTimes = new HashMap<String, Long>();

    }

    public double accumulatedSecondsForKey(final String key) {
        if (accumulatedMilliseconds.containsKey(key)) {
            return ((double) accumulatedMilliseconds.get(key)) / MILLISECONDS_PER_SECOND;
        } else {
            System.err.println("ERROR: no accumulated time for key: " + key);
            return 0.0;
        }

    }

    public void clear(final String key) {
        accumulatedMilliseconds.remove(key);
        startTimes.remove(key);
    }

    public void start(final String key) {
        startTimes.put(key, System.currentTimeMillis());

        if (!accumulatedMilliseconds.containsKey(key)) {
            accumulatedMilliseconds.put(key, 0L);
        }
    }

    public void stop(final String key) {
        final long stopTime = System.currentTimeMillis();

        final long startTime = startTimes.get(key);

        long accumulatedTime = accumulatedMilliseconds.get(key);

        accumulatedTime += (stopTime - startTime);

        accumulatedMilliseconds.put(key, accumulatedTime);
    }
}
