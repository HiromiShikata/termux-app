package com.termux.app.terminal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

public class SessionNewActivityStore {

    private static final long ONE_SECOND_MILLIS = 1000L;
    private static final long ONE_MINUTE_MILLIS = 60L * ONE_SECOND_MILLIS;
    private static final long ONE_HOUR_MILLIS = 60L * ONE_MINUTE_MILLIS;

    private final Map<String, Long> mLastBellTimeMillisByHandle = new HashMap<>();
    private final Map<String, Long> mLastSeenTimeMillisByHandle = new HashMap<>();

    public void recordBell(@NonNull String sessionHandle, long bellTimeMillis) {
        mLastBellTimeMillisByHandle.put(sessionHandle, bellTimeMillis);
    }

    public void recordSeen(@NonNull String sessionHandle, long seenTimeMillis) {
        mLastSeenTimeMillisByHandle.put(sessionHandle, seenTimeMillis);
    }

    public void purgeSession(@NonNull String sessionHandle) {
        mLastBellTimeMillisByHandle.remove(sessionHandle);
        mLastSeenTimeMillisByHandle.remove(sessionHandle);
    }

    @Nullable
    public Long getLastBellTimeMillis(@NonNull String sessionHandle) {
        return mLastBellTimeMillisByHandle.get(sessionHandle);
    }

    @Nullable
    public Long getLastSeenTimeMillis(@NonNull String sessionHandle) {
        return mLastSeenTimeMillisByHandle.get(sessionHandle);
    }

    public boolean hasUnseenBell(@NonNull String sessionHandle) {
        return SessionNewActivityIndicator.isBellUnseen(
            getLastBellTimeMillis(sessionHandle), getLastSeenTimeMillis(sessionHandle));
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
