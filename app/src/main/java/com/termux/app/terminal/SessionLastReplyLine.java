package com.termux.app.terminal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class SessionLastReplyLine {

    private final boolean mVisible;

    @NonNull
    private final String mAgeLabel;

    private SessionLastReplyLine(boolean visible, @NonNull String ageLabel) {
        mVisible = visible;
        mAgeLabel = ageLabel;
    }

    @NonNull
    public static SessionLastReplyLine of(@Nullable String ageLabel) {
        String normalized = ageLabel == null ? "" : ageLabel.trim();
        if (normalized.isEmpty()) {
            return new SessionLastReplyLine(false, "");
        }
        return new SessionLastReplyLine(true, normalized);
    }

    public boolean isVisible() {
        return mVisible;
    }

    @NonNull
    public String getAgeLabel() {
        return mAgeLabel;
    }
}
