package com.termux.app.ownercall;

import androidx.annotation.NonNull;

public final class OwnerCallElapsedTime {

    private static final long MILLIS_PER_SECOND = 1000L;
    private static final long SECONDS_PER_MINUTE = 60L;
    private static final long MINUTES_PER_HOUR = 60L;
    private static final String SECONDS_SUFFIX = "s";
    private static final String MINUTES_SUFFIX = "m";
    private static final String HOURS_SUFFIX = "h";

    private OwnerCallElapsedTime() {
    }

    @NonNull
    public static String of(long calledAtMillis, long nowMillis) {
        long elapsedSeconds = Math.max(0L, (nowMillis - calledAtMillis) / MILLIS_PER_SECOND);
        if (elapsedSeconds < SECONDS_PER_MINUTE) {
            return elapsedSeconds + SECONDS_SUFFIX;
        }
        long elapsedMinutes = elapsedSeconds / SECONDS_PER_MINUTE;
        if (elapsedMinutes < MINUTES_PER_HOUR) {
            return elapsedMinutes + MINUTES_SUFFIX;
        }
        return elapsedMinutes / MINUTES_PER_HOUR + HOURS_SUFFIX;
    }
}
