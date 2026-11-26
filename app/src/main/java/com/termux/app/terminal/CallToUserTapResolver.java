package com.termux.app.terminal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Set;

public final class CallToUserTapResolver {
    public enum Outcome {
        SELECT_CALLING_SESSION,
        CALLING_SESSION_ENDED,
        NOTHING_TO_SELECT
    }

    private CallToUserTapResolver() {
    }

    @NonNull
    public static Outcome resolve(@Nullable String requestedSessionName,
                                  @NonNull Set<String> liveSessionNames) {
        if (requestedSessionName == null || requestedSessionName.isEmpty()) {
            return Outcome.NOTHING_TO_SELECT;
        }
        return liveSessionNames.contains(requestedSessionName)
            ? Outcome.SELECT_CALLING_SESSION
            : Outcome.CALLING_SESSION_ENDED;
    }
}
