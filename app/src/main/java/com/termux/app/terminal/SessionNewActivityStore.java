package com.termux.app.terminal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SessionNewActivityStore {

    private static final long ONE_SECOND_MILLIS = 1000L;
    private static final long ONE_MINUTE_MILLIS = 60L * ONE_SECOND_MILLIS;
    private static final long ONE_HOUR_MILLIS = 60L * ONE_MINUTE_MILLIS;

    private final Map<String, Long> mLastBellTimeMillisByHandle = new HashMap<>();
    private final Map<String, Long> mLastSeenTimeMillisByHandle = new HashMap<>();

    @NonNull
    private final SessionNewActivityPersistence mPersistence;

    public SessionNewActivityStore() {
        this(new InMemorySessionNewActivityPersistence());
    }

    public SessionNewActivityStore(@NonNull SessionNewActivityPersistence persistence) {
        mPersistence = persistence;
        for (SessionNewActivityState state : persistence.load()) {
            if (state.getLastBellTimeMillis() != null)
                mLastBellTimeMillisByHandle.put(state.getHandle(), state.getLastBellTimeMillis());
            if (state.getLastSeenTimeMillis() != null)
                mLastSeenTimeMillisByHandle.put(state.getHandle(), state.getLastSeenTimeMillis());
        }
    }

    public void recordBell(@NonNull String sessionHandle, long bellTimeMillis) {
        mLastBellTimeMillisByHandle.put(sessionHandle, bellTimeMillis);
        save();
    }

    public void recordSeen(@NonNull String sessionHandle, long seenTimeMillis) {
        mLastSeenTimeMillisByHandle.put(sessionHandle, seenTimeMillis);
        save();
    }

    public void purgeSession(@NonNull String sessionHandle) {
        mLastBellTimeMillisByHandle.remove(sessionHandle);
        mLastSeenTimeMillisByHandle.remove(sessionHandle);
        save();
    }

    public void pruneToHandles(@NonNull Set<String> knownHandles) {
        boolean changed = mLastBellTimeMillisByHandle.keySet().retainAll(knownHandles);
        changed |= mLastSeenTimeMillisByHandle.keySet().retainAll(knownHandles);
        if (changed)
            save();
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

    private void save() {
        Set<String> handles = new HashSet<>(mLastBellTimeMillisByHandle.keySet());
        handles.addAll(mLastSeenTimeMillisByHandle.keySet());
        List<SessionNewActivityState> states = new ArrayList<>();
        for (String handle : handles) {
            states.add(new SessionNewActivityState(handle,
                mLastBellTimeMillisByHandle.get(handle), mLastSeenTimeMillisByHandle.get(handle)));
        }
        mPersistence.save(states);
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
