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

    private final Map<String, Long> mLastBellTimeMillisByName = new HashMap<>();
    private final Map<String, Long> mLastSeenTimeMillisByName = new HashMap<>();

    @NonNull
    private final SessionNewActivityPersistence mPersistence;

    public SessionNewActivityStore() {
        this(new InMemorySessionNewActivityPersistence());
    }

    public SessionNewActivityStore(@NonNull SessionNewActivityPersistence persistence) {
        mPersistence = persistence;
        for (SessionNewActivityState state : persistence.load()) {
            if (state.getLastBellTimeMillis() != null)
                mLastBellTimeMillisByName.put(state.getSessionName(), state.getLastBellTimeMillis());
            if (state.getLastSeenTimeMillis() != null)
                mLastSeenTimeMillisByName.put(state.getSessionName(), state.getLastSeenTimeMillis());
        }
    }

    public void recordBell(@NonNull String sessionName, long bellTimeMillis) {
        mLastBellTimeMillisByName.put(sessionName, bellTimeMillis);
        save();
    }

    public void recordSeen(@NonNull String sessionName, long seenTimeMillis) {
        mLastSeenTimeMillisByName.put(sessionName, seenTimeMillis);
        save();
    }

    public void purgeSession(@NonNull String sessionName) {
        mLastBellTimeMillisByName.remove(sessionName);
        mLastSeenTimeMillisByName.remove(sessionName);
        save();
    }

    public void pruneToSessionNames(@NonNull Set<String> knownSessionNames) {
        boolean changed = mLastBellTimeMillisByName.keySet().retainAll(knownSessionNames);
        changed |= mLastSeenTimeMillisByName.keySet().retainAll(knownSessionNames);
        if (changed)
            save();
    }

    @Nullable
    public Long getLastBellTimeMillis(@NonNull String sessionName) {
        return mLastBellTimeMillisByName.get(sessionName);
    }

    @Nullable
    public Long getLastSeenTimeMillis(@NonNull String sessionName) {
        return mLastSeenTimeMillisByName.get(sessionName);
    }

    public boolean hasUnseenBell(@NonNull String sessionName) {
        return SessionNewActivityIndicator.isBellUnseen(
            getLastBellTimeMillis(sessionName), getLastSeenTimeMillis(sessionName));
    }

    private void save() {
        Set<String> sessionNames = new HashSet<>(mLastBellTimeMillisByName.keySet());
        sessionNames.addAll(mLastSeenTimeMillisByName.keySet());
        List<SessionNewActivityState> states = new ArrayList<>();
        for (String sessionName : sessionNames) {
            states.add(new SessionNewActivityState(sessionName,
                mLastBellTimeMillisByName.get(sessionName), mLastSeenTimeMillisByName.get(sessionName)));
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
