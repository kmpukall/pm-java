/*******************************************************************************
  Copyright (C) 2008 Devin Coughlin

  This program is provided WITHOUT WARRANY of any kind, either expressed or
  implied.  Please refer to the included file LICENCE, detailing the terms of
  the GNU Lesser General Public Licence v3.0 or later, for details.

 *******************************************************************************/

package net.creichen.pm.utils;

import java.util.HashMap;

public class Timer {

    private static final double MILLISECONDS_PER_SECOND = 1000.0;

    private final HashMap<String, Long> accumulatedMilliseconds;

    private final HashMap<String, Long> startTimes;

    private static Timer sharedTimer;

    public static Timer sharedTimer() {
        if (sharedTimer == null) {
            sharedTimer = new Timer();
        }

        return sharedTimer;
    }

    public Timer() {
        this.accumulatedMilliseconds = new HashMap<String, Long>();
        this.startTimes = new HashMap<String, Long>();

    }

    public double accumulatedSecondsForKey(final String key) {
        if (this.accumulatedMilliseconds.containsKey(key)) {
            return ((double) this.accumulatedMilliseconds.get(key)) / MILLISECONDS_PER_SECOND;
        } else {
            System.err.println("ERROR: no accumulated time for key: " + key);
            return 0.0;
        }

    }

    public void clear(final String key) {
        this.accumulatedMilliseconds.remove(key);
        this.startTimes.remove(key);
    }

    public void start(final String key) {
        this.startTimes.put(key, System.currentTimeMillis());

        if (!this.accumulatedMilliseconds.containsKey(key)) {
            this.accumulatedMilliseconds.put(key, 0L);
        }
    }

    public void stop(final String key) {
        final long stopTime = System.currentTimeMillis();

        final long startTime = this.startTimes.get(key);

        long accumulatedTime = this.accumulatedMilliseconds.get(key);

        accumulatedTime += stopTime - startTime;

        this.accumulatedMilliseconds.put(key, accumulatedTime);
    }
}
