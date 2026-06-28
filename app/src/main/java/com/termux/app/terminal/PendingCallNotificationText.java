package com.termux.app.terminal;

import androidx.annotation.NonNull;

public final class PendingCallNotificationText {

    private PendingCallNotificationText() {
    }

    @NonNull
    public static String fractionSuffix(int pendingCallSessionCount, int totalSessionCount) {
        if (totalSessionCount <= 0) {
            return "";
        }
        int clampedPending = Math.max(0, Math.min(pendingCallSessionCount, totalSessionCount));
        return clampedPending + "/" + totalSessionCount + " calls";
    }
}
