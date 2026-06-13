package com.termux.app.terminal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

public class SessionBellNotificationStore {

    private static final long ONE_SECOND_MILLIS = 1000L;
    private static final long ONE_MINUTE_MILLIS = 60L * ONE_SECOND_MILLIS;
    private static final long ONE_HOUR_MILLIS = 60L * ONE_MINUTE_MILLIS;

    private final Map<String, Long> mBellArrivalTimeMillisByHandle = new HashMap<>();

    public void recordBell(@NonNull String sessionHandle, long arrivalTimeMillis) {
        mBellArrivalTimeMillisByHandle.put(sessionHandle, arrivalTimeMillis);
    }

    public void clearBell(@NonNull String sessionHandle) {
        mBellArrivalTimeMillisByHandle.remove(sessionHandle);
    }

    public boolean hasPendingNotification(@NonNull String sessionHandle) {
        return mBellArrivalTimeMillisByHandle.containsKey(sessionHandle);
    }

    @Nullable
    public Long getBellArrivalTimeMillis(@NonNull String sessionHandle) {
        return mBellArrivalTimeMillisByHandle.get(sessionHandle);
    }

    public static String formatRelativeTime(long elapsedMillis) {
        long clampedElapsedMillis = Math.max(0L, elapsedMillis);
        if (clampedElapsedMillis < ONE_MINUTE_MILLIS) {
            return (clampedElapsedMillis / ONE_SECOND_MILLIS) + "s ago";
        }
        if (clampedElapsedMillis < ONE_HOUR_MILLIS) {
            return (clampedElapsedMillis / ONE_MINUTE_MILLIS) + "m ago";
        }
        return (clampedElapsedMillis / ONE_HOUR_MILLIS) + "h ago";
    }
}
