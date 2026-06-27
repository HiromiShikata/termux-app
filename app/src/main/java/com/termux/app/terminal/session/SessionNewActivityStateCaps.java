package com.termux.app.terminal.session;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.termux.app.terminal.SessionNewActivityState;

import java.util.ArrayList;
import java.util.List;

public final class SessionNewActivityStateCaps {

    public static final int MAX_REASON_LENGTH = 300;
    public static final int MAX_REASONS_PER_SESSION = 10;

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
        if (reasons == null) {
            return null;
        }
        int size = reasons.size();
        int start = Math.max(0, size - MAX_REASONS_PER_SESSION);
        List<String> capped = new ArrayList<>(Math.min(size, MAX_REASONS_PER_SESSION));
        for (int index = start; index < size; index++) {
            capped.add(capReason(reasons.get(index)));
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
            capReasons(state.getAcknowledgedCallReasons()),
            state.getStatuslineCallTimeMillis(),
            state.getStatuslineOutTimeMillis(),
            state.getStatuslineReplyTimeMillis(),
            state.getSubagentCount());
    }
}
