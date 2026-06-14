package com.termux.app.terminal;

import androidx.annotation.Nullable;

public final class SessionNameBarVisibility {

    public static boolean isVisible(@Nullable String sessionName, boolean browserVisible) {
        if (browserVisible) return false;
        return sessionName != null && !sessionName.trim().isEmpty();
    }

    private SessionNameBarVisibility() {
    }
}
