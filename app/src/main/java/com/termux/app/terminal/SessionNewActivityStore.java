package com.termux.app.terminal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SessionNewActivityStore {

    private static final long ONE_SECOND_MILLIS = 1000L;
    private static final long ONE_MINUTE_MILLIS = 60L * ONE_SECOND_MILLIS;
    private static final long ONE_HOUR_MILLIS = 60L * ONE_MINUTE_MILLIS;
    private static final long ONE_DAY_MILLIS = 24L * ONE_HOUR_MILLIS;
    static final String MORE_THAN_ONE_DAY_LABEL = ">1 day";

    private final Map<String, Long> mLastOutputActivityTimeMillisByName = new HashMap<>();
    private final Map<String, Long> mLastExplicitCallTimeMillisByName = new HashMap<>();
    private final Map<String, String> mLastExplicitCallReasonByName = new HashMap<>();
    private final Map<String, List<String>> mUnacknowledgedCallReasonsByName = new HashMap<>();
    private final Map<String, List<String>> mAcknowledgedCallReasonsByName = new HashMap<>();
    private final Map<String, Long> mLastSeenTimeMillisByName = new HashMap<>();
    private final Map<String, Long> mLastUserInputTimeMillisByName = new HashMap<>();
    private final Map<String, Long> mStatuslineCallTimeMillisByName = new HashMap<>();
    private final Map<String, Long> mStatuslineOutTimeMillisByName = new HashMap<>();
    private final Map<String, Long> mStatuslineReplyTimeMillisByName = new HashMap<>();

    @NonNull
    private final SessionNewActivityPersistence mPersistence;

    public SessionNewActivityStore() {
        this(new InMemorySessionNewActivityPersistence());
    }

    public SessionNewActivityStore(@NonNull SessionNewActivityPersistence persistence) {
        mPersistence = persistence;
        for (SessionNewActivityState state : persistence.load()) {
            if (state.getLastOutputActivityTimeMillis() != null)
                mLastOutputActivityTimeMillisByName.put(state.getSessionName(), state.getLastOutputActivityTimeMillis());
            if (state.getLastExplicitCallTimeMillis() != null)
                mLastExplicitCallTimeMillisByName.put(state.getSessionName(), state.getLastExplicitCallTimeMillis());
            if (state.getLastExplicitCallReason() != null)
                mLastExplicitCallReasonByName.put(state.getSessionName(), state.getLastExplicitCallReason());
            if (state.getUnacknowledgedCallReasons() != null)
                mUnacknowledgedCallReasonsByName.put(state.getSessionName(),
                    new ArrayList<>(state.getUnacknowledgedCallReasons()));
            if (state.getAcknowledgedCallReasons() != null)
                mAcknowledgedCallReasonsByName.put(state.getSessionName(),
                    new ArrayList<>(state.getAcknowledgedCallReasons()));
            if (state.getLastSeenTimeMillis() != null)
                mLastSeenTimeMillisByName.put(state.getSessionName(), state.getLastSeenTimeMillis());
            if (state.getLastUserInputTimeMillis() != null)
                mLastUserInputTimeMillisByName.put(state.getSessionName(), state.getLastUserInputTimeMillis());
            if (state.getStatuslineCallTimeMillis() != null)
                mStatuslineCallTimeMillisByName.put(state.getSessionName(), state.getStatuslineCallTimeMillis());
            if (state.getStatuslineOutTimeMillis() != null)
                mStatuslineOutTimeMillisByName.put(state.getSessionName(), state.getStatuslineOutTimeMillis());
            if (state.getStatuslineReplyTimeMillis() != null)
                mStatuslineReplyTimeMillisByName.put(state.getSessionName(), state.getStatuslineReplyTimeMillis());
        }
    }

    public void recordOutputActivity(@NonNull String sessionName, long outputActivityTimeMillis) {
        mLastOutputActivityTimeMillisByName.put(sessionName, outputActivityTimeMillis);
        save();
    }

    public void recordExplicitCall(@NonNull String sessionName, long explicitCallTimeMillis) {
        recordExplicitCall(sessionName, explicitCallTimeMillis, "");
    }

    public void recordExplicitCall(@NonNull String sessionName, long explicitCallTimeMillis,
                                   @NonNull String reason) {
        if (!reason.trim().isEmpty() && isAlreadyKnownCall(sessionName, reason)) {
            return;
        }
        mLastExplicitCallTimeMillisByName.put(sessionName, explicitCallTimeMillis);
        mLastExplicitCallReasonByName.put(sessionName, reason);
        if (!reason.trim().isEmpty()) {
            List<String> reasons = mUnacknowledgedCallReasonsByName.get(sessionName);
            if (reasons == null) {
                reasons = new ArrayList<>();
                mUnacknowledgedCallReasonsByName.put(sessionName, reasons);
            }
            reasons.add(reason);
        }
        save();
    }

    private boolean isAlreadyKnownCall(@NonNull String sessionName, @NonNull String reason) {
        return containsCanonically(mUnacknowledgedCallReasonsByName.get(sessionName), reason)
            || containsCanonically(mAcknowledgedCallReasonsByName.get(sessionName), reason);
    }

    private static boolean containsCanonically(@Nullable List<String> reasons, @NonNull String reason) {
        if (reasons == null) {
            return false;
        }
        String canonicalReason = canonicalReasonKey(reason);
        for (String knownReason : reasons) {
            if (knownReason != null && canonicalReasonKey(knownReason).equals(canonicalReason)) {
                return true;
            }
        }
        return false;
    }

    /**
     * The reflow-insensitive identity of a call-to-user reason used for deduplication only (the
     * stored and displayed reason text keeps its original whitespace). The same {@code
     * <call-to-user>} tag re-rendered after a terminal column resize or a scrollback reflow wraps its
     * inner text across different line boundaries, so a re-scan on the load/reload path produces the
     * same reason with interior newlines and spaces shifted. Collapsing every run of whitespace to a
     * single space and trimming yields a stable key, so a reloaded session whose call-to-user was
     * already recorded or acknowledged is recognized as already known and {@code recordExplicitCall}
     * stays a no-op — it does not bump the timestamp or re-arm the red tier.
     */
    @NonNull
    private static String canonicalReasonKey(@NonNull String reason) {
        return reason.replaceAll("\\s+", " ").trim();
    }

    public void recordSeen(@NonNull String sessionName, long seenTimeMillis) {
        mLastSeenTimeMillisByName.put(sessionName, seenTimeMillis);
        save();
    }

    public void recordUserInput(@NonNull String sessionName, long userInputTimeMillis) {
        mLastUserInputTimeMillisByName.put(sessionName, userInputTimeMillis);
        acknowledgeCallReasons(sessionName);
        save();
    }

    public void recordStatuslineTimes(@NonNull String sessionName,
                                      @Nullable Long callTimeMillis,
                                      @Nullable Long outTimeMillis,
                                      @Nullable Long replyTimeMillis) {
        if (callTimeMillis != null) {
            mStatuslineCallTimeMillisByName.put(sessionName, callTimeMillis);
            mLastExplicitCallTimeMillisByName.put(sessionName, callTimeMillis);
        }
        if (outTimeMillis != null) {
            mStatuslineOutTimeMillisByName.put(sessionName, outTimeMillis);
            mLastOutputActivityTimeMillisByName.put(sessionName, outTimeMillis);
        }
        if (replyTimeMillis != null) {
            mStatuslineReplyTimeMillisByName.put(sessionName, replyTimeMillis);
            mLastUserInputTimeMillisByName.put(sessionName, replyTimeMillis);
            boolean replyAnsweredCall = callTimeMillis == null || replyTimeMillis >= callTimeMillis;
            if (replyAnsweredCall) {
                acknowledgeCallReasons(sessionName);
            }
        }
        save();
    }

    private void acknowledgeCallReasons(@NonNull String sessionName) {
        List<String> clearedReasons = mUnacknowledgedCallReasonsByName.remove(sessionName);
        if (clearedReasons == null) {
            return;
        }
        List<String> acknowledged = mAcknowledgedCallReasonsByName.get(sessionName);
        if (acknowledged == null) {
            acknowledged = new ArrayList<>();
            mAcknowledgedCallReasonsByName.put(sessionName, acknowledged);
        }
        for (String reason : clearedReasons) {
            if (!acknowledged.contains(reason)) {
                acknowledged.add(reason);
            }
        }
    }

    public void purgeSession(@NonNull String sessionName) {
        mLastOutputActivityTimeMillisByName.remove(sessionName);
        mLastExplicitCallTimeMillisByName.remove(sessionName);
        mLastExplicitCallReasonByName.remove(sessionName);
        mUnacknowledgedCallReasonsByName.remove(sessionName);
        mAcknowledgedCallReasonsByName.remove(sessionName);
        mLastSeenTimeMillisByName.remove(sessionName);
        mLastUserInputTimeMillisByName.remove(sessionName);
        mStatuslineCallTimeMillisByName.remove(sessionName);
        mStatuslineOutTimeMillisByName.remove(sessionName);
        mStatuslineReplyTimeMillisByName.remove(sessionName);
        save();
    }

    public void pruneToSessionNames(@NonNull Set<String> knownSessionNames) {
        boolean changed = mLastOutputActivityTimeMillisByName.keySet().retainAll(knownSessionNames);
        changed |= mLastExplicitCallTimeMillisByName.keySet().retainAll(knownSessionNames);
        changed |= mLastExplicitCallReasonByName.keySet().retainAll(knownSessionNames);
        changed |= mUnacknowledgedCallReasonsByName.keySet().retainAll(knownSessionNames);
        changed |= mAcknowledgedCallReasonsByName.keySet().retainAll(knownSessionNames);
        changed |= mLastSeenTimeMillisByName.keySet().retainAll(knownSessionNames);
        changed |= mLastUserInputTimeMillisByName.keySet().retainAll(knownSessionNames);
        changed |= mStatuslineCallTimeMillisByName.keySet().retainAll(knownSessionNames);
        changed |= mStatuslineOutTimeMillisByName.keySet().retainAll(knownSessionNames);
        changed |= mStatuslineReplyTimeMillisByName.keySet().retainAll(knownSessionNames);
        if (changed)
            save();
    }

    @Nullable
    public Long getLastOutputActivityTimeMillis(@NonNull String sessionName) {
        return mLastOutputActivityTimeMillisByName.get(sessionName);
    }

    @Nullable
    public Long getLastExplicitCallTimeMillis(@NonNull String sessionName) {
        return mLastExplicitCallTimeMillisByName.get(sessionName);
    }

    @NonNull
    public String getLastExplicitCallReason(@NonNull String sessionName) {
        String reason = mLastExplicitCallReasonByName.get(sessionName);
        return reason == null ? "" : reason;
    }

    @NonNull
    public List<String> getUnacknowledgedCallReasons(@NonNull String sessionName) {
        List<String> reasons = mUnacknowledgedCallReasonsByName.get(sessionName);
        if (reasons == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<>(reasons));
    }

    @Nullable
    public Long getLastSeenTimeMillis(@NonNull String sessionName) {
        return mLastSeenTimeMillisByName.get(sessionName);
    }

    @Nullable
    public Long getLastUserInputTimeMillis(@NonNull String sessionName) {
        return mLastUserInputTimeMillisByName.get(sessionName);
    }

    @Nullable
    public Long getStatuslineCallTimeMillis(@NonNull String sessionName) {
        return mStatuslineCallTimeMillisByName.get(sessionName);
    }

    @Nullable
    public Long getStatuslineOutTimeMillis(@NonNull String sessionName) {
        return mStatuslineOutTimeMillisByName.get(sessionName);
    }

    @Nullable
    public Long getStatuslineReplyTimeMillis(@NonNull String sessionName) {
        return mStatuslineReplyTimeMillisByName.get(sessionName);
    }

    @NonNull
    public SessionNewActivityTier tierFor(@NonNull String sessionName) {
        return SessionNewActivityTier.resolve(
            getLastOutputActivityTimeMillis(sessionName),
            getLastExplicitCallTimeMillis(sessionName),
            getLastUserInputTimeMillis(sessionName),
            getLastSeenTimeMillis(sessionName));
    }

    @NonNull
    public SessionNewActivityTier tierFor(@NonNull String sessionName, long nowMillis) {
        return SessionNewActivityTier.resolve(
            getLastOutputActivityTimeMillis(sessionName),
            getLastExplicitCallTimeMillis(sessionName),
            getLastUserInputTimeMillis(sessionName),
            getLastSeenTimeMillis(sessionName),
            nowMillis);
    }

    @NonNull
    public SessionNewActivityTier globalActiveTier(@NonNull Set<String> sessionNames) {
        SessionNewActivityTier activeTier = SessionNewActivityTier.NONE;
        for (String sessionName : sessionNames) {
            SessionNewActivityTier tier = tierFor(sessionName);
            if (tier == SessionNewActivityTier.RED) {
                return SessionNewActivityTier.RED;
            }
            if (tier == SessionNewActivityTier.YELLOW) {
                activeTier = SessionNewActivityTier.YELLOW;
            }
        }
        return activeTier;
    }

    public boolean hasUnseenActivity(@NonNull String sessionName) {
        return tierFor(sessionName) != SessionNewActivityTier.NONE;
    }

    public boolean hasPendingExplicitCall(@NonNull String sessionName) {
        return tierFor(sessionName) == SessionNewActivityTier.RED;
    }

    @Nullable
    Long pendingSignalTimeMillis(@NonNull String sessionName) {
        switch (tierFor(sessionName)) {
            case RED:
                return getLastExplicitCallTimeMillis(sessionName);
            case YELLOW:
                return getLastOutputActivityTimeMillis(sessionName);
            case NONE:
            default:
                return null;
        }
    }

    private void save() {
        Set<String> sessionNames = new HashSet<>(mLastOutputActivityTimeMillisByName.keySet());
        sessionNames.addAll(mLastExplicitCallTimeMillisByName.keySet());
        sessionNames.addAll(mLastExplicitCallReasonByName.keySet());
        sessionNames.addAll(mUnacknowledgedCallReasonsByName.keySet());
        sessionNames.addAll(mAcknowledgedCallReasonsByName.keySet());
        sessionNames.addAll(mLastSeenTimeMillisByName.keySet());
        sessionNames.addAll(mLastUserInputTimeMillisByName.keySet());
        sessionNames.addAll(mStatuslineCallTimeMillisByName.keySet());
        sessionNames.addAll(mStatuslineOutTimeMillisByName.keySet());
        sessionNames.addAll(mStatuslineReplyTimeMillisByName.keySet());
        List<SessionNewActivityState> states = new ArrayList<>();
        for (String sessionName : sessionNames) {
            List<String> reasons = mUnacknowledgedCallReasonsByName.get(sessionName);
            List<String> acknowledgedReasons = mAcknowledgedCallReasonsByName.get(sessionName);
            states.add(new SessionNewActivityState(sessionName,
                mLastOutputActivityTimeMillisByName.get(sessionName),
                mLastExplicitCallTimeMillisByName.get(sessionName),
                mLastExplicitCallReasonByName.get(sessionName),
                mLastSeenTimeMillisByName.get(sessionName),
                mLastUserInputTimeMillisByName.get(sessionName),
                reasons == null ? null : new ArrayList<>(reasons),
                acknowledgedReasons == null ? null : new ArrayList<>(acknowledgedReasons),
                mStatuslineCallTimeMillisByName.get(sessionName),
                mStatuslineOutTimeMillisByName.get(sessionName),
                mStatuslineReplyTimeMillisByName.get(sessionName)));
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

    public static String formatRelativeAge(long timeMillis, long nowMillis) {
        if (timeMillis > nowMillis) {
            return MORE_THAN_ONE_DAY_LABEL;
        }
        long elapsedMillis = nowMillis - timeMillis;
        if (elapsedMillis >= ONE_DAY_MILLIS) {
            return MORE_THAN_ONE_DAY_LABEL;
        }
        if (elapsedMillis < ONE_MINUTE_MILLIS) {
            return (elapsedMillis / ONE_SECOND_MILLIS) + "s";
        }
        if (elapsedMillis < ONE_HOUR_MILLIS) {
            return (elapsedMillis / ONE_MINUTE_MILLIS) + "m";
        }
        return (elapsedMillis / ONE_HOUR_MILLIS) + "h";
    }

    @Nullable
    public String lastOutputActivityAgeLabel(@NonNull String sessionName, long nowMillis) {
        Long t = getLastOutputActivityTimeMillis(sessionName);
        return t == null ? null : formatRelativeTime(nowMillis - t);
    }

    @Nullable
    public String lastUserInputAgeLabel(@NonNull String sessionName, long nowMillis) {
        Long t = getLastUserInputTimeMillis(sessionName);
        return t == null ? null : formatRelativeTime(nowMillis - t);
    }
}
