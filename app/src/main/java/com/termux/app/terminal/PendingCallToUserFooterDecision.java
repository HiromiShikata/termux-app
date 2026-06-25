package com.termux.app.terminal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class PendingCallToUserFooterDecision {

    private final boolean mVisible;

    @NonNull
    private final String mReportText;

    private PendingCallToUserFooterDecision(boolean visible, @NonNull String reportText) {
        mVisible = visible;
        mReportText = reportText;
    }

    public boolean isVisible() {
        return mVisible;
    }

    @NonNull
    public String getReportText() {
        return mReportText;
    }

    @NonNull
    public static PendingCallToUserFooterDecision resolve(@NonNull SessionNewActivityTier tier,
                                                          @Nullable String reason) {
        if (tier != SessionNewActivityTier.RED) {
            return new PendingCallToUserFooterDecision(false, "");
        }
        String trimmedReason = reason == null ? "" : reason.trim();
        if (trimmedReason.isEmpty()) {
            return new PendingCallToUserFooterDecision(false, "");
        }
        return new PendingCallToUserFooterDecision(true, trimmedReason);
    }
}
