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

    static boolean isBellUnseen(@Nullable Long lastBellTimeMillis, @Nullable Long lastSeenTimeMillis) {
        if (lastBellTimeMillis == null) {
            return false;
        }
        if (lastSeenTimeMillis == null) {
            return true;
        }
        return lastBellTimeMillis > lastSeenTimeMillis;
    }

    @NonNull
    public static SessionNewActivityIndicator labelFor(@Nullable Long lastBellTimeMillis,
                                                       @Nullable Long lastSeenTimeMillis, long nowMillis) {
        if (!isBellUnseen(lastBellTimeMillis, lastSeenTimeMillis)) {
            return new SessionNewActivityIndicator(false, "");
        }
        return new SessionNewActivityIndicator(true,
            SessionNewActivityStore.formatRelativeTime(nowMillis - lastBellTimeMillis));
    }
}
