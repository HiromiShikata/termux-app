package com.termux.app.terminal;

import androidx.annotation.Nullable;

public final class SessionNameBarVisibility {

    public static boolean isVisible(@Nullable String sessionName) {
        return sessionName != null && !sessionName.trim().isEmpty();
    }

    private SessionNameBarVisibility() {
    }
}
