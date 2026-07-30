package com.termux.app.terminal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.termux.shared.termux.settings.preferences.TermuxAppSharedPreferences;

public final class OpenedSessionUnhider {

    private OpenedSessionUnhider() {
    }

    public static boolean unhideOpenedSession(@NonNull TermuxAppSharedPreferences preferences,
                                                 @Nullable String sessionName) {
        if (sessionName == null || sessionName.isEmpty()) {
            return false;
        }
        if (!preferences.isSessionDisabled(sessionName)) {
            return false;
        }
        preferences.setSessionDisabled(sessionName, false);
        return true;
    }
}
