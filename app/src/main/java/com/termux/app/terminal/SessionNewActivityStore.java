package com.termux.app.terminal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.termux.app.terminal.session.SessionNewActivityStateCaps;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SessionNewActivityStore {

    static final int MAX_REASONS_PER_SESSION = SessionNewActivityStateCaps.MAX_REASONS_PER_SESSION;

    private static final long ONE_SECOND_MILLIS = 1000L;
    private static final long ONE_MINUTE_MILLIS = 60L * ONE_SECOND_MILLIS;
    private static final long ONE_HOUR_MILLIS = 60L * ONE_MINUTE_MILLIS;
    private static final long ONE_DAY_MILLIS = 24L * ONE_HOUR_MILLIS;
    static final String MORE_THAN_ONE_DAY_LABEL = ">1d";

    private final Map<String, Long> mLastOutputActivityTimeMillisByName = new HashMap<>();
    private final Map<String, Long> mLastExplicitCallTimeMillisByName = new HashMap<>();
    private final Map<String, String> mLastExplicitCallReasonByName = new HashMap<>();
    private final Map<String, List<String>> mUnacknowledgedCallReasonsByName = new HashMap<>();
    private final Map<String, Long> mUnacknowledgedCallReasonsRecordedTimeMillisByName = new HashMap<>();
    private final Map<String, List<String>> mAcknowledgedCallReasonsByName = new HashMap<>();
    private final Map<String, Long> mLastSeenTimeMillisByName = new HashMap<>();
    private final Map<String, Long> mLastUserInputTimeMillisByName = new HashMap<>();
    private final Map<String, Long> mStatuslineCallTimeMillisByName = new HashMap<>();
    private final Map<String, Long> mStatuslineOutTimeMillisByName = new HashMap<>();
    private final Map<String, Long> mStatuslineReplyTimeMillisByName = new HashMap<>();
    private final Map<String, Integer> mSubagentCountByName = new HashMap<>();

    @NonNull
    private final SessionNewActivityPersistence mPersistence;

    @Nullable
    private OnChangeListener mOnChangeListener;

    public interface OnChangeListener {
        void onSessionNewActivityStoreChanged(@NonNull SessionNewActivityStore store);
    }

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
            if (state.getUnacknowledgedCallReasons() != null) {
                mUnacknowledgedCallReasonsByName.put(state.getSessionName(),
                    new ArrayList<>(state.getUnacknowledgedCallReasons()));
                if (!state.getUnacknowledgedCallReasons().isEmpty()
                    && state.getLastExplicitCallTimeMillis() != null)
                    mUnacknowledgedCallReasonsRecordedTimeMillisByName.put(state.getSessionName(),
                        state.getLastExplicitCallTimeMillis());
            }
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
            if (state.getSubagentCount() != null)
                mSubagentCountByName.put(state.getSessionName(), state.getSubagentCount());
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
        String cappedReason = SessionNewActivityStateCaps.capReason(reason);
        if (!cappedReason.trim().isEmpty() && isAlreadyKnownCall(sessionName, cappedReason)) {
            return;
        }
        mLastExplicitCallTimeMillisByName.put(sessionName, explicitCallTimeMillis);
        mLastExplicitCallReasonByName.put(sessionName, cappedReason);
        if (!cappedReason.trim().isEmpty()) {
            List<String> reasons = mUnacknowledgedCallReasonsByName.get(sessionName);
            if (reasons == null) {
                reasons = new ArrayList<>();
                mUnacknowledgedCallReasonsByName.put(sessionName, reasons);
            }
            reasons.add(cappedReason);
            capTrailing(reasons);
            mUnacknowledgedCallReasonsRecordedTimeMillisByName.put(sessionName, explicitCallTimeMillis);
        }
        save();
    }

    private static void capTrailing(@NonNull List<String> reasons) {
        while (reasons.size() > MAX_REASONS_PER_SESSION) {
            reasons.remove(0);
        }
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
        advanceLastUserInputTime(sessionName, userInputTimeMillis);
        acknowledgeCallReasons(sessionName);
        save();
    }

    private void advanceLastUserInputTime(@NonNull String sessionName, long userInputTimeMillis) {
        Long stored = mLastUserInputTimeMillisByName.get(sessionName);
        if (stored != null && stored >= userInputTimeMillis) {
            return;
        }
        mLastUserInputTimeMillisByName.put(sessionName, userInputTimeMillis);
    }

    private void advanceStatuslineReplyTime(@NonNull String sessionName, long replyTimeMillis) {
        Long stored = mStatuslineReplyTimeMillisByName.get(sessionName);
        if (stored != null && stored >= replyTimeMillis) {
            return;
        }
        mStatuslineReplyTimeMillisByName.put(sessionName, replyTimeMillis);
    }

    public void recordStatuslineTimes(@NonNull String sessionName,
                                      @Nullable Long callTimeMillis,
                                      @Nullable Long outTimeMillis,
                                      @Nullable Long replyTimeMillis) {
        recordStatuslineTimes(sessionName, callTimeMillis, outTimeMillis, replyTimeMillis, 0);
    }

    public void recordStatuslineTimes(@NonNull String sessionName,
                                      @Nullable Long callTimeMillis,
                                      @Nullable Long outTimeMillis,
                                      @Nullable Long replyTimeMillis,
                                      int subagentCount) {
        if (statuslineTimesUnchanged(sessionName, callTimeMillis, outTimeMillis, replyTimeMillis,
            subagentCount)) {
            return;
        }
        mSubagentCountByName.put(sessionName, subagentCount);
        if (callTimeMillis != null) {
            mStatuslineCallTimeMillisByName.put(sessionName, callTimeMillis);
            mLastExplicitCallTimeMillisByName.put(sessionName, callTimeMillis);
        }
        if (outTimeMillis != null) {
            mStatuslineOutTimeMillisByName.put(sessionName, outTimeMillis);
            mLastOutputActivityTimeMillisByName.put(sessionName, outTimeMillis);
        }
        if (replyTimeMillis != null) {
            advanceStatuslineReplyTime(sessionName, replyTimeMillis);
            if (statuslineReplyAcknowledgesPendingReasons(sessionName, replyTimeMillis)) {
                acknowledgeCallReasons(sessionName);
            }
        }
        save();
    }

    private boolean statuslineTimesUnchanged(@NonNull String sessionName,
                                             @Nullable Long callTimeMillis,
                                             @Nullable Long outTimeMillis,
                                             @Nullable Long replyTimeMillis,
                                             int subagentCount) {
        Integer storedSubagentCount = mSubagentCountByName.get(sessionName);
        if (storedSubagentCount == null || storedSubagentCount != subagentCount) {
            return false;
        }
        if (!sameStoredStatuslineValue(mStatuslineCallTimeMillisByName.get(sessionName), callTimeMillis)) {
            return false;
        }
        if (!sameStoredStatuslineValue(mStatuslineOutTimeMillisByName.get(sessionName), outTimeMillis)) {
            return false;
        }
        if (!sameStoredStatuslineValue(mStatuslineReplyTimeMillisByName.get(sessionName), replyTimeMillis)) {
            return false;
        }
        if (replyTimeMillis != null
            && statuslineReplyAcknowledgesPendingReasons(sessionName, replyTimeMillis)
            && hasUnacknowledgedCallReasons(sessionName)) {
            return false;
        }
        return true;
    }

    private static boolean sameStoredStatuslineValue(@Nullable Long storedValue,
                                                     @Nullable Long incomingValue) {
        if (incomingValue == null) {
            return true;
        }
        return incomingValue.equals(storedValue);
    }

    private boolean hasUnacknowledgedCallReasons(@NonNull String sessionName) {
        List<String> reasons = mUnacknowledgedCallReasonsByName.get(sessionName);
        return reasons != null && !reasons.isEmpty();
    }

    private boolean statuslineReplyAcknowledgesPendingReasons(@NonNull String sessionName,
                                                              long replyTimeMillis) {
        Long callTimeMillis = callTimeMillisForReplyComparison(sessionName);
        if (callTimeMillis == null) {
            return true;
        }
        return replyTimeMillis >= callTimeMillis;
    }

    @Nullable
    private Long callTimeMillisForReplyComparison(@NonNull String sessionName) {
        Long statuslineCallTimeMillis = mStatuslineCallTimeMillisByName.get(sessionName);
        if (statuslineCallTimeMillis != null) {
            return statuslineCallTimeMillis;
        }
        return mLastExplicitCallTimeMillisByName.get(sessionName);
    }

    private void acknowledgeCallReasons(@NonNull String sessionName) {
        mUnacknowledgedCallReasonsRecordedTimeMillisByName.remove(sessionName);
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
        capTrailing(acknowledged);
    }

    public void purgeSession(@NonNull String sessionName) {
        mLastOutputActivityTimeMillisByName.remove(sessionName);
        mLastExplicitCallTimeMillisByName.remove(sessionName);
        mLastExplicitCallReasonByName.remove(sessionName);
        mUnacknowledgedCallReasonsByName.remove(sessionName);
        mUnacknowledgedCallReasonsRecordedTimeMillisByName.remove(sessionName);
        mAcknowledgedCallReasonsByName.remove(sessionName);
        mLastSeenTimeMillisByName.remove(sessionName);
        mLastUserInputTimeMillisByName.remove(sessionName);
        mStatuslineCallTimeMillisByName.remove(sessionName);
        mStatuslineOutTimeMillisByName.remove(sessionName);
        mStatuslineReplyTimeMillisByName.remove(sessionName);
        mSubagentCountByName.remove(sessionName);
        save();
    }

    public void pruneToSessionNames(@NonNull Set<String> knownSessionNames) {
        boolean changed = mLastOutputActivityTimeMillisByName.keySet().retainAll(knownSessionNames);
        changed |= mLastExplicitCallTimeMillisByName.keySet().retainAll(knownSessionNames);
        changed |= mLastExplicitCallReasonByName.keySet().retainAll(knownSessionNames);
        changed |= mUnacknowledgedCallReasonsByName.keySet().retainAll(knownSessionNames);
        changed |= mUnacknowledgedCallReasonsRecordedTimeMillisByName.keySet().retainAll(knownSessionNames);
        changed |= mAcknowledgedCallReasonsByName.keySet().retainAll(knownSessionNames);
        changed |= mLastSeenTimeMillisByName.keySet().retainAll(knownSessionNames);
        changed |= mLastUserInputTimeMillisByName.keySet().retainAll(knownSessionNames);
        changed |= mStatuslineCallTimeMillisByName.keySet().retainAll(knownSessionNames);
        changed |= mStatuslineOutTimeMillisByName.keySet().retainAll(knownSessionNames);
        changed |= mStatuslineReplyTimeMillisByName.keySet().retainAll(knownSessionNames);
        changed |= mSubagentCountByName.keySet().retainAll(knownSessionNames);
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

    /**
     * The call-to-user reason text to display in the current-session scene for {@code sessionName},
     * or null when nothing should be shown. It is the latest unacknowledged reason, but only when
     * that reason belongs to the call-to-user cycle that currently arms the RED tier. The RED tier
     * is armed by two independent signals — the unacknowledged-reasons list ({@link
     * #pendingCallToUserTimeMillis}) and the statusline {@code call: > reply:} relation ({@link
     * #statuslineCallPendingTimeMillis}) — and these can refer to different cycles: after a new
     * statusline {@code call:} token arms RED, the throttled transcript {@code <call-to-user>} scan
     * has not yet recorded the new cycle's reason, so the unacknowledged list still ends in a reason
     * recorded for a previous call. Showing that earlier reason against the current call is the stale
     * scene the owner misreads as a different session's content. The reason recorded time ({@link
     * #mUnacknowledgedCallReasonsRecordedTimeMillisByName}) is compared against the current
     * statusline call token: a stored reason older than the current call is from a prior cycle and is
     * suppressed (the scene shows nothing) until the scan records the current cycle's reason. When no
     * statusline call token arms the tier (a non-Claude session armed only by the tag scan), the
     * reason itself defines the pending cycle and is shown.
     */
    @Nullable
    public String currentPendingCallToUserReason(@NonNull String sessionName) {
        List<String> reasons = mUnacknowledgedCallReasonsByName.get(sessionName);
        if (reasons == null || reasons.isEmpty()) {
            return null;
        }
        Long statuslineCallPendingTimeMillis = statuslineCallPendingTimeMillis(sessionName);
        if (statuslineCallPendingTimeMillis != null) {
            Long reasonRecordedTimeMillis =
                mUnacknowledgedCallReasonsRecordedTimeMillisByName.get(sessionName);
            if (reasonRecordedTimeMillis == null
                || reasonRecordedTimeMillis < statuslineCallPendingTimeMillis) {
                return null;
            }
        }
        return reasons.get(reasons.size() - 1);
    }

    public int pendingCallToUserSessionCount() {
        int count = 0;
        for (List<String> reasons : mUnacknowledgedCallReasonsByName.values()) {
            if (reasons != null && !reasons.isEmpty()) {
                count++;
            }
        }
        return count;
    }

    public void setOnChangeListener(@Nullable OnChangeListener listener) {
        mOnChangeListener = listener;
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

    public int getSubagentCount(@NonNull String sessionName) {
        Integer count = mSubagentCountByName.get(sessionName);
        return count == null ? 0 : count;
    }

    @NonNull
    public SessionNewActivityTier tierFor(@NonNull String sessionName) {
        return SessionNewActivityTier.resolve(
            getLastOutputActivityTimeMillis(sessionName),
            pendingCallToUserTimeMillis(sessionName),
            statuslineCallPendingTimeMillis(sessionName),
            getLastUserInputTimeMillis(sessionName),
            getLastSeenTimeMillis(sessionName));
    }

    @NonNull
    public SessionNewActivityTier tierFor(@NonNull String sessionName, long nowMillis) {
        return SessionNewActivityTier.resolve(
            outActivityTimeMillisForDotTier(sessionName),
            pendingCallToUserTimeMillis(sessionName),
            statuslineCallPendingTimeMillis(sessionName),
            getLastUserInputTimeMillis(sessionName),
            getLastSeenTimeMillis(sessionName),
            effectiveReplyTimeMillis(sessionName),
            nowMillis);
    }

    /**
     * The single source of truth for both the RED call-to-user tier and the call-to-user scene
     * content. A call-to-user is pending exactly when its detected reason has not yet been
     * acknowledged, so the pending-call time the tier ages off is the time the still-unacknowledged
     * reason was recorded ({@link #mUnacknowledgedCallReasonsRecordedTimeMillisByName}) and is
     * present only while {@link #getUnacknowledgedCallReasons} is non-empty. Keying the tier off this
     * derived time instead of the raw {@link #getLastExplicitCallTimeMillis} makes {@code tierFor}
     * and {@code getUnacknowledgedCallReasons} unable to diverge: the tier cannot be RED while the
     * scene is empty, and the scene cannot show while the tier is not RED. The owner's app-side input
     * ({@link #recordUserInput}) clears the unacknowledged reasons, which simultaneously dismisses the
     * scene and drops the tier out of RED. The raw last-explicit-call time and the statusline call
     * token remain available as display values but no longer arm the tier on their own, so a
     * reasonless explicit call or a re-rendered statusline call token cannot leave the indicator RED
     * with no scene content.
     */
    @Nullable
    Long pendingCallToUserTimeMillis(@NonNull String sessionName) {
        List<String> reasons = mUnacknowledgedCallReasonsByName.get(sessionName);
        if (reasons == null || reasons.isEmpty()) {
            return null;
        }
        return mUnacknowledgedCallReasonsRecordedTimeMillisByName.get(sessionName);
    }

    /**
     * The reliable statusline-derived pending-call signal that arms the RED tier even when the
     * throttled transcript {@code <call-to-user>} tag scan missed the tag. A Claude Code session
     * renders a statusline whose {@code call:} and {@code reply:} clock tokens are reparsed on every
     * tick ({@link #recordStatuslineTimes} keeps {@link #mStatuslineCallTimeMillisByName} and {@link
     * #mStatuslineReplyTimeMillisByName} fresh), so the call-newer-than-reply relation is observed
     * reliably, unlike the one-shot tag whose last firing before idle can land inside the scan
     * throttle window and never be re-scanned. The session is pending exactly when a {@code call:}
     * token exists and no {@code reply:} has caught up to it ({@code reply == null || call > reply}),
     * in which case the call token time is returned; otherwise null. A session with no statusline at
     * all (a non-Claude session) has a null {@code call:} token and is never armed through this
     * signal, leaving the tag-scan path as its sole, unchanged source of the RED tier.
     */
    @Nullable
    public Long statuslineCallPendingTimeMillis(@NonNull String sessionName) {
        Long statuslineCallTimeMillis = mStatuslineCallTimeMillisByName.get(sessionName);
        if (statuslineCallTimeMillis == null) {
            return null;
        }
        Long effectiveReplyTimeMillis = effectiveReplyTimeMillis(sessionName);
        if (effectiveReplyTimeMillis == null || statuslineCallTimeMillis > effectiveReplyTimeMillis) {
            return statuslineCallTimeMillis;
        }
        return null;
    }

    /**
     * The reply timestamp that both the RED call-to-user pending check and the displayed {@code
     * reply:} value use. It is the later of the app-captured owner input ({@link
     * #getLastUserInputTimeMillis}, updated instantly by {@link #recordUserInput}) and the statusline
     * {@code reply:} token ({@link #getStatuslineReplyTimeMillis}, which lags several minutes behind
     * the owner's actual reply in practice). Preferring the more recent of the two means that the
     * instant the owner types input, the effective reply catches up to or passes the call, so the RED
     * dot clears immediately and the displayed {@code reply:} reflects the input without waiting for
     * the laggy statusline token. The app-held input time is in-memory only; after a restart that
     * loses it the statusline reply token is the sole remaining source and is used as the fallback.
     * Returns null only when neither source has a value.
     */
    @Nullable
    public Long effectiveReplyTimeMillis(@NonNull String sessionName) {
        Long lastUserInputTimeMillis = mLastUserInputTimeMillisByName.get(sessionName);
        Long statuslineReplyTimeMillis = mStatuslineReplyTimeMillisByName.get(sessionName);
        if (lastUserInputTimeMillis == null) {
            return statuslineReplyTimeMillis;
        }
        if (statuslineReplyTimeMillis == null) {
            return lastUserInputTimeMillis;
        }
        return Math.max(lastUserInputTimeMillis, statuslineReplyTimeMillis);
    }

    /**
     * Whether the expensive transcript {@code <call-to-user>} reason/scene scan should run for this
     * session. It runs only when the session has a pending call on its reliable statusline signal
     * ({@link #statuslineCallPendingTimeMillis} is non-null, i.e. {@code call: > reply:}), or when the
     * session has no statusline at all (a non-Claude session with neither a {@code call:} nor a {@code
     * reply:} token, which has no statusline-pending signal and so keeps the tag scan as its sole
     * call-to-user source). A session whose statusline shows a reply caught up to the call is not
     * pending and skips the scan, removing the per-output tag-scan cost for the common idle case.
     */
    public boolean shouldScanCallToUserTag(@NonNull String sessionName) {
        if (statuslineCallPendingTimeMillis(sessionName) != null) {
            return true;
        }
        return !hasStatusline(sessionName);
    }

    private boolean hasStatusline(@NonNull String sessionName) {
        return mStatuslineCallTimeMillisByName.get(sessionName) != null
            || mStatuslineReplyTimeMillisByName.get(sessionName) != null;
    }

    /**
     * The output timestamp that drives the colored activity dot's YELLOW/GRAY age tier. The dot the
     * owner sees sits next to the session's displayed {@code out:} value, which is sourced from the
     * statusline {@code out} time ({@link #getStatuslineOutTimeMillis}). The dot tier MUST therefore
     * age off that same timestamp, not off {@link #getLastOutputActivityTimeMillis}: raw PTY output
     * (terminal noise, background process output) keeps the last-output-activity time fresh through
     * {@link #recordOutputActivity} independently of the statusline out, so keying the tier off the
     * raw output time leaves the dot YELLOW for hours after the displayed out went stale (the
     * reported "out is 9h but the dot is still yellow" defect). When a session has a statusline out
     * time it is authoritative; sessions that only ever emitted raw output and never a statusline
     * out fall back to the raw last-output-activity time so their dot still ages normally.
     */
    @Nullable
    Long outActivityTimeMillisForDotTier(@NonNull String sessionName) {
        Long statuslineOutTimeMillis = getStatuslineOutTimeMillis(sessionName);
        if (statuslineOutTimeMillis != null) {
            return statuslineOutTimeMillis;
        }
        return getLastOutputActivityTimeMillis(sessionName);
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
                return pendingCallToUserTimeMillis(sessionName);
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
        sessionNames.addAll(mSubagentCountByName.keySet());
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
                mStatuslineReplyTimeMillisByName.get(sessionName),
                mSubagentCountByName.get(sessionName)));
        }
        mPersistence.save(states);
        if (mOnChangeListener != null) {
            mOnChangeListener.onSessionNewActivityStoreChanged(this);
        }
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
