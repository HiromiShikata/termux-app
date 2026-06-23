package com.termux.app.terminal;

import androidx.annotation.Nullable;

public final class UrgentCallSeenSuppression {

    private UrgentCallSeenSuppression() {
    }

    public static boolean shouldSuppressSeen(@Nullable String suppressedSessionName,
                                             @Nullable String activeSessionName) {
        return suppressedSessionName != null && suppressedSessionName.equals(activeSessionName);
    }

    public static boolean clearsSuppressionOnSwitch(@Nullable String suppressedSessionName,
                                                    @Nullable String targetSessionName) {
        return suppressedSessionName != null && !suppressedSessionName.equals(targetSessionName);
    }
}
