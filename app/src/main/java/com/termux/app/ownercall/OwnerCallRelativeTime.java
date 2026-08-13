package com.termux.app.ownercall;

import androidx.annotation.NonNull;

public final class OwnerCallRelativeTime {

    private static final long MILLIS_PER_SECOND = 1000L;
    private static final long SECONDS_PER_MINUTE = 60L;
    private static final long MINUTES_PER_HOUR = 60L;

    private OwnerCallRelativeTime() {
    }

    @NonNull
    public static String of(long calledAtMillis, long nowMillis) {
        long elapsedSeconds = Math.max(0L, (nowMillis - calledAtMillis) / MILLIS_PER_SECOND);
        if (elapsedSeconds < SECONDS_PER_MINUTE) {
            return elapsedSeconds + "秒前";
        }
        long elapsedMinutes = elapsedSeconds / SECONDS_PER_MINUTE;
        if (elapsedMinutes < MINUTES_PER_HOUR) {
            return elapsedMinutes + "分前";
        }
        return elapsedMinutes / MINUTES_PER_HOUR + "時間前";
    }
}
