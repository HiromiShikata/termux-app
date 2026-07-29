package com.termux.app.terminal.session;

import androidx.annotation.Nullable;

public final class TransientCommandSessionName {

    public static final String RESET_PREFIX = "[reset] ";

    public static final String KILL_PREFIX = "[kill] ";

    private TransientCommandSessionName() {
    }

    @Nullable
    public static String forResetOfSession(@Nullable String sessionName) {
        return withPrefix(RESET_PREFIX, sessionName);
    }

    @Nullable
    public static String forKillOfSession(@Nullable String sessionName) {
        return withPrefix(KILL_PREFIX, sessionName);
    }

    public static boolean isTransient(@Nullable String sessionName) {
        return sessionName != null
            && (sessionName.startsWith(RESET_PREFIX) || sessionName.startsWith(KILL_PREFIX));
    }

    @Nullable
    private static String withPrefix(String prefix, @Nullable String sessionName) {
        if (sessionName == null || sessionName.isEmpty()) {
            return null;
        }
        return prefix + sessionName;
    }
}
