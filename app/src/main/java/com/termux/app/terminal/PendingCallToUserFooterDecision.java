package com.termux.app.terminal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

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

    @NonNull
    public static PendingCallToUserFooterDecision resolveAll(@NonNull SessionNewActivityTier tier,
                                                             @Nullable List<String> reasons) {
        if (tier != SessionNewActivityTier.RED || reasons == null) {
            return new PendingCallToUserFooterDecision(false, "");
        }
        StringBuilder reportText = new StringBuilder();
        for (String reason : reasons) {
            String trimmedReason = reason == null ? "" : reason.trim();
            if (trimmedReason.isEmpty()) {
                continue;
            }
            if (reportText.length() > 0) {
                reportText.append('\n');
            }
            reportText.append(trimmedReason);
        }
        if (reportText.length() == 0) {
            return new PendingCallToUserFooterDecision(false, "");
        }
        return new PendingCallToUserFooterDecision(true, reportText.toString());
    }
}
