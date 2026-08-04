package com.termux.shared.termux.settings.preferences;

import androidx.annotation.Nullable;

public final class UserRemovedSessionHideWindow {

    public static final long HIDE_DURATION_MILLIS = 15 * 60 * 1000L;

    private UserRemovedSessionHideWindow() {
    }

    public static boolean hidesSession(@Nullable Long removedAtMillis, long nowMillis) {
        return removedAtMillis != null && nowMillis - removedAtMillis < HIDE_DURATION_MILLIS;
    }
}
