package com.termux.app.terminal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class SessionNewActivityIndicator {

    private final boolean visible;
    private final String label;

    private SessionNewActivityIndicator(boolean visible, @NonNull String label) {
        this.visible = visible;
        this.label = label;
    }

    public boolean isVisible() {
        return visible;
    }

    @NonNull
    public String getLabel() {
        return label;
    }

    @NonNull
    public static SessionNewActivityIndicator labelFor(@Nullable Long arrivalTimeMillis, long nowMillis) {
        if (arrivalTimeMillis == null) {
            return new SessionNewActivityIndicator(false, "");
        }
        return new SessionNewActivityIndicator(true,
            SessionNewActivityStore.formatRelativeTime(nowMillis - arrivalTimeMillis));
    }
}
