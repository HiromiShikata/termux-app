package com.termux.app.terminal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

public class SessionNewActivityStore {

    private static final long ONE_SECOND_MILLIS = 1000L;
    private static final long ONE_MINUTE_MILLIS = 60L * ONE_SECOND_MILLIS;
    private static final long ONE_HOUR_MILLIS = 60L * ONE_MINUTE_MILLIS;

    private final Map<String, Long> mArrivalTimeMillisByHandle = new HashMap<>();

    public void markNewActivity(@NonNull String sessionHandle, long arrivalTimeMillis) {
        mArrivalTimeMillisByHandle.put(sessionHandle, arrivalTimeMillis);
    }

    public void clearNewActivity(@NonNull String sessionHandle) {
        mArrivalTimeMillisByHandle.remove(sessionHandle);
    }

    public void purgeSession(@NonNull String sessionHandle) {
        mArrivalTimeMillisByHandle.remove(sessionHandle);
    }

    public boolean hasNewActivity(@NonNull String sessionHandle) {
        return mArrivalTimeMillisByHandle.containsKey(sessionHandle);
    }

    @Nullable
    public Long getArrivalTimeMillis(@NonNull String sessionHandle) {
        return mArrivalTimeMillisByHandle.get(sessionHandle);
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
