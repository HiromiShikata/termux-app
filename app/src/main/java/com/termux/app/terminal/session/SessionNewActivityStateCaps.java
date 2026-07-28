package com.termux.app.terminal.session;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.termux.app.terminal.SessionNewActivityState;

import java.util.ArrayList;
import java.util.List;

public final class SessionNewActivityStateCaps {

    public static final int MAX_REASON_LENGTH = 300;
    public static final int MAX_REASONS_PER_SESSION = 10;

    /**
     * The call trigger values are the deduplication key set, not display content, and must be able to
     * hold every call the replaced contract could still recognize. That contract kept two independent
     * lists of {@link #MAX_REASONS_PER_SESSION} reasons each — acknowledged and unacknowledged — and
     * treated a call as already known when its reason appeared in either, so its key set held twice
     * {@link #MAX_REASONS_PER_SESSION}. Capping the single replacement list at
     * {@link #MAX_REASONS_PER_SESSION} would halve that capacity and let an already-answered call fall
     * out of the key set and re-arm the red indicator on the next transcript scan.
     */
    public static final int MAX_CALL_TRIGGER_VALUES_PER_SESSION = 2 * MAX_REASONS_PER_SESSION;

    private SessionNewActivityStateCaps() {
    }

    @Nullable
    public static String capReason(@Nullable String reason) {
        if (reason == null) {
            return null;
        }
        if (reason.length() <= MAX_REASON_LENGTH) {
            return reason;
        }
        return reason.substring(0, MAX_REASON_LENGTH);
    }

    @Nullable
    public static List<String> capReasons(@Nullable List<String> reasons) {
        return capTrailing(reasons, MAX_REASONS_PER_SESSION);
    }

    @Nullable
    public static List<String> capCallTriggerValues(@Nullable List<String> callTriggerValues) {
        return capTrailing(callTriggerValues, MAX_CALL_TRIGGER_VALUES_PER_SESSION);
    }

    @Nullable
    private static List<String> capTrailing(@Nullable List<String> values, int maxSize) {
        if (values == null) {
            return null;
        }
        int size = values.size();
        int start = Math.max(0, size - maxSize);
        List<String> capped = new ArrayList<>(Math.min(size, maxSize));
        for (int index = start; index < size; index++) {
            capped.add(capReason(values.get(index)));
        }
        return capped;
    }

    @NonNull
    public static List<SessionNewActivityState> capStates(@NonNull List<SessionNewActivityState> states) {
        List<SessionNewActivityState> capped = new ArrayList<>(states.size());
        for (SessionNewActivityState state : states) {
            capped.add(capState(state));
        }
        return capped;
    }

    @NonNull
    public static SessionNewActivityState capState(@NonNull SessionNewActivityState state) {
        return new SessionNewActivityState(
            state.getSessionName(),
            state.getLastOutputActivityTimeMillis(),
            state.getLastExplicitCallTimeMillis(),
            capReason(state.getLastExplicitCallReason()),
            state.getLastSeenTimeMillis(),
            state.getLastUserInputTimeMillis(),
            capReasons(state.getUnacknowledgedCallReasons()),
            capCallTriggerValues(state.getCallTriggerValues()),
            state.getStatuslineCallTimeMillis(),
            state.getStatuslineOutTimeMillis(),
            state.getStatuslineReplyTimeMillis(),
            state.getSubagentCount());
    }
}
